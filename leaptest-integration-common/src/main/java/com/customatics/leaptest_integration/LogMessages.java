package com.customatics.leaptest_integration;

/**
 * Created by User on 02.05.2017.
 */
public class LogMessages {

    private static LogMessages LOG = null;
    private LogMessages(){}

    public final String ERROR_CODE_MESSAGE = "Code: %1$s Status: %2$s!";
    public final String SCHEDULE_RUN_SUCCESS = "Schedule: %1$s | Schedule Id: %2$s | Launched: SUCCESSFULLY";
    public final String SCHEDULE_RUN_FAILURE = "Failed to run %1$s(%2$s)! Check it at your Leaptest server or connection to your server and try again!";
    public final String SCHEDULE_STATE_FAILURE = "Tried to get %1$s(%2$s) state! Check connection to your server and try again!";
    public final String SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT = "Tried to get schedule title or id! Check connection to your server and try again!";
    public final String CASE_STACKTRACE_FORMAT = "%1$s - %2$s\n";
    public final String SCHEDULE_HAS_NO_CASES = "Schedule has no cases! JSON:\n %2$s";
    public final String GET_ALL_AVAILABLE_SCHEDULES_URI = "%1$s/api/v1/runSchedules";
    public final String RUN_SCHEDULE_URI = "%1$s/%2$s/runNow";
    public final String GET_SCHEDULE_STATE_URI = "%1$s/state/%2$s";
    public final String INVALID_SCHEDULES = "INVALID SCHEDULES";
    public final String PLUGIN_SUCCESSFUL_FINISH = "Leaptest for Teamcity  plugin  successfully finished!";
    public final String PLUGIN_ERROR_FINISH = "Leaptest for Teamcity plugin finished with errors!";
    public final String NO_SUCH_SCHEDULE = "No such schedule!";

    public static LogMessages getInstance()
    {
        if(LOG == null) LOG = new LogMessages();
        return LOG;
    }
}
