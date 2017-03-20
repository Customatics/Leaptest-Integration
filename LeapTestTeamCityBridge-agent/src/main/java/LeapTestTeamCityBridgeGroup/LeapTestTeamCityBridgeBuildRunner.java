package LeapTestTeamCityBridgeGroup;


import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class LeapTestTeamCityBridgeBuildRunner implements AgentBuildRunner, AgentBuildRunnerInfo {
    @NotNull
    public BuildProcess createBuildProcess(AgentRunningBuild agentRunningBuild,
                                           BuildRunnerContext buildRunnerContext)
            throws RunBuildException {
        return new LeapTestTeamCityBridgeBuildProcess(buildRunnerContext);
    }

    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo() {
        return this;
    }

    @NotNull
    public String getType() {
        return StringConstants.RunTypeName;
    }

    public boolean canRun(@NotNull BuildAgentConfiguration buildAgentConfiguration) {
        return true;
    }
}