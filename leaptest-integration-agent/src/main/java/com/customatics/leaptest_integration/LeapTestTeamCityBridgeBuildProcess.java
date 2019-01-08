package com.customatics.leaptest_integration;

import com.ning.http.client.AsyncHttpClient;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

        logger.message(Messages.INPUT_VALUES_MESSAGE);
        logger.message(Messages.CASE_CONSOLE_LOG_SEPARATOR);
        logger.message(String.format(Messages.INPUT_HOSTNAME_VALUE,leapworkHostname));
        logger.message(String.format(Messages.INPUT_PORT_VALUE,leapworkPort));
        String controllerApiHttpAddress = pluginHandler.getControllerApiHttpAdderess(leapworkHostname, leapworkPort, logger);
        logger.message(String.format(Messages.INPUT_CONTROLLER_URL,controllerApiHttpAddress));
        logger.message(String.format(Messages.INPUT_ACCESS_KEY_VALUE,leapworkAccessKey));
        logger.message(String.format(Messages.INPUT_SCHEDULE_NAMES_VALUE,leapworkSchNames));
        logger.message(String.format(Messages.INPUT_SCHEDULE_IDS_VALUE,leapworkSchIds));
        logger.message(String.format(Messages.INPUT_DELAY_VALUE,leapworkDelay));
        logger.message(String.format(Messages.INPUT_DONE_VALUE,leapworkDoneStatusAs));


        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
        final HashMap<String,Integer> repeatedNameMapCounter = new HashMap<>();

        ArrayList<String> rawScheduleList = pluginHandler.getRawScheduleList(leapworkSchIds, leapworkSchNames);

        int timeDelay = pluginHandler.getTimeDelay(leapworkDelay, logger);
        boolean isDoneStatusAsSuccess = leapworkDoneStatusAs.contentEquals("Success");

        AsyncHttpClient mainClient = new AsyncHttpClient();
        try{

            //Get schedule titles (or/and ids in case of pipeline)
            HashMap<UUID, String> schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(mainClient, leapworkAccessKey, controllerApiHttpAddress,rawScheduleList, logger,invalidSchedules);
            rawScheduleList.clear();//don't need that anymore

            if(schedulesIdTitleHashMap.isEmpty())
            {
                throw new Exception(Messages.NO_SCHEDULES);
            }

            List<UUID> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());

            ListIterator<UUID> iter = schIdsList.listIterator();
            while( iter.hasNext())
            {

                UUID schId = iter.next();
                String schTitle = schedulesIdTitleHashMap.get(schId);

                UUID runId = pluginHandler.runSchedule(mainClient,controllerApiHttpAddress, leapworkAccessKey, schId, schTitle, logger);
                if(runId != null)
                {
                    CollectScheduleRunResults(controllerApiHttpAddress, leapworkAccessKey,runId,schTitle,timeDelay,isDoneStatusAsSuccess, logger,repeatedNameMapCounter);
                }
                else
                    invalidSchedules.add(new InvalidSchedule(schTitle,String.format(Messages.SCHEDULE_RUN_FAILURE,schTitle,schId)));

                iter.remove();
            }

            schIdsList.clear();
            schedulesIdTitleHashMap.clear();

            if (invalidSchedules.size() > 0)
            {
                logger.message(Messages.INVALID_SCHEDULES);

                Date errorDate = new Date();
                logger.logSuiteStarted(Messages.INVALID_SCHEDULES);

                for (InvalidSchedule invalidSchedule : invalidSchedules)
                {
                    logger.message(invalidSchedule.getName());
                    logger.logTestStarted(invalidSchedule.getName(), errorDate);
                    logger.logTestFailed(invalidSchedule.getName(),  invalidSchedule.getStackTrace(), null);
                    logger.logTestFinished(invalidSchedule.getName(), errorDate);
                }

                logger.logSuiteFinished(Messages.INVALID_SCHEDULES);
            }

            logger.message(Messages.PLUGIN_SUCCESSFUL_FINISH);

        }
        catch (InterruptedException | CancellationException e)
        {
            logger.error("INTERRUPTED");
            status = BuildFinishedStatus.INTERRUPTED;
        }
        catch (Exception e){
            logger.error(e.getMessage());
            logger.message(Messages.PLUGIN_ERROR_FINISH);
            status = BuildFinishedStatus.FINISHED_WITH_PROBLEMS;
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
        } finally {
            mainClient.close();
            return status;
        }
    }

    private static void CollectScheduleRunResults(String controllerApiHttpAddress, String accessKey, UUID runId, String scheduleName, int timeDelay,boolean isDoneStatusAsSuccess,final BuildProgressLogger logger, HashMap<String,Integer> repeatedNameMapCounter) throws InterruptedException {

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
                    waiter.wait(timeDelay * 1000);//Time delay
                }

                List<UUID> executedRunItems = pluginHandler.getRunRunItems(client,controllerApiHttpAddress,accessKey,runId);
                executedRunItems.removeAll(runItemsId); //left only new


                for(ListIterator<UUID> iter = executedRunItems.listIterator(); iter.hasNext();)
                {
                    UUID runItemId = iter.next();
                    RunItem runItem = pluginHandler.getRunItem(client,controllerApiHttpAddress,accessKey,runItemId, scheduleName,isDoneStatusAsSuccess,logger );

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
                            String passedFlowTitle  = pluginHandler.correctRepeatedTitles(repeatedNameMapCounter,runItem.getCaseName());
                            logger.logTestStarted(passedFlowTitle);
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(passedFlowTitle, Double.toString(runItem.getElapsedTime())));
                            break;
                        case "Failed":
                            String failedFlowTitle  = pluginHandler.correctRepeatedTitles(repeatedNameMapCounter,runItem.getCaseName());
                            logger.logTestStarted(failedFlowTitle);
                            logger.logTestFailed(failedFlowTitle,runItem.failure.getMessage(),null);
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(failedFlowTitle, Double.toString(runItem.getElapsedTime())));
                            break;
                        case "Error":
                        case "Inconclusive":
                        case "Timeout":
                        case "Cancelled":
                            String errorFlowTitle  = pluginHandler.correctRepeatedTitles(repeatedNameMapCounter,runItem.getCaseName());
                            logger.logTestStarted(errorFlowTitle);
                            logger.logTestFailed(errorFlowTitle,runItem.failure.getMessage(),null);
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(errorFlowTitle, Double.toString(runItem.getElapsedTime())));
                            break;
                        case"Done":
                            String doneFlowTitle  = pluginHandler.correctRepeatedTitles(repeatedNameMapCounter,runItem.getCaseName());
                            logger.logTestStarted(doneFlowTitle);
                            if(isDoneStatusAsSuccess == false)
                            {
                                logger.logTestFailed(doneFlowTitle,runItem.failure.getMessage(),null);
                            }
                            logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(doneFlowTitle, Double.toString(runItem.getElapsedTime())));
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
                Date cancelDate = new Date();
                logger.logTestStarted("Aborted schedule", cancelDate);
                logger.logTestFailed("Aborted schedule",  interruptedExceptionMessage, null);
                logger.logTestFinished("Aborted schedule", cancelDate);
                logger.logSuiteFinished(scheduleName, cancelDate);
                pluginHandler.stopRun(controllerApiHttpAddress,runId,scheduleName,accessKey, logger);
            }
            finally {
                lock.unlock();
                throw e;
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            Date cancelDate = new Date();
            logger.logTestStarted("Invalid run", cancelDate);
            logger.logTestFailed("Invalid run",  e.getMessage(), null);
            logger.logTestFinished("Invalid run", cancelDate);
            logger.logSuiteFinished(scheduleName);
        }
        finally {
            logger.logSuiteFinished(scheduleName);
            client.close();
        }

    }
}