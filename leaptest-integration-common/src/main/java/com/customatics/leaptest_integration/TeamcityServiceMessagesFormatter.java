package com.customatics.leaptest_integration;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by User on 10.05.2017.
 */
public class TeamcityServiceMessagesFormatter {

    private static TeamcityServiceMessagesFormatter teamcityServiceMessagesFormatter = null;

    private TeamcityServiceMessagesFormatter(){}

    public static TeamcityServiceMessagesFormatter getInstance()
    {
        if(teamcityServiceMessagesFormatter == null) teamcityServiceMessagesFormatter = new TeamcityServiceMessagesFormatter();

        return teamcityServiceMessagesFormatter;
    }

    private String teamcityMessageTemplate = "##teamcity[%1$s]";


    private String generateMessage(String ServiceMessageType, ArrayList<String> properties)
    {
       StringBuilder message = new StringBuilder();

       message.append(ServiceMessageType + " ");

       for(int i = 0; i < properties.size(); i++)
           message.append(properties.get(i) + " ");

       return message.toString();
    }

    private String generateMessageProperty(String serviceMessagePropertyType, String value)
    {
        return String.format("%1$s='%2$s'", serviceMessagePropertyType, value);
    }

    private final class ServiceMessageType
    {
        public static final String TEST_SUITE_STARTED = "testSuiteStarted";
        public static final String TEST_SUITE_FINISHED = "testSuiteFinished";
        public static final String TEST_STARTED = "testStarted";
        public static final String TEST_FINISHED = "testFinished";
        public static final String TEST_FAILED = "testFailed";
        public static final String BUILD_STATUS = "buildStatus";
    }

    private final class ServiceMessagePropertyType
    {
        public static final String NAME = "name";
        public static final String CAPTURE_STANDARD_OUTPUT = "captureStandardOutput";
        public static final String MESSAGE = "message";
        public static final String DETAILS = "details";
        public static final String DURATION = "duration";
        public static final String STATUS = "status";
        public static final String TEXT = "text";

    }

    public String testStartedMessage(String caseName, boolean captureStandardOutput)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.TEST_STARTED,
                       new ArrayList<String>(
                               Arrays.asList(
                                       generateMessageProperty(ServiceMessagePropertyType.CAPTURE_STANDARD_OUTPUT, Boolean.toString(captureStandardOutput)),
                                       generateMessageProperty(ServiceMessagePropertyType.NAME, caseName)
                               )
                       )
                )
        );
    }

    public String testFinishedMessage( String caseName, String duration)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.TEST_FINISHED,
                        new ArrayList<String>(
                                Arrays.asList(
                                        generateMessageProperty(ServiceMessagePropertyType.NAME, caseName),
                                        generateMessageProperty(ServiceMessagePropertyType.DURATION, duration)
                                )
                        )
                )
        );
    }

    public String testFailedMessage( String caseName, boolean captureStandardOutput, String message, String details)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.TEST_FAILED,
                        new ArrayList<String>(
                                Arrays.asList(
                                        generateMessageProperty(ServiceMessagePropertyType.CAPTURE_STANDARD_OUTPUT, Boolean.toString(captureStandardOutput)),
                                        generateMessageProperty(ServiceMessagePropertyType.NAME, caseName),
                                        generateMessageProperty(ServiceMessagePropertyType.MESSAGE, message),
                                        generateMessageProperty(ServiceMessagePropertyType.DETAILS, details)
                                )
                        )
                )
        );
    }

    public String testFailedMessage( String caseName, boolean captureStandardOutput)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.TEST_FAILED,
                        new ArrayList<String>(
                                Arrays.asList(
                                        generateMessageProperty(ServiceMessagePropertyType.CAPTURE_STANDARD_OUTPUT, Boolean.toString(captureStandardOutput)),
                                        generateMessageProperty(ServiceMessagePropertyType.NAME, caseName)

                                )
                        )
                )
        );
    }

    public String testSuiteStartedMessage( String scheduleName)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.TEST_SUITE_STARTED,
                        new ArrayList<String>(
                                Arrays.asList(
                                        generateMessageProperty(ServiceMessagePropertyType.NAME, scheduleName)

                                )
                        )
                )
        );
    }

    public String testSuiteFinishedMessage( String scheduleName)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.TEST_SUITE_FINISHED,
                        new ArrayList<String>(
                                Arrays.asList(
                                        generateMessageProperty(ServiceMessagePropertyType.NAME, scheduleName)

                                )
                        )
                )
        );
    }

    public String buildStatusFailureMessage(String text)
    {
        return String.format(teamcityMessageTemplate,
                generateMessage(ServiceMessageType.BUILD_STATUS,
                        new ArrayList<String>(
                                Arrays.asList(
                                        generateMessageProperty(ServiceMessagePropertyType.STATUS,"FAILURE"),
                                        generateMessageProperty(ServiceMessagePropertyType.TEXT, text)

                                )
                        )
                )
        );
    }

}


