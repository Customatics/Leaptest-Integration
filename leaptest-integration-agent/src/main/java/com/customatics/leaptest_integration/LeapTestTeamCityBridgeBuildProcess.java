package com.customatics.leaptest_integration;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LeapTestTeamCityBridgeBuildProcess extends FutureBasedBuildProcess {
    private final AgentRunningBuild buildingAgent;

    private final BuildRunnerContext context;
    private Process testRunnerProcess = null;


    private static TeamcityServiceMessagesFormatter TeamcityServiceMessages = TeamcityServiceMessagesFormatter.getInstance();
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

    private List<String> getAssemblies(final String rawAssemblyParameter) {
        List<String> rawAssemblies = StringUtil.split(rawAssemblyParameter, true, ',', ';', '\n'/*, '\r','[',']'*/);
        List<String> assemblies = new ArrayList<String>();
        for(int i = 0; i < rawAssemblies.size(); i++ )
        {
            assemblies.add(rawAssemblies.get(i));
        }
        return assemblies;
    }

    protected void cancelBuild() {
        if (testRunnerProcess == null)
            return;

        testRunnerProcess.destroy();
    }

    public BuildFinishedStatus call() throws Exception {

        BuildFinishedStatus status = BuildFinishedStatus.FINISHED_SUCCESS;
        Lock lock = new ReentrantLock();

        HashMap<String, String> schedulesIdTitleHashMap = null; // Id-Title
        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
        ArrayList<String> rawScheduleList = null;

        String leaptestControllerURL = getParameter(StringConstants.ParameterName_LeaptestControllerURL);
        String accessKey = getParameter(StringConstants.ParameterName_AccessKey);
        String timeDelay = getParameter(StringConstants.ParameterName_TimeDelay);
        String doneStatusAs = getParameter(StringConstants.ParameterName_DoneStatus);
        String uri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, leaptestControllerURL);
        int delay = pluginHandler.getTimeDelay(timeDelay);

        String schId = null;
        String schTitle = null;

        try {
            rawScheduleList = pluginHandler.getRawScheduleList(getParameter(StringConstants.ParameterName_ScheduleIds),getParameter(StringConstants.ParameterName_ScheduleNames));

            //Get schedule titles (or/and ids in case of pipeline)
            schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(leaptestControllerURL,accessKey,rawScheduleList,logger,invalidSchedules);
            rawScheduleList = null;

            if(schedulesIdTitleHashMap.isEmpty())
            {
                throw new Exception(Messages.NO_SCHEDULES);
            }

            List<String> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());

            int currentScheduleIndex = 0;
            boolean needSomeSleep = false;   //this time is required if there are schedules to rerun left

            while(!schIdsList.isEmpty())
            {

                if(needSomeSleep) {
                    Thread.sleep(delay * 1000); //Time delay
                    needSomeSleep = false;
                }

                for(ListIterator<String> iter = schIdsList.listIterator(); iter.hasNext(); )
                {
                    schId = iter.next();
                    schTitle = schedulesIdTitleHashMap.get(schId);
                    RUN_RESULT runResult = pluginHandler.runSchedule(leaptestControllerURL, accessKey, schId, schTitle, currentScheduleIndex, logger,  invalidSchedules);
                    logger.message("Current schedule index: " + currentScheduleIndex);

                    if (runResult.equals(RUN_RESULT.RUN_SUCCESS)) // if schedule was successfully run
                    {
                        boolean isStillRunning = true;

                        do
                        {
                            Thread.sleep(delay * 1000); //Time delay
                            isStillRunning = pluginHandler.getScheduleState(leaptestControllerURL, accessKey, schId,schTitle,currentScheduleIndex,doneStatusAs,logger, invalidSchedules);
                            if(isStillRunning) logger.message(String.format(Messages.SCHEDULE_IS_STILL_RUNNING, schTitle, schId));
                        }
                        while (isStillRunning);

                        iter.remove();
                        currentScheduleIndex++;
                    }
                    else if (runResult.equals(RUN_RESULT.RUN_REPEAT))
                    {
                        needSomeSleep = true;
                    }
                    else
                    {
                        iter.remove();
                        currentScheduleIndex++;
                    }
                }
            }

            schIdsList = null;
            schedulesIdTitleHashMap = null;

            if (invalidSchedules.size() > 0)
            {
                logger.message(Messages.INVALID_SCHEDULES);
                logger.logSuiteStarted(Messages.INVALID_SCHEDULES, new Date());

                for (InvalidSchedule invalidSchedule : invalidSchedules)
                {
                    logger.message(invalidSchedule.getName());
                    logger.logTestStarted(invalidSchedule.getName(), new Date());
                    logger.logTestFailed(invalidSchedule.getName(),  invalidSchedule.getStackTrace(), null);
                    logger.logTestFinished(invalidSchedule.getName(), new Date());                }

                logger.logSuiteFinished(Messages.INVALID_SCHEDULES, new Date());
            }

            logger.message(Messages.PLUGIN_SUCCESSFUL_FINISH);

        }
        catch (InterruptedException e)
        {
            lock.lock();
            try {
                String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
                logger.error(interruptedExceptionMessage);
                pluginHandler.stopSchedule(leaptestControllerURL, accessKey, schId, schTitle, logger);
                logger.error("INTERRUPTED");
                status = BuildFinishedStatus.INTERRUPTED;
            }
            finally {
                lock.unlock();
            }
        }
        catch (Exception e){
            logger.error(e.getMessage());
            logger.message(Messages.PLUGIN_ERROR_FINISH);
            status = BuildFinishedStatus.FINISHED_WITH_PROBLEMS;
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
        } finally {
            return status;
        }
    }
}