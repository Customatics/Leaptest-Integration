package com.customatics.leaptest_integration;

public final class StringConstants {
    public static final String RunTypeName = "LTTCBridgeRunner";
    public static final String ToolName = "LeapTestTeamCityBridge"; // Should mirror the xunit-runner artifactId
    public static final String ParameterName_Version = "Version";
    public static final String ParameterName_URLs = "URLs";
    public static final String ParameterName_TestNames = "TestNames";
    public static final String ParameterName_Ids = "Ids";
    public static final String ParameterName_TimeDelay = "TimeDelay";
    public static final String ParameterName_DoneStatus = "DoneStatus";

    // Getter methods for JSP pages
    public String getParameterName_Version() {return ParameterName_Version;}
    public String getParameterName_URLs() {return ParameterName_URLs;}
    public String getParameterName_TestNames(){return  ParameterName_TestNames;}
    public String getParameterName_TimeDelay(){return  ParameterName_TimeDelay;}
    public String getParameterName_DoneStatus() {return ParameterName_DoneStatus;}
    public String getParameterName_Ids() {return  ParameterName_Ids;}
}
