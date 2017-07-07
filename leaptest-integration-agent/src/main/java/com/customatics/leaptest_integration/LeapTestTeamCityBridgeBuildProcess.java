package com.customatics.leaptest_integration;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
        
        try {

            HashMap<String, String> schedulesIdTitleHashMap = null; // Id-Title
            ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
            ArrayList<String> rawScheduleList = null;


            String leaptestControllerURL = getParameter(StringConstants.ParameterName_LeaptestControllerURL);
            String timeDelay = getParameter(StringConstants.ParameterName_TimeDelay);
            String doneStatusAs = getParameter(StringConstants.ParameterName_DoneStatus);
            String uri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, leaptestControllerURL);
            int delay = pluginHandler.getTimeDelay(timeDelay);
            logger.message(String.format("Time Delay: %1$d", delay));

            rawScheduleList = pluginHandler.getRawScheduleList(getParameter(StringConstants.ParameterName_ScheduleIds),getParameter(StringConstants.ParameterName_ScheduleNames));

            //Get schedule titles (or/and ids in case of pipeline)
            schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(leaptestControllerURL,rawScheduleList,logger,invalidSchedules);
            rawScheduleList = null;

            int currentScheduleIndex = 0;
            for (HashMap.Entry<String,String> schedule : schedulesIdTitleHashMap.entrySet())
            {

                if (pluginHandler.runSchedule(leaptestControllerURL,schedule, currentScheduleIndex, logger,  invalidSchedules)) // if schedule was successfully run
                {
                    boolean isStillRunning = true;

                    do
                    {
                        Thread.sleep(delay * 1000); //Time delay
                        isStillRunning = pluginHandler.getScheduleState(leaptestControllerURL,schedule,currentScheduleIndex, doneStatusAs, logger, invalidSchedules);
                    }
                    while (isStillRunning);
                }

                currentScheduleIndex++;
            }

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



        }catch (Exception e){
            logger.error(e.getMessage());
            logger.message(Messages.PLUGIN_ERROR_FINISH);
            status = BuildFinishedStatus.FINISHED_FAILED;
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
        }finally {
            return status;
        }
    }



}