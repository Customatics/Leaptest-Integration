package com.customatics.leaptest_integration;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.IntegerOption;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

class LeapTestTeamCityBridgeBuildProcess extends FutureBasedBuildProcess {
    private final AgentRunningBuild buildingAgent;
    private final BuildRunnerContext context;
    private Process testRunnerProcess = null;

    private static LogMessages MESSAGES = LogMessages.getInstance();
    private static TeamcityServiceMessagesFormatter TeamcityServiceMessages = TeamcityServiceMessagesFormatter.getInstance();

    public LeapTestTeamCityBridgeBuildProcess(@NotNull final BuildRunnerContext context) {
        super(context);

        this.context = context;
        this.buildingAgent = context.getBuild();
    }

    private String getParameter(@NotNull final String parameterName)
    {
        final String value = context.getRunnerParameters().get(parameterName);
        logger.message(String.format("Input parameter: %1$s: %2$s",parameterName, value));
        if (value == null || value.trim().length() == 0) return "";
        return value.trim();
    }

    private List<String> getAssemblies(final String rawAssemblyParameter) {
        List<String> rawAssemblies = StringUtil.split(rawAssemblyParameter, true, ',', ';', '\n'/*, '\r','[',']'*/);
        List<String> assemblies = new ArrayList<String>();
        for(int i = 0; i < rawAssemblies.size(); i++ )
        {
            assemblies.add(rawAssemblies.get(i));
        }
        return assemblies;
    }

    protected void cancelBuild() {
        if (testRunnerProcess == null)
            return;

        testRunnerProcess.destroy();
    }

    public BuildFinishedStatus call() throws Exception {


        BuildFinishedStatus status = BuildFinishedStatus.FINISHED_SUCCESS;
        
        try {
            String serverURL = getParameter(StringConstants.ParameterName_ServerURL);
            String timeDelay = getParameter(StringConstants.ParameterName_TimeDelay);
            String doneStatusAs = getParameter(StringConstants.ParameterName_DoneStatus);
            List<String> scheduleIds = getAssemblies(getParameter(StringConstants.ParameterName_ScheduleIds));

            
            HashMap<String, String> schedules = new HashMap<String, String>(); // Id-Title
            HashMap<String,String> InValidSchedules = new HashMap<String, String>(); // Id-Stack trace
            MutableBoolean isScheduleStillRunning = new MutableBoolean(false);
            MutableBoolean isSuccessfullyLaunchedSchedule =  new MutableBoolean(false);


            String uri = String.format(MESSAGES.GET_ALL_AVAILABLE_SCHEDULES_URI, serverURL);

            int delay = 3;

            if(!timeDelay.isEmpty() || !"".equals(timeDelay))
            {delay = Integer.parseInt(timeDelay);}
            logger.message("Time Delay: " + (Integer.toString(delay)));


            GetSchTitlesOrIds(uri, scheduleIds, schedules,  InValidSchedules, logger); //Get schedule titles (or/and ids in case of pipeline)

            scheduleIds = null;

            int index = 0;

            for (HashMap.Entry<String,String> schedule : schedules.entrySet())
            {

                String runUri = String.format(MESSAGES.RUN_SCHEDULE_URI, uri, schedule.getKey());
                String stateUri = String.format(MESSAGES.GET_SCHEDULE_STATE_URI, uri, schedule.getKey());

                RunSchedule(runUri, schedule.getKey(), schedule.getValue(), index, isSuccessfullyLaunchedSchedule,  InValidSchedules, logger); // Run schedule. In case of unsuccessfull run throws exception

                if (isSuccessfullyLaunchedSchedule.getValue()) // if schedule was successfully run
                {
                    do
                    {
                        Thread.sleep(delay * 1000); //Time delay
                        GetScheduleState(stateUri, schedule.getKey(), schedule.getValue(), index,  isScheduleStillRunning,  doneStatusAs,  InValidSchedules, logger); //Get schedule state info
                    }
                    while (isScheduleStillRunning.getValue());
                }

                index++;
            }

            if (InValidSchedules.size() > 0) {

                logger.logSuiteStarted(MESSAGES.INVALID_SCHEDULES, new Date());

                for (String invalidsch : InValidSchedules.keySet()) {

                    logger.logTestStarted(invalidsch, new Date());
                    logger.logTestFailed(invalidsch,  InValidSchedules.get(invalidsch), null);
                    logger.logTestFinished(invalidsch, new Date());
                }

                logger.logSuiteFinished(MESSAGES.INVALID_SCHEDULES, new Date());
            }

            logger.message(MESSAGES.PLUGIN_SUCCESSFUL_FINISH);

            return status;
    }catch (Exception e){
            logger.message(MESSAGES.PLUGIN_ERROR_FINISH);
            status = BuildFinishedStatus.FINISHED_FAILED;
            return status;
        }
    }



    private static String JsonToTeamcity(String str, int current,  MutableBoolean isScheduleStillRunning, String doneStatus,  HashMap<String, String> InValidSchedules, BuildProgressLogger logger)
    {

        String teamCityMessage = "";

        org.json.JSONObject json = new org.json.JSONObject(str);

        String ScheduleId = json.getString("ScheduleId");


        if (json.optString("Status").equals("Running") || json.optString("Status").equals("Queued"))
        {
            isScheduleStillRunning.setValue(true);
        }
        else
        {
            isScheduleStillRunning.setValue(false);

            /////////Schedule Info
            org.json.JSONObject LastRun = json.optJSONObject("LastRun");

            if (LastRun != null)
            {
                String ScheduleTitle = json.optJSONObject("LastRun").getString("ScheduleTitle");

                ///////////AutomationRunItemInfo
                JSONArray jsonArray = json.getJSONObject("LastRun").getJSONArray("AutomationRunItems");

                ArrayList<String> AutomationRunId = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) AutomationRunId.add(jsonArray.getJSONObject(i).getString("AutomationRunId"));
                ArrayList<String> Status = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Status.add(jsonArray.getJSONObject(i).getString("Status"));
                ArrayList<String> Environment = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Environment.add(jsonArray.getJSONObject(i).getJSONObject("Environment").getString("Title"));
                ArrayList<String> Elapsed = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Elapsed.add(jsonArray.getJSONObject(i).getString("Elapsed"));

                // old API support
                ArrayList<String> CaseName = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++)
                {
                    String caseTitle = jsonArray.getJSONObject(i).getJSONObject("Case").optString("Title","null");
                    if(caseTitle.contains("null"))
                    {
                        CaseName.add(CaseName.get(CaseName.size() - 1));
                    }
                    else {
                        CaseName.add(caseTitle);
                    }
                }

                // multiple environment support
                HashSet<String> NotRepeatedNamesSet = new HashSet<String>(CaseName);
                String[] NotRepeatedNames = NotRepeatedNamesSet.toArray(new String[NotRepeatedNamesSet.size()]);
                NotRepeatedNamesSet = null;

                for(int i = 0; i < NotRepeatedNames.length; i++)
                {
                    int count = 0;
                    for(int j = 0; j < CaseName.size(); j++)
                    {
                        if(NotRepeatedNames[i].equals(CaseName.get(j)))
                        {
                            if(count > 0)
                            {
                                CaseName.set(j,String.format("%1$s(%2$d)",CaseName.get(j),count));
                            }
                            count++;
                        }
                    }
                }

                NotRepeatedNames = null;

                logger.logSuiteStarted(ScheduleTitle);

                /// case information
                for (int i = 0; i < jsonArray.length(); i++)
                {

                    logger.logTestStarted(CaseName.get(i));

                    if (Status.get(i).contains("Failed") || (Status.get(i).contains("Done") && doneStatus.contains("Failed")))
                    {

                        JSONArray keyframes = jsonArray.getJSONObject(i).getJSONArray("Keyframes");

                        //KeyframeInfo
                        ArrayList<String> KeyFrameTimeStamp = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameTimeStamp.add(keyframes.getJSONObject(j).getString("Timestamp"));
                        ArrayList<String> KeyFrameLogMessage = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameLogMessage.add(keyframes.getJSONObject(j).getString("LogMessage"));

                        String stackTrace = String.format("Environment: %1$s\n",Environment.get(i));

                        for (int j = 0; j < keyframes.length(); j++)
                        {
                            String level =  ObjectUtils.firstNonNull(keyframes.getJSONObject(j).optString("Level"));

                            if (level.equals("") || level.contains("Trace")) { }
                            else
                            {
                                stackTrace += String.format(MESSAGES.CASE_STACKTRACE_FORMAT, KeyFrameTimeStamp.get(j),  KeyFrameLogMessage.get(j));
                            }
                        }

                        stackTrace += String.format("Elapsed: %1$s",Elapsed.get(i));

                        keyframes = null;

                        logger.logTestFailed(CaseName.get(i), stackTrace , null);
                    }

                    String ElapsedTime[] = Elapsed.get(i).split(":|\\.");
                    double seconds  = Double.parseDouble(ElapsedTime[0]) * 60 * 60 + Double.parseDouble(ElapsedTime[1]) * 60 + Double.parseDouble(ElapsedTime[2]) + Double.parseDouble("0." + ElapsedTime[3]);
                    int miliseconds = (int)(seconds * 1000);
                    ElapsedTime = null;

                    logger.message(TeamcityServiceMessages.testFinishedMessage(CaseName.get(i), Integer.toString(miliseconds)));
                    //logger.logTestFinished(CaseName.get(i));
                }


                logger.logSuiteFinished(ScheduleTitle);

                jsonArray = null;
            }
            else
            {
                InValidSchedules.put(ScheduleId,String.format(MESSAGES.SCHEDULE_HAS_NO_CASES, ScheduleId, str));
            }
        }

        return teamCityMessage;
    }

    private static  void GetSchTitlesOrIds(String uri, List<String> scheduleIds, HashMap<String, String> schedules, HashMap<String, String> InValidSchedules, BuildProgressLogger logger)
    {
        try
        {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();

            client = null;

            JSONArray jsonArray =  new JSONArray(response.getResponseBody()); response = null;

            for (int i = 0; i < scheduleIds.size(); i++)
            {
                boolean success = false;
                for (int j = 0; j < jsonArray.length(); j++)
                {
                    if ( ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Id")).contentEquals(scheduleIds.get(i)))
                    {
                        String title = ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Title"));


                        if (!schedules.containsValue(title))
                        {
                            schedules.put(scheduleIds.get(i), title);
                        }
                        success = true;
                    }
                }

                if (!success) InValidSchedules.put(scheduleIds.get(i), MESSAGES.NO_SUCH_SCHEDULE);

            }

        }
        catch (InterruptedException e) {
            logger.error(e.getMessage());
        } catch (ExecutionException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        catch (Exception e)
        {
            logger.error(MESSAGES.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
            logger.error(TeamcityServiceMessages.buildStatusFailureMessage(MESSAGES.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT +"\n" +  e.getMessage()));
        }
    }

    private static void RunSchedule(String uri, String schId, String schTitle, int current,  MutableBoolean isSuccessfullyLaunchedSchedule,  HashMap<String, String> InValidSchedules, BuildProgressLogger logger)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;

            if (response.getStatusCode() != 204)          // 204 Response means correct schedule launching
            {
                String errorMessage = String.format(MESSAGES.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                logger.error(errorMessage);
                throw new Exception(errorMessage);
            }
            else
            {
                isSuccessfullyLaunchedSchedule.setValue(true);
                logger.message(String.format(MESSAGES.SCHEDULE_RUN_SUCCESS, schTitle, schId));
            }

            return;
        }
        catch (InterruptedException e) {
            logger.error(e.getMessage());
        } catch (ExecutionException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        catch (Exception e){
            String errorMessage = String.format(MESSAGES.SCHEDULE_RUN_FAILURE,  schTitle, schId);

            logger.error(errorMessage);

            InValidSchedules.put(schId,errorMessage);

            isSuccessfullyLaunchedSchedule.setValue(false);

            logger.error(TeamcityServiceMessages.buildStatusFailureMessage(errorMessage + "\n" + e.getMessage()));
        }

    }

    private static void GetScheduleState(String uri, String schId, String schTitle, int current,  MutableBoolean isScheduleStillRunning, String doneStatus,  HashMap<String, String> InValidSchedules, BuildProgressLogger logger)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;

            if(response.getStatusCode() != 200)
            {
                String errorMessage = String.format(MESSAGES.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                logger.error(errorMessage);
                throw new Exception(errorMessage);
            }
            else
            {
                JsonToTeamcity( response.getResponseBody(), current,  isScheduleStillRunning,  doneStatus,  InValidSchedules, logger);
            }
        }
        catch (InterruptedException e) {
            logger.error(e.getMessage());

        } catch (ExecutionException e) {
            logger.error(e.getMessage());

        } catch (IOException e) {
            logger.error(e.getMessage());

        } catch (Exception e)
        {
            String errorMessage = String.format(MESSAGES.SCHEDULE_STATE_FAILURE, schTitle, schId);
            logger.error(errorMessage);
            InValidSchedules.put(schId, errorMessage);
            isScheduleStillRunning.setValue(false);
            logger.error(TeamcityServiceMessages.buildStatusFailureMessage(errorMessage + "\n" + e.getMessage()));
        }
    }


}