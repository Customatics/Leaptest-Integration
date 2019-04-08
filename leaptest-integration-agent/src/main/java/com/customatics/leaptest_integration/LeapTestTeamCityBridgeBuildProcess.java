package com.customatics.leaptest_integration;

import com.ning.http.client.AsyncHttpClient;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.StringUtil;
import org.aspectj.bridge.AbortException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LeapTestTeamCityBridgeBuildProcess extends FutureBasedBuildProcess {
    private final AgentRunningBuild buildingAgent;

    private final BuildRunnerContext context;
    private Process testRunnerProcess = null;


    private static PluginHandler pluginHandler = PluginHandler.getInstance();

    public LeapTestTeamCityBridgeBuildProcess(@NotNull final BuildRunnerContext context) {
        super(context);

        this.context = context;
        this.buildingAgent = context.getBuild();
    }

    private String getParameter(@NotNull final String parameterName)
    {
        final String value = context.getRunnerParameters().get(parameterName);
        logger.message(String.format("Input parameter: %1$s: %2$s",parameterName, value));
        if (value == null || value.trim().length() == 0) return "";
        return value.trim();
    }

    protected void cancelBuild() {
        if (testRunnerProcess == null)
            return;

        testRunnerProcess.destroy();
    }

    public BuildFinishedStatus call() throws Exception {

        BuildFinishedStatus status = BuildFinishedStatus.FINISHED_SUCCESS;

        String leapworkHostname = getParameter(StringConstants.ParameterName_Hostname);
        String leapworkPort =  getParameter(StringConstants.ParameterName_Port);
        String leapworkAccessKey = getParameter(StringConstants.ParameterName_AccessKey);
        String leapworkDelay = getParameter(StringConstants.ParameterName_TimeDelay);
        String leapworkDoneStatusAs = getParameter(StringConstants.ParameterName_DoneStatus);
        final String leapworkSchIds = getParameter(StringConstants.ParameterName_ScheduleIds);
        final String leapworkSchNames = getParameter(StringConstants.ParameterName_ScheduleNames);
        String leapworkReport = getParameter(StringConstants.ParameterName_Report);
        String leapworkPassedKeyframes = getParameter(StringConstants.ParameterName_PassedKeyframes);
        String leapworkScheduleVariables = getParameter(StringConstants.ParameterName_ScheduleVariables);

        String controllerApiHttpAddress = pluginHandler.getControllerApiHttpAdderess(leapworkHostname, leapworkPort, logger);
        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
        final HashMap<String,Integer> repeatedNameMapCounter = new HashMap<>();

        ArrayList<String> rawScheduleList = pluginHandler.getRawScheduleList(leapworkSchIds, leapworkSchNames);

        int timeDelay = pluginHandler.getTimeDelay(leapworkDelay, logger);
        String reportFileName = pluginHandler.getReportFileName(leapworkReport, "report.xml");
        boolean isDoneStatusAsSuccess = pluginHandler.isDoneStatusAsSuccess(leapworkDoneStatusAs);
        boolean writePassedKeyframes = Utils.defaultBooleanIfNull(leapworkPassedKeyframes,false);

        String scheduleVariablesRequestPart = pluginHandler.getScheduleVariablesRequestPart(leapworkScheduleVariables, logger);

        AsyncHttpClient mainClient = new AsyncHttpClient();
        try{

            //Get schedule titles (or/and ids in case of pipeline)
            LinkedHashMap<UUID, String> schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(mainClient, leapworkAccessKey, controllerApiHttpAddress,rawScheduleList, logger,invalidSchedules);
            rawScheduleList.clear();//don't need that anymore

            if(schedulesIdTitleHashMap.isEmpty())
            {
                throw new Exception(Messages.NO_SCHEDULES);
            }

            List<UUID> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());
            LinkedHashMap<UUID, LeapworkRun> resultsMap = new LinkedHashMap<>();

            ListIterator<UUID> iter = schIdsList.listIterator();
            while( iter.hasNext())
            {

                UUID schId = iter.next();
                String schTitle = schedulesIdTitleHashMap.get(schId);
                LeapworkRun run  = new LeapworkRun(schId.toString(),schTitle);

                UUID runId = pluginHandler.runSchedule(mainClient,controllerApiHttpAddress, leapworkAccessKey, schId, schTitle, logger, run, scheduleVariablesRequestPart);
                if(runId != null)
                {
                    resultsMap.put(runId, run);
                    CollectScheduleRunResults(controllerApiHttpAddress, leapworkAccessKey,runId,schTitle,timeDelay,isDoneStatusAsSuccess, writePassedKeyframes, run, logger, repeatedNameMapCounter);
                }
                else
                    resultsMap.put(UUID.randomUUID(),run);

                iter.remove();
            }

            schIdsList.clear();
            schedulesIdTitleHashMap.clear();
            RunCollection buildResult = new RunCollection();


            if (invalidSchedules.size() > 0)
            {
                logger.message(Messages.INVALID_SCHEDULES);

                for (InvalidSchedule invalidSchedule : invalidSchedules)
                {
                    logger.warning(String.format("%1$s: %2$s",invalidSchedule.getName(),invalidSchedule.getStackTrace()));
                    LeapworkRun notFoundSchedule = new LeapworkRun(invalidSchedule.getName());
                    RunItem invalidRunItem = new RunItem("Error","Error",0,invalidSchedule.getStackTrace(),invalidSchedule.getName());
                    notFoundSchedule.runItems.add(invalidRunItem);
                    notFoundSchedule.setError(invalidSchedule.getStackTrace());
                    buildResult.leapworkRuns.add(notFoundSchedule);
                }

            }
            List<LeapworkRun> resultRuns = new ArrayList<>(resultsMap.values());
            logger.warning(Messages.TOTAL_SEPARATOR);

            for (LeapworkRun run : resultRuns)
            {
                buildResult.leapworkRuns.add(run);

                buildResult.addFailedTests(run.getFailed());
                buildResult.addPassedTests(run.getPassed());
                buildResult.addErrors(run.getErrors());
                run.setTotal(run.getPassed() + run.getFailed());
                buildResult.addTotalTime(run.getTime());
                logger.warning(String.format(Messages.SCHEDULE_TITLE,run.getScheduleTitle(),run.getScheduleId()));
                logger.warning(String.format(Messages.CASES_PASSED,run.getPassed()));
                logger.warning(String.format(Messages.CASES_FAILED,run.getFailed()));
                logger.warning(String.format(Messages.CASES_ERRORED,run.getErrors()));
            }
            buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

            logger.warning(Messages.TOTAL_SEPARATOR);
            logger.warning(String.format(Messages.TOTAL_CASES_PASSED,buildResult.getPassedTests()));
            logger.warning(String.format(Messages.TOTAL_CASES_FAILED,buildResult.getFailedTests()));
            logger.warning(String.format(Messages.TOTAL_CASES_ERROR,buildResult.getErrors()));

            File reportFile = pluginHandler.createJUnitReport(context, reportFileName, logger, buildResult);

            if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0 || invalidSchedules.size() > 0) {
                if(buildResult.getErrors() > 0)
                    logger.warning(Messages.ERROR_NOTIFICATION);
                logger.warning("FAILURE");
                status = BuildFinishedStatus.FINISHED_FAILED;
            }
            else {
                logger.warning("SUCCESS");
                status =BuildFinishedStatus.FINISHED_SUCCESS;
            }

            logger.warning(Messages.PLUGIN_SUCCESSFUL_FINISH);

        }
        catch (InterruptedException e)
        {
            logger.error("INTERRUPTED");
            status = BuildFinishedStatus.INTERRUPTED;
            logger.error(Messages.PLUGIN_ERROR_FINISH);
        }
        catch (Exception e)
        {
            logger.error(Messages.PLUGIN_ERROR_FINISH);
            logger.error(e.getMessage());
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
            logger.error("FAILURE");
            status = BuildFinishedStatus.FINISHED_FAILED;
        } finally {
            mainClient.close();
            return status;
        }
    }

    private static void CollectScheduleRunResults(String controllerApiHttpAddress, String accessKey, UUID runId, String scheduleName, int timeDelay,boolean isDoneStatusAsSuccess, boolean writePassedKeyframes, LeapworkRun resultRun, final BuildProgressLogger logger, HashMap<String, Integer> repeatedNameMapCounter) throws InterruptedException {
        logger.logSuiteStarted(scheduleName);
        List<UUID> runItemsId = new ArrayList<>();
        Object waiter = new Object();

        //get statuses
        AsyncHttpClient client = new AsyncHttpClient();
        try
        {
            boolean isStillRunning = true;

            do
            {
                synchronized (waiter)
                {
                    waiter.wait(timeDelay*1000);//Time delay
                }

                List<UUID> executedRunItems = pluginHandler.getRunRunItems(client,controllerApiHttpAddress,accessKey,runId);
                executedRunItems.removeAll(runItemsId); //left only new


                for(ListIterator<UUID> iter = executedRunItems.listIterator(); iter.hasNext();)
                {
                    UUID runItemId = iter.next();
                    RunItem runItem = pluginHandler.getRunItem(client,controllerApiHttpAddress,accessKey,runItemId, scheduleName,isDoneStatusAsSuccess, writePassedKeyframes, logger );

                    String status = runItem.getCaseStatus();

                    switch (status)
                    {
                        case "NoStatus":
                        case "Initializing":
                        case "Connecting":
                        case "Connected":
                        case "Running":
                            iter.remove();
                            break;
                        case "Passed":
                            runItem.setCaseName(pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
                            logger.logTestStarted(runItem.getCaseName());
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(runItem.getCaseName(), Double.toString(runItem.getElapsedTime())));
                            resultRun.incPassed();
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                        case "Failed":
                            runItem.setCaseName(pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
                            logger.logTestStarted(runItem.getCaseName());
                            logger.logTestFailed(runItem.getCaseName(),runItem.failure.getMessage(),null);
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(runItem.getCaseName(), Double.toString(runItem.getElapsedTime())));
                            resultRun.incFailed();
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                        case "Error":
                        case "Inconclusive":
                        case "Timeout":
                        case "Cancelled":
                            runItem.setCaseName(pluginHandler.correctRepeatedTitles(repeatedNameMapCounter, runItem.getCaseName()));
                            logger.logTestStarted(runItem.getCaseName());
                            logger.logTestFailed(runItem.getCaseName(),runItem.failure.getMessage(),null);
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(runItem.getCaseName(), Double.toString(runItem.getElapsedTime())));
                            resultRun.incErrors();
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                        case"Done":
                            runItem.setCaseName(pluginHandler.correctRepeatedTitles(repeatedNameMapCounter,runItem.getCaseName()));
                            if(isDoneStatusAsSuccess && !writePassedKeyframes)
                            {
                                resultRun.incPassed();
                            }
                            else{
                                if(!isDoneStatusAsSuccess)
                                    resultRun.incFailed();
                                else
                                    resultRun.incPassed();
                                logger.logTestStarted(runItem.getCaseName());
                                logger.logTestFailed(runItem.getCaseName(),runItem.failure.getMessage(),null);
                            }
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(runItem.getCaseName(), Double.toString(runItem.getElapsedTime())));
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                    }
                }

                runItemsId.addAll(executedRunItems);

                String runStatus = pluginHandler.getRunStatus(client,controllerApiHttpAddress,accessKey,runId);
                if(runStatus.contentEquals("Finished"))
                {
                    List<UUID> allExecutedRunItems = pluginHandler.getRunRunItems(client,controllerApiHttpAddress,accessKey,runId);
                    if(allExecutedRunItems.size() > 0 && allExecutedRunItems.size() <= runItemsId.size())
                        isStillRunning = false;
                }

                if(isStillRunning)
                    logger.message(String.format("The schedule status is already '%1$s' - wait a minute...", runStatus));

            }
            while (isStillRunning);

        }
        catch (InterruptedException | CancellationException e)
        {
            Lock lock = new ReentrantLock();
            lock.lock();
            try {
                String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
                logger.error(interruptedExceptionMessage);
                RunItem invalidItem = new RunItem("Aborted run","Cancelled",0,e.getMessage(),scheduleName);
                pluginHandler.stopRun(controllerApiHttpAddress,runId,scheduleName,accessKey, logger);
                resultRun.incErrors();
                resultRun.runItems.add(invalidItem);
            }
            finally {
                lock.unlock();
                throw e;
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            RunItem invalidItem = new RunItem("Invalid run","Error",0,e.getMessage(),scheduleName);
            resultRun.incErrors();
            resultRun.runItems.add(invalidItem);
        }
        finally {
            logger.logSuiteFinished(scheduleName);
            client.close();
        }

    }
}