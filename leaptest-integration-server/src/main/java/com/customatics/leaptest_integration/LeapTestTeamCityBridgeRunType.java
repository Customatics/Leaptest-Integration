package com.customatics.leaptest_integration;


import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import com.customatics.leaptest_integration.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LeapTestTeamCityBridgeRunType extends RunType {
    private final PluginDescriptor pluginDescriptor;

    public LeapTestTeamCityBridgeRunType(@NotNull final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor)
    {
        this.pluginDescriptor = pluginDescriptor;
        runTypeRegistry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return StringConstants.RunTypeName;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Leapwork Integration";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Integrates Leapwork codeless test automation with Teamcity. Run tests, get results, generate reports.";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new PropertiesProcessor() {
            public Collection<InvalidProperty> process(Map<String, String> properties) {
                ArrayList<InvalidProperty> toReturn = new ArrayList<InvalidProperty>();
                return toReturn;
            }
        };
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("editRunnerParameters.jsp");
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("viewRunnerParameters.jsp");
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        HashMap<String, String> defaults = new HashMap<String, String>();
        defaults.put(StringConstants.ParameterName_DoneStatus, "Success");
        defaults.put(StringConstants.ParameterName_TimeDelay,"3");
        defaults.put(StringConstants.ParameterName_Port,"9001");
        defaults.put(StringConstants.ParameterName_Report, "report.xml");
        return defaults;
    }

    @NotNull
    @Override
    public List<Requirement> getRunnerSpecificRequirements(@NotNull Map<String, String> runParameters) {
        List<Requirement> requirements = new ArrayList<Requirement>(super.getRunnerSpecificRequirements(runParameters));
        return requirements;

    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nLeapwork Controller Host: ");
        sb.append(parameters.get(StringConstants.ParameterName_Hostname));
        sb.append("\nLeapwork Controller Port: ");
        sb.append(parameters.get(StringConstants.ParameterName_Port));
        sb.append("\nAccess Key: ");
        sb.append(parameters.get(StringConstants.ParameterName_AccessKey));
        sb.append("\nReport File: ");
        sb.append(parameters.get(StringConstants.ParameterName_Report));
        sb.append("\nTime Delay: ");
        sb.append(parameters.get(StringConstants.ParameterName_TimeDelay));
        sb.append("\nDone Status As:");
        sb.append(parameters.get(StringConstants.ParameterName_DoneStatus));
        sb.append("\nWrite keyframes of passed flows: ");
        sb.append(Utils.defaultBooleanIfNull(parameters.get(StringConstants.ParameterName_PassedKeyframes), false));
        sb.append("\nSchedule Variables: ");
        sb.append(parameters.get(StringConstants.ParameterName_ScheduleVariables));
        sb.append("\nSchedule Names: ");
        sb.append(parameters.get(StringConstants.ParameterName_ScheduleNames));
        sb.append("\nSchedule Ids: ");
        sb.append(parameters.get(StringConstants.ParameterName_ScheduleIds));
        return sb.toString();
    }
}
