package com.customatics.leaptest_integration;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import jetbrains.buildServer.agent.BuildProgressLogger;

import java.io.*;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;


public final class PluginHandler {

    private static PluginHandler pluginHandler = null;

    private PluginHandler(){}

    public static PluginHandler getInstance()
    {
        if( pluginHandler == null ) pluginHandler = new PluginHandler();

        return pluginHandler;
    }


    public ArrayList<String> getRawScheduleList(String rawScheduleIds, String rawScheduleTitles)
    {
        ArrayList<String> rawScheduleList = new ArrayList<>();

        String[] schidsArray = rawScheduleIds.split("\n|, |,");
        String[] testsArray = rawScheduleTitles.split("\n|, |,");

        for(int i = 0; i < schidsArray.length; i++)
        {
            rawScheduleList.add(schidsArray[i]);
        }
        for(int i = 0; i < testsArray.length; i++)
        {
            rawScheduleList.add(testsArray[i]);
        }

        return rawScheduleList;
    }

    public int getTimeDelay(String rawTimeDelay)
    {
        int defaultTimeDelay = 3;
        if(!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay))
            return Integer.parseInt(rawTimeDelay);
        else
            return defaultTimeDelay;
    }


    public HashMap<String, String> getSchedulesIdTitleHashMap(
            String leaptestControllerURL,
            String accessKey,
            ArrayList<String> rawScheduleList,
            BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws Exception {

        HashMap<String, String> schedulesIdTitleHashMap = new HashMap<>();

        String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, leaptestControllerURL);

        try {
            try {

                AsyncHttpClient client = new AsyncHttpClient();
                Response response = client
                        .prepareGet(scheduleListUri)
                        .setHeader("AccessKey", accessKey)
                        .setHeader("Integration", Messages.INTEGRATION)
                        .execute()
                        .get();
                client = null;

                switch (response.getStatusCode())
                {
                    case 200:

                        JsonParser parser = new JsonParser();
                        JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

                        for (String rawSchedule : rawScheduleList)
                        {
                            boolean successfullyMapped = false;
                            for (JsonElement jsonScheduleElement : jsonScheduleList) {
                                JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

                                String Id = Utils.defaultStringIfNull(jsonSchedule.get("Id"), "null Id");
                                String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");


                                if (Id.contentEquals(rawSchedule)) {
                                    if (!schedulesIdTitleHashMap.containsValue(Title)) {
                                        schedulesIdTitleHashMap.put(rawSchedule, Title);
                                        logger.message(String.format(Messages.SCHEDULE_DETECTED, Title, rawSchedule));
                                    }
                                    successfullyMapped = true;
                                }

                                if (Title.contentEquals(rawSchedule)) {
                                    if (!schedulesIdTitleHashMap.containsKey(Id)) {
                                        schedulesIdTitleHashMap.put(Id, rawSchedule);
                                        logger.message(String.format(Messages.SCHEDULE_DETECTED, rawSchedule, Title));
                                    }
                                    successfullyMapped = true;
                                }
                            }

                            if (!successfullyMapped)
                                invalidSchedules.add(new InvalidSchedule(rawSchedule, Messages.NO_SUCH_SCHEDULE));
                        }
                    break;

                    case 406:
                        String errorMessage406 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage406 += String.format("\n%1$s", Messages.WRONG_ACCESS_KEY);
                        throw new Exception(errorMessage406);

                    case 445:
                        String errorMessage445 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage445 += String.format("\n%1$s", Messages.LICENSE_EXPIRED);
                        throw new Exception(errorMessage445);

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                        throw new Exception(errorMessage500);

                    default:
                        String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        throw new Exception(errorMessage);
                }
            }
            catch (ConnectException | UnknownHostException e )
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
                throw new Exception(connectionErrorMessage);
            }
            catch (InterruptedException e)
            {
                String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
                throw new Exception(interruptedExceptionMessage);
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof  UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getCause().getMessage());
                    throw new Exception(connectionErrorMessage);
                }
                else
                {
                    String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                    throw new Exception(executionExceptionMessage);
                }
            }
            catch (IOException e)
            {
                String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
                throw new Exception(ioExceptionMessage);
            }
        }
        catch (Exception e)
        {
            logger.error(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
            throw e;
        }

        return schedulesIdTitleHashMap;
    }

    public RUN_RESULT runSchedule(
            String leaptestAddress,
            String accessKey,
            String scheduleId,
            String scheduleTitle,
            int currentScheduleIndex,
            BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws Exception {
        RUN_RESULT isSuccessfullyRun = RUN_RESULT.RUN_FAIL;

        String uri = String.format(Messages.RUN_SCHEDULE_URI, leaptestAddress, scheduleId);

        try {
            try {

                AsyncHttpClient client = new AsyncHttpClient();
                Response response = client
                        .preparePut(uri)
                        .setHeader("AccessKey",accessKey)
                        .setHeader("Integration",Messages.INTEGRATION)
                        .setBody("")
                        .execute()
                        .get();
                client = null;

                switch (response.getStatusCode()) {
                    case 204:
                        isSuccessfullyRun = RUN_RESULT.RUN_SUCCESS;
                        String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, scheduleTitle, scheduleId);
                        logger.message(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
                        logger.message(successMessage);
                        break;

                    case 404:
                        String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage404);

                    case 406:
                        String errorMessage406 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage406 += String.format("\n%1$s", Messages.WRONG_ACCESS_KEY);
                        throw new Exception(errorMessage406);

                    case 444:
                        String errorMessage444 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage444 += String.format("\n%1$s", String.format(Messages.SCHEDULE_HAS_NO_CASES, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage444);

                    case 445:
                        String errorMessage445 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage445 += String.format("\n%1$s", Messages.LICENSE_EXPIRED);
                        throw new InterruptedException(errorMessage445);

                    case 448:
                        String errorMessage448 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage448 += String.format("\n%1$s", String.format(Messages.CACHE_TIMEOUT_EXCEPTION, scheduleTitle, scheduleId));
                        isSuccessfullyRun = RUN_RESULT.RUN_REPEAT;
                        logger.error(errorMessage448);
                        break;

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", String.format(Messages.SCHEDULE_IS_RUNNING_NOW, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage500);

                    default:
                        String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        throw new Exception(errorMessage);
                }

            } catch (ConnectException | UnknownHostException e)
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getMessage());
                logger.error(connectionErrorMessage);
                return RUN_RESULT.RUN_REPEAT;
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage());
                    logger.error(connectionErrorMessage);
                    return RUN_RESULT.RUN_REPEAT;
                }
                else
                {
                    String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                    throw new Exception(executionExceptionMessage);
                }
            }
            catch (IOException e)
            {
                String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
                throw new Exception(ioExceptionMessage);
            }
            catch (Exception e)
            {
                throw e;
            }
        }
        catch (InterruptedException e)
        {
            throw new Exception(e.getMessage());
        }
        catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_RUN_FAILURE, scheduleTitle, scheduleId);
            logger.error(errorMessage);
            logger.error(e.getMessage());
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT, scheduleTitle, scheduleId), String.format("%1$s\n%2$s", errorMessage, e.getMessage())));
            return RUN_RESULT.RUN_FAIL;
        }

        return isSuccessfullyRun;
    }

    public boolean stopSchedule(String leaptestAddress, String accessKey, String scheduleId, String scheduleTitle, BuildProgressLogger logger)
    {
        boolean isSuccessfullyStopped = false;

        logger.error(String.format(Messages.STOPPING_SCHEDULE,scheduleTitle,scheduleId));
        String uri = String.format(Messages.STOP_SCHEDULE_URI, leaptestAddress, scheduleId);
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client
                    .preparePut(uri)
                    .setHeader("AccessKey", accessKey)
                    .setHeader("Integration", Messages.INTEGRATION)
                    .setBody("")
                    .execute()
                    .get();
            client = null;

            switch (response.getStatusCode())
            {
                case 204:
                    logger.error(String.format(Messages.STOP_SUCCESS,scheduleTitle,scheduleId));
                    isSuccessfullyStopped = true;
                    break;
                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);

            }
        } catch (Exception e)
        {
            logger.error(String.format(Messages.STOP_FAIL,scheduleTitle,scheduleId));
            logger.error(e.getMessage());
        }
        finally
        {
            return isSuccessfullyStopped;
        }

    }

    public boolean getScheduleState(
            String leaptestAddress,
            String accessKey,
            String scheduleId,
            String scheduleTitle,
            int currentScheduleIndex,
            String doneStatusValue,
            BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws InterruptedException {
        boolean isScheduleStillRunning = true;

        String uri = String.format(Messages.GET_SCHEDULE_STATE_URI, leaptestAddress, scheduleId);

        try {
            try {

                AsyncHttpClient client = new AsyncHttpClient();
                Response response = client
                        .prepareGet(uri)
                        .setHeader("AccessKey",accessKey)
                        .setHeader("Integration",Messages.INTEGRATION)
                        .execute()
                        .get();
                client = null;

                switch (response.getStatusCode()) {
                    case 200:
                        JsonParser parser = new JsonParser();
                        JsonObject jsonState = parser.parse(response.getResponseBody()).getAsJsonObject();
                        parser = null;

                        String ScheduleId = jsonState.get("ScheduleId").getAsString();

                        if (isScheduleStillRunning(jsonState))
                            isScheduleStillRunning = true;
                        else {
                            isScheduleStillRunning = false;

                            /////////Schedule Info
                            JsonElement jsonLastRun = jsonState.get("LastRun");


                            JsonObject lastRun = jsonLastRun.getAsJsonObject();

                            String ScheduleTitle = lastRun.get("ScheduleTitle").getAsString();

                            int passedCount = caseStatusCount("PassedCount", lastRun);
                            int failedCount = caseStatusCount("FailedCount", lastRun);
                            int doneCount = caseStatusCount("DoneCount", lastRun);


                            if (doneStatusValue.contentEquals("Failed"))
                                failedCount += doneCount;
                            else
                                passedCount += doneCount;


                            ///////////AutomationRunItemsInfo
                            JsonArray jsonAutomationRunItems = lastRun.get("AutomationRunItems").getAsJsonArray();

                            ArrayList<String> automationRunId = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                automationRunId.add(jsonAutomationRunItem.getAsJsonObject().get("AutomationRunId").getAsString());
                            ArrayList<String> statuses = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                statuses.add(jsonAutomationRunItem.getAsJsonObject().get("Status").getAsString());
                            ArrayList<String> elapsed = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                elapsed.add(defaultElapsedIfNull(jsonAutomationRunItem.getAsJsonObject().get("Elapsed")));
                            ArrayList<String> environments = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                environments.add(jsonAutomationRunItem.getAsJsonObject().get("Environment").getAsJsonObject().get("Title").getAsString());

                            ArrayList<String> caseTitles = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems) {
                                String caseTitle = Utils.defaultStringIfNull(jsonAutomationRunItem.getAsJsonObject().get("Case").getAsJsonObject().get("Title"), "Null case Title");
                                if (caseTitle.contentEquals("Null case Title"))
                                    caseTitles.add(caseTitles.get(caseTitles.size() - 1));
                                else
                                    caseTitles.add(caseTitle);
                            }

                            makeCaseTitlesNonRepeatable(caseTitles); //this is required because Teamcity does not suppose that there can be 2 or more case with the same name but different results

                            logger.logSuiteStarted(ScheduleTitle);

                            for (int i = 0; i < jsonAutomationRunItems.size(); i++) {

                                logger.message(Messages.CASE_CONSOLE_LOG_SEPARATOR);
                                logger.logTestStarted(caseTitles.get(i));

                                //double seconds = jsonArray.getJSONObject(i).getDouble("TotalSeconds");
                                Double milliSeconds = parseExecutionTimeToMilliSeconds(elapsed.get(i));


                                if (statuses.get(i).contentEquals("Failed") || (statuses.get(i).contentEquals("Done") && doneStatusValue.contentEquals("Failed")) || statuses.get(i).contentEquals("Error") || statuses.get(i).contentEquals("Cancelled")) {

                                    JsonArray jsonKeyframes = jsonAutomationRunItems.get(i).getAsJsonObject().get("Keyframes").getAsJsonArray();

                                    //KeyframeInfo
                                    ArrayList<String> keyFrameTimeStamps = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameTimeStamps.add(jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsString());
                                    ArrayList<String> keyFrameLogMessages = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameLogMessages.add(jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString());

                                    logger.message(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));


                                    int currentKeyFrameIndex = 0;

                                    for (JsonElement jsonKeyFrame : jsonKeyframes) {
                                        String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "");
                                        if (!level.contentEquals("") && !level.contentEquals("Trace")) {
                                            String stacktrace = String.format(Messages.CASE_STACKTRACE_FORMAT, keyFrameTimeStamps.get(currentKeyFrameIndex), keyFrameLogMessages.get(currentKeyFrameIndex));
                                            logger.message(stacktrace);
                                        }
                                        currentKeyFrameIndex++;
                                    }


                                    logger.message("Environment: " + environments.get(i));
                                    logger.logTestFailed(caseTitles.get(i),/* fullstacktrace*/null, null);

                                }

                                logger.message(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));
                                logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(caseTitles.get(i), Integer.toString(milliSeconds.intValue())));

                            }

                            logger.logSuiteFinished(ScheduleTitle);

                        }
                        break;

                    case 404:
                        String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage404);

                    case 406:
                        String errorMessage406 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage406 += String.format("\n%1$s", Messages.WRONG_ACCESS_KEY);
                        throw new Exception(errorMessage406);

                    case 445:
                        String errorMessage445 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage445 += String.format("\n%1$s", Messages.LICENSE_EXPIRED);
                        throw new InterruptedException(errorMessage445);

                    case 448:
                        String errorMessage448 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage448 += String.format("\n%1$s", String.format(Messages.CACHE_TIMEOUT_EXCEPTION, scheduleTitle, scheduleId));
                        isScheduleStillRunning = true;
                        logger.error(errorMessage448);
                        break;

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                        throw new Exception(errorMessage500);

                    default:
                        String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        throw new Exception(errorMessage);
                }

            } catch (NoRouteToHostException e)
            {
                String connectionLostErrorMessage = String.format(Messages.CONNECTION_LOST, e.getCause().getMessage());
                logger.error(connectionLostErrorMessage);
                return true;
            }
            catch (ConnectException | UnknownHostException e)
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getMessage());
                logger.error(connectionErrorMessage);
                return true;
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof  UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage());
                    logger.error(connectionErrorMessage);
                    return true;
                }
                else if(e.getCause() instanceof NoRouteToHostException)
                {
                    String connectionLostErrorMessage = String.format(Messages.CONNECTION_LOST, e.getCause().getMessage());
                    logger.error(connectionLostErrorMessage);
                    return true;
                }
                else
                {
                    String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                    throw new Exception(executionExceptionMessage);
                }

            }
            catch (IOException e)
            {
                String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
                throw new Exception(ioExceptionMessage);
            }
            catch (Exception e)
            {
                throw e;
            }
        }
        catch (InterruptedException e)
        {
            throw new InterruptedException(e.getMessage());
        }
        catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_STATE_FAILURE, scheduleTitle, scheduleId);
            logger.error(errorMessage);
            logger.error(e.getMessage());
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT, scheduleTitle, scheduleId), String.format("%1$s\n%2$s", errorMessage, e.getMessage())));
            return false;
        }

        return isScheduleStillRunning;
    }

    private boolean isScheduleStillRunning(JsonObject jsonState)
    {
        String status = Utils.defaultStringIfNull(jsonState.get("Status"), "Finished");

        if (status.contentEquals("Running") || status.contentEquals("Queued"))
            return true;
        else
            return false;

    }

    private double parseExecutionTimeToMilliSeconds(String rawExecutionTime)
    {
        String ExecutionTotalTime[] = rawExecutionTime.split(":|\\.");

        double seconds = Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 +  //hours
                Double.parseDouble(ExecutionTotalTime[1]) * 60 +        //minutes
                Double.parseDouble(ExecutionTotalTime[2]) +             //seconds
                Double.parseDouble("0." + ExecutionTotalTime[3]);   //milliseconds

        return seconds*1000;
    }

    private int caseStatusCount(String statusName, JsonObject lastRun)
    {
        Integer temp =  Utils.defaultIntIfNull(lastRun.get(statusName), 0);
        return temp.intValue();
    }

    private void makeCaseTitlesNonRepeatable(ArrayList<String> caseTitles)
    {
        HashSet<String> NotRepeatedNamesSet = new HashSet<String>(caseTitles);
        String[] NotRepeatedNames = NotRepeatedNamesSet.toArray(new String[NotRepeatedNamesSet.size()]);
        NotRepeatedNamesSet = null;

        for(int i = 0; i < NotRepeatedNames.length; i++)
        {
            int count = 0;
            for(int j = 0; j < caseTitles.size(); j++)
            {
                if(NotRepeatedNames[i].equals(caseTitles.get(j)))
                {
                    if(count > 0)
                    {
                        caseTitles.set(j,String.format("%1$s(%2$d)",caseTitles.get(j),count));
                    }
                    count++;
                }
            }
        }

        NotRepeatedNames = null;
    }

    private String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
            return   rawElapsed.getAsString();
        else
            return "00:00:00.0000000";

    }
}
