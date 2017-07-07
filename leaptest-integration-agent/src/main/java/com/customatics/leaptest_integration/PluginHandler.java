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
            ArrayList<String> rawScheduleList,
            BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    )
    {

        HashMap<String, String> schedulesIdTitleHashMap = new HashMap<>();

        String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, leaptestControllerURL);

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(scheduleListUri).execute().get();
            client = null;

            switch (response.getStatusCode())
            {
                case 200:

                    JsonParser parser = new JsonParser();
                    JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

                    for (String rawSchedule : rawScheduleList) {
                        boolean successfullyMapped = false;
                        for (JsonElement jsonScheduleElement : jsonScheduleList) {
                            JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

                            String Id = Utils.defaultStringIfNull(jsonSchedule.get("Id"), "null Id");
                            String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");
                            //TODO check if schedules can be without titles

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

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s",Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (ConnectException e){
            logger.error( e.getMessage());
        } catch (InterruptedException e) {
            logger.error( e.getMessage());
        } catch (ExecutionException e) {
            logger.error( e.getMessage());
        } catch (IOException e) {
            logger.error( e.getMessage());
        } catch (Exception e) {
            logger.error(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
            logger.error(e.getMessage());
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
        } finally {
            return schedulesIdTitleHashMap;
        }
    }

    public boolean runSchedule(
            String leaptestAddress,
            HashMap.Entry<String,String> schedule,
            int currentScheduleIndex,
            BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    )
    {
        boolean isSuccessfullyRun = false;

        String uri = String.format(Messages.RUN_SCHEDULE_URI, leaptestAddress, schedule.getKey());

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;

            switch (response.getStatusCode())
            {
                case 204:
                    isSuccessfullyRun = true;
                    String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, schedule.getValue(), schedule.getKey());
                    logger.message(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
                    logger.message(successMessage);
                break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s",String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage404);

                case 444:
                    String errorMessage444 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage444 += String.format("\n%1$s",String.format(Messages.SCHEDULE_HAS_NO_CASES,schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage444);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s",String.format(Messages.SCHEDULE_IS_RUNNING_NOW, schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage500);


                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } catch (ExecutionException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        catch (Exception e){
            String errorMessage = String.format(Messages.SCHEDULE_RUN_FAILURE,  schedule.getValue(), schedule.getKey());
            logger.error(errorMessage);
            logger.error(e.getMessage());
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT,schedule.getValue(),schedule.getKey()),String.format("%1$s\n%2$s",errorMessage,e.getMessage())));

        }
        finally {
            return isSuccessfullyRun;
        }
    }

    public boolean getScheduleState(
            String leaptestAddress,
            HashMap.Entry<String,String> schedule,
            int currentScheduleIndex,
            String doneStatusValue,
            BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    )
    {
        boolean isScheduleStillRunning = true;

        String uri = String.format(Messages.GET_SCHEDULE_STATE_URI, leaptestAddress, schedule.getKey());

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;

            switch (response.getStatusCode())
            {
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
                                    logger.logTestFailed(caseTitles.get(i),/* fullstacktrace*/null , null);

                                }

                                logger.message(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));
                                logger.message(TeamcityServiceMessagesFormatter.getInstance().testFinishedMessage(caseTitles.get(i), Integer.toString(milliSeconds.intValue())));

                            }

                            logger.logSuiteFinished(ScheduleTitle);

                    }
                break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s",String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage404);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s",Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } catch (ExecutionException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_STATE_FAILURE, schedule.getValue(), schedule.getKey());
            logger.error(errorMessage);
            logger.error(e.getMessage());
            logger.error(Messages.PLEASE_CONTACT_SUPPORT);
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT,schedule.getValue(),schedule.getKey()),String.format("%1$S\n%2$s",errorMessage, e.getMessage())));
        } finally {
            return isScheduleStillRunning;
        }
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
