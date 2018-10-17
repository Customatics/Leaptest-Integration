package com.customatics.leaptest_integration;

public final class StringConstants {
    public static final String RunTypeName = "Leapwork Integration";
    public static final String ParameterName_Hostname = "leapworkHostname";
    public static final String ParameterName_Port = "leapworkPort";
    public static final String ParameterName_AccessKey= "leapworkAccessKey";
    public static final String ParameterName_ScheduleNames = "leapworkScheduleNames";
    public static final String ParameterName_ScheduleIds = "leapworkScheduleIds";
    public static final String ParameterName_TimeDelay = "leapworkTimeDelay";
    public static final String ParameterName_DoneStatus = "leapworkDoneStatusAs";

    // Getter methods for JSP pages
    public String getParameterName_Hostname(){return ParameterName_Hostname;}
    public String getParameterName_Port(){return ParameterName_Port;}
    public String getParameterName_AccessKey() {return ParameterName_AccessKey;}
    public String getParameterName_ScheduleNames(){return ParameterName_ScheduleNames;}
    public String getParameterName_TimeDelay(){return  ParameterName_TimeDelay;}
    public String getParameterName_DoneStatus() {return ParameterName_DoneStatus;}
    public String getParameterName_ScheduleIds() {return ParameterName_ScheduleIds;}
}
