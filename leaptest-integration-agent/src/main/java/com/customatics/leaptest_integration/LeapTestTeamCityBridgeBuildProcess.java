package com.customatics.leaptest_integration;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class LeapTestTeamCityBridgeBuildProcess extends FutureBasedBuildProcess {
    private final AgentRunningBuild buildingAgent;
    private final BuildRunnerContext context;
    private Process testRunnerProcess;

    public LeapTestTeamCityBridgeBuildProcess(@NotNull final BuildRunnerContext context) {
        super(context);

        this.context = context;
        this.buildingAgent = context.getBuild();
    }

    private String getParameter(@NotNull final String parameterName)
    {
        final String value = context.getRunnerParameters().get(parameterName);
        if (value == null || value.trim().length() == 0) return "";
        return value.trim();
    }

    private List<String> getAssemblies(final String rawAssemblyParameter) {
        List<String> rawAssemblies = StringUtil.split(rawAssemblyParameter, true, ',', ';', '\n'/*, '\r','[',']'*/);
        List<String> assemblies = new ArrayList<String>();
        for(int i = 0; i < rawAssemblies.size(); i++ )
        {
            assemblies.add(rawAssemblies.get(i));
            logger.message(rawAssemblies.get(i));
        }
        return assemblies;
    }

    protected void cancelBuild() {
        if (testRunnerProcess == null)
            return;

        testRunnerProcess.destroy();
    }

    public BuildFinishedStatus call() throws Exception {
        try {
            String version = getParameter(StringConstants.ParameterName_Version);
            File agentToolsDirectory = buildingAgent.getAgentConfiguration().getAgentToolsDirectory();
            String runnerPath = new File(agentToolsDirectory, "leaptest-integration-runner\\bin\\" + version +"\\"+ "ConsoleApp.exe").getPath();
            String URLs = getParameter(StringConstants.ParameterName_URLs);
            List<String> SchedulesId = getAssemblies(getParameter(StringConstants.ParameterName_Ids));
            String TimeDelay = getParameter(StringConstants.ParameterName_TimeDelay);
            String DoneStatus = getParameter(StringConstants.ParameterName_DoneStatus);
            BuildFinishedStatus status = BuildFinishedStatus.FINISHED_SUCCESS;

            ArrayList<String> parameters = new ArrayList<String>();
            parameters.add(runnerPath);
            parameters.add(URLs);
            parameters.add(TimeDelay);
            parameters.add(DoneStatus);

            for(int i = 0; i < SchedulesId.size(); i++)
            { parameters.add(SchedulesId.get(i));}

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(parameters);


            Map<String, String> env = processBuilder.environment();
            for(Map.Entry<String, String> kvp : context.getBuildParameters().getEnvironmentVariables().entrySet()) {
                env.put(kvp.getKey(), kvp.getValue());
            }
            testRunnerProcess = processBuilder.start();
            redirectStreamToLogger(testRunnerProcess.getInputStream(), new RedirectionTarget() {
                public void redirect(String s)
                {
                    logger.message(s);
                    System.out.println(s);
                    //get teamcity service message
                }
            });
            redirectStreamToLogger(testRunnerProcess.getErrorStream(), new RedirectionTarget() {
                public void redirect(String s) {
                    logger.warning(s);
                    logger.error(s);
                    System.out.println(s);
                }
            });

            testRunnerProcess.waitFor();
            testRunnerProcess.destroy();

            return status;
        }
        catch(Exception e) {
            logger.message("Failed to run tests! Try again!");
            logger.exception(e);
            return BuildFinishedStatus.FINISHED_FAILED;
        }
    }

    private interface RedirectionTarget {
        void redirect(String s);
    }
    private void redirectStreamToLogger(final InputStream s, final RedirectionTarget target) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(s);
                while (sc.hasNextLine()) {
                    target.redirect(sc.nextLine());
                }
            }
        }).start();
    }

}