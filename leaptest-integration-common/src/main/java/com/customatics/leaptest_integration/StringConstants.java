package com.customatics.leaptest_integration;

public final class StringConstants {
    public static final String RunTypeName = "Leaptest Integration";
    public static final String ParameterName_ServerURL = "ServerURL";
    public static final String ParameterName_ScheduleNames = "ScheduleNames";
    public static final String ParameterName_ScheduleIds = "ScheduleIds";
    public static final String ParameterName_TimeDelay = "TimeDelay";
    public static final String ParameterName_DoneStatus = "DoneStatusAs";

    // Getter methods for JSP pages
    public String getParameterName_ServerURL() {return ParameterName_ServerURL;}
    public String getParameterName_ScheduleNames(){return ParameterName_ScheduleNames;}
    public String getParameterName_TimeDelay(){return  ParameterName_TimeDelay;}
    public String getParameterName_DoneStatus() {return ParameterName_DoneStatus;}
    public String getParameterName_ScheduleIds() {return ParameterName_ScheduleIds;}
}
