package com.customatics.leaptest_integration;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.vfs.FilePath;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.joda.time.LocalTime;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;


public final class PluginHandler {

    private static PluginHandler pluginHandler = null;

    private static final String scheduleSeparatorRegex = "\r\n|\n|\\s+,\\s+|,\\s+|\\s+,|,";
    private static final String variableSeparatorRegex = "\\s+:\\s+|:\\s+|\\s+:|:";
    private static final String STRING_EMPTY = "";

    private PluginHandler(){}

    public static PluginHandler getInstance()
    {
        if( pluginHandler == null ) pluginHandler = new PluginHandler();

        return pluginHandler;
    }

    public String getScheduleVariablesRequestPart(String rawScheduleVariables, final BuildProgressLogger logger)
    {
        if(Utils.isBlank(rawScheduleVariables)) return STRING_EMPTY;

        LinkedHashMap<String,String> variables = new LinkedHashMap<>();
        String[] rawSplittedKeyValuePairs = rawScheduleVariables.split(scheduleSeparatorRegex);
        for (String rawKeyValuePair : rawSplittedKeyValuePairs)
        {
            String[] splittedKeyAndValue = rawKeyValuePair.split(variableSeparatorRegex);
            if(splittedKeyAndValue.length < 2){
               logger.warning(String.format(Messages.INVALID_SCHEDULE_VARIABLE,rawKeyValuePair));
                continue;
            }
            String key = splittedKeyAndValue[0];
            String value = splittedKeyAndValue[1];
            if(Utils.isBlank(key) || Utils.isBlank(value))
            {
                logger.warning(String.format(Messages.INVALID_SCHEDULE_VARIABLE,rawKeyValuePair));
                continue;
            }
            if(Utils.tryAddToMap(variables,key,value) == false)
            {
                logger.warning(String.format(Messages.SCHEDULE_VARIABLE_KEY_DUPLICATE,rawKeyValuePair));
                continue;
            }
        }
        if(variables.isEmpty()) return STRING_EMPTY;
        String prefix = "?";
        StringBuilder stringBuilder = new StringBuilder();
        for(Map.Entry<String,String> variable : variables.entrySet())
        {
            stringBuilder.append(prefix).append(variable.getKey()).append("=").append(variable.getValue());
            prefix = "&";
        }

        String variableRequestPart = stringBuilder.toString();
        logger.message(String.format(Messages.SCHEDULE_VARIABLE_REQUEST_PART,variableRequestPart));
        return variableRequestPart;
    }

    public String correctRepeatedTitles(HashMap<String,Integer> repeatedNameMapCounter, String title)
    {
        if(repeatedNameMapCounter.containsKey(title))
        {
            Integer number = repeatedNameMapCounter.get(title);
            title =  String.format("%1$s(%2$d)",title, number);
            number++;
            return title;
        }
        else
        {
            repeatedNameMapCounter.put(title,new Integer(0));
            return title;
        }
    }


    public ArrayList<String> getRawScheduleList(String rawScheduleIds, String rawScheduleTitles)
    {
        ArrayList<String> rawScheduleList = new ArrayList<>();

        String[] schidsArray = rawScheduleIds.split(scheduleSeparatorRegex);
        String[] testsArray = rawScheduleTitles.split(scheduleSeparatorRegex);

        rawScheduleList.addAll(Arrays.asList(schidsArray));
        rawScheduleList.addAll(Arrays.asList(testsArray));
        rawScheduleList.removeIf(sch->sch.trim().length() == 0);

        return rawScheduleList;
    }

    public int getTimeDelay(String rawTimeDelay, final BuildProgressLogger logger)
    {
        int defaultTimeDelay = 5;
        try
        {
            if(!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay))
                return Integer.parseInt(rawTimeDelay);
            else
            {
                logger.warning(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID,defaultTimeDelay));
                return defaultTimeDelay;
            }
        }
        catch (Exception e)
        {
            logger.warning(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID,defaultTimeDelay));
            return defaultTimeDelay;
        }
    }

    public boolean isDoneStatusAsSuccess(String doneStatusAs)
    {
        return doneStatusAs.contentEquals("Success");
    }
    public String getControllerApiHttpAdderess(String hostname, String rawPort, final BuildProgressLogger logger)
    {
        StringBuilder stringBuilder = new StringBuilder();
        int port = getPortNumber(rawPort,logger);
        stringBuilder.append("http://").append(hostname).append(":").append(port);
        return stringBuilder.toString();
    }
    private int getPortNumber(String rawPortStr,final BuildProgressLogger logger)
    {
        int defaultPortNumber = 9001;
        try
        {
            if(!rawPortStr.isEmpty() || !"".equals(rawPortStr))
                return Integer.parseInt(rawPortStr);
            else
            {
                logger.warning(String.format(Messages.PORT_NUMBER_IS_INVALID,defaultPortNumber));
                return defaultPortNumber;
            }
        }
        catch (Exception e)
        {
            logger.warning(String.format(Messages.PORT_NUMBER_IS_INVALID,defaultPortNumber));
            return defaultPortNumber;
        }
    }


    public LinkedHashMap<UUID, String> getSchedulesIdTitleHashMap(
            AsyncHttpClient client,
            String accessKey,
            String controllerApiHttpAddress,
            ArrayList<String> rawScheduleList,
            final BuildProgressLogger logger,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws  Exception {

        LinkedHashMap<UUID, String> schedulesIdTitleHashMap = new LinkedHashMap<>();

        String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, controllerApiHttpAddress);


            try
            {
                Response response = client.prepareGet(scheduleListUri).setHeader("AccessKey",accessKey).execute().get();

                switch (response.getStatusCode()) {
                    case 200:
                        JsonParser parser = new JsonParser();
                        JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

                        for (String rawSchedule : rawScheduleList) {
                            boolean successfullyMapped = false;
                            for (JsonElement jsonScheduleElement : jsonScheduleList) {
                                JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

                                if(jsonSchedule.get("Type").getAsString().contentEquals("TemporaryScheduleInfo")) continue;

                                UUID Id = Utils.defaultUuidIfNull(jsonSchedule.get("Id"), UUID.randomUUID());
                                String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");

                                boolean isEnabled = Utils.defaultBooleanIfNull(jsonSchedule.get("IsEnabled"), false);

                                if (Id.toString().contentEquals(rawSchedule))
                                {
                                    if (!schedulesIdTitleHashMap.containsValue(Title))
                                    {
                                        if(isEnabled)
                                        {
                                            schedulesIdTitleHashMap.put(Id, Title);
                                            logger.message(String.format(Messages.SCHEDULE_DETECTED, Title, rawSchedule));
                                        }
                                        else
                                        {
                                            invalidSchedules.add(new InvalidSchedule(rawSchedule, String.format(Messages.SCHEDULE_DISABLED,Title,Id)));
                                            logger.warning((String.format(Messages.SCHEDULE_DISABLED, Title, Id)));
                                        }
                                    }

                                    successfullyMapped = true;
                                }

                                if (Title.contentEquals(rawSchedule))
                                {
                                    if (!schedulesIdTitleHashMap.containsKey(Id))
                                    {
                                        if(isEnabled)
                                        {
                                            schedulesIdTitleHashMap.put(Id, rawSchedule);
                                            logger.message(String.format(Messages.SCHEDULE_DETECTED,rawSchedule, Id));
                                        }
                                        else
                                        {
                                            invalidSchedules.add(new InvalidSchedule(rawSchedule, String.format(Messages.SCHEDULE_DISABLED,Title,Id)));
                                        }
                                    }

                                    successfullyMapped = true;
                                }
                            }

                            if (!successfullyMapped)
                                invalidSchedules.add(new InvalidSchedule(rawSchedule, Messages.NO_SUCH_SCHEDULE));
                        }
                        break;

                    case 401:
                        StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                        appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                        OnFailedToGetScheduleTitleIdMap(null,errorMessage401.toString(),logger);

                    case 500:
                        StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                        appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                        OnFailedToGetScheduleTitleIdMap(null,errorMessage500.toString(),logger);

                    default:
                        StringBuilder errorMessage = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                        OnFailedToGetScheduleTitleIdMap(null,errorMessage.toString(),logger);

                }
            }
            catch (ConnectException | UnknownHostException e )
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
                OnFailedToGetScheduleTitleIdMap(e,connectionErrorMessage,logger);
            }
            catch (InterruptedException e)
            {
                String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
                OnFailedToGetScheduleTitleIdMap(e,interruptedExceptionMessage,logger);
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof  UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getCause().getMessage());
                    OnFailedToGetScheduleTitleIdMap(e,connectionErrorMessage,logger);
                }
                else
                {
                    String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                    OnFailedToGetScheduleTitleIdMap(e,executionExceptionMessage,logger);
                }
            }
            catch (IOException e)
            {
                String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
                OnFailedToGetScheduleTitleIdMap(e,ioExceptionMessage,logger);
            }

        return schedulesIdTitleHashMap;
    }

    private static HashMap<UUID,String> OnFailedToGetScheduleTitleIdMap(Exception e, String errorMessage, BuildProgressLogger logger) throws Exception {
        logger.error(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
        if(errorMessage != null && errorMessage.isEmpty() == false)
            logger.error(errorMessage);
        else
            errorMessage = Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT;
        if(e == null)
            e = new Exception(errorMessage);
        throw e;
    }

    public UUID runSchedule(
            AsyncHttpClient client,
            String controllerApiHttpAddress,
            String accessKey,
            UUID scheduleId,
            String scheduleTitle,
            final BuildProgressLogger logger,
            LeapworkRun run,
            String scheduleVariablesRequestPart
    ) throws Exception {

        String uri = String.format(Messages.RUN_SCHEDULE_URI, controllerApiHttpAddress, scheduleId.toString(), scheduleVariablesRequestPart);
        try
        {
            Response response = client.preparePut(uri).setHeader("AccessKey",accessKey).setBody("").execute().get();

            switch (response.getStatusCode()) {
                case 200:
                    String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, scheduleTitle, scheduleId);
                    logger.message(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
                    logger.message(successMessage);
                    JsonParser parser = new JsonParser();
                    JsonObject jsonRunObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                    JsonElement jsonRunId = jsonRunObject.get("RunId");
                    String runIdStr = Utils.defaultStringIfNull(jsonRunId);
                    UUID runId = UUID.fromString(runIdStr);
                    return runId;

                case 400:
                    StringBuilder errorMessage400 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage400,Messages.INVALID_VARIABLE_KEY_NAME);
                    return OnScheduleRunFailure(errorMessage400,run,scheduleId,logger);

                case 401:
                    StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                    return OnScheduleRunFailure(errorMessage401,run,scheduleId,logger);

                case 404:
                    StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage404,String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                    return OnScheduleRunFailure(errorMessage404,run,scheduleId,logger);

                case 446:
                    StringBuilder errorMessage446 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage446,Messages.NO_DISK_SPACE);
                    return OnScheduleRunFailure(errorMessage446,run,scheduleId,logger);

                case 455:
                    StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                    return OnScheduleRunFailure(errorMessage455,run,scheduleId,logger);

                case 500:
                    StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    return OnScheduleRunFailure(errorMessage500,run,scheduleId,logger);

                default:
                    StringBuilder errorMessage = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    return OnScheduleRunFailure(errorMessage,run,scheduleId,logger);
            }
        }
        catch (ConnectException | UnknownHostException e)
        {
            OnScheduleRunConnectionFailure(e,logger);
        }
        catch (ExecutionException e)
        {
            if(e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException)
            {
                OnScheduleRunConnectionFailure(e,logger);
            }
            else
            {
                throw e;
            }
        }
        return null;
    }

    private static UUID OnScheduleRunFailure(StringBuilder errorMessage,LeapworkRun failedRun,UUID scheduleId, BuildProgressLogger logger)
    {
        logger.error(String.format(Messages.SCHEDULE_RUN_FAILURE, failedRun.getScheduleTitle(), scheduleId.toString()));
        logger.error(errorMessage.toString());
        failedRun.setError(errorMessage.toString());
        failedRun.incErrors();
        return null;
    }

    private static UUID OnScheduleRunConnectionFailure(Exception e, BuildProgressLogger logger)
    {
        logger.error(String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage()));
        return null;
    }


    public boolean stopRun(String controllerApiHttpAddress, UUID runId, String scheduleTitle,String accessKey ,final BuildProgressLogger logger)
    {
        boolean isSuccessfullyStopped = false;

        logger.error(String.format(Messages.STOPPING_RUN,scheduleTitle,runId));
        String uri = String.format(Messages.STOP_RUN_URI, controllerApiHttpAddress, runId.toString());
        AsyncHttpClient client = new AsyncHttpClient();
        try
        {

            Response response = client.preparePut(uri).setBody("").setHeader("AccessKey",accessKey).execute().get();
            client.close();

            switch (response.getStatusCode())
            {
                case 200:
                    JsonParser parser = new JsonParser();
                    JsonObject jsonStopRunObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                    JsonElement jsonStopSuccessfull = jsonStopRunObject.get("OperationCompleted");
                    isSuccessfullyStopped = Utils.defaultBooleanIfNull(jsonStopSuccessfull,false);
                    if(isSuccessfullyStopped)
                        logger.error(String.format(Messages.STOP_RUN_SUCCESS,scheduleTitle,runId.toString()));
                    else
                        logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
                    break;

                case 401:
                    logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    logger.error(Messages.INVALID_ACCESS_KEY);
                    logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 404:
                    logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    logger.error(String.format(Messages.NO_SUCH_RUN_WAS_FOUND,  runId,scheduleTitle));
                    logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 446:
                    logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    logger.error(Messages.NO_DISK_SPACE);
                    logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 455:
                    logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    logger.error(Messages.DATABASE_NOT_RESPONDING);
                    logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 500:
                    logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    logger.error(Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
                default:
                    logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

            }
        } catch (Exception e)
        {
            logger.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
            logger.error(e.getMessage());
        }
        finally
        {
            client.close();
            return isSuccessfullyStopped;
        }

    }
    public File createJUnitReport(final BuildRunnerContext buildRunnerContext, String JunitReportFile, final BuildProgressLogger logger, RunCollection buildResult) throws Exception
    {
        String TeamCityWorkSpace = buildRunnerContext.getWorkingDirectory().getAbsolutePath();
        String JUnitReportFilePath = Paths.get(TeamCityWorkSpace,JunitReportFile).toString();
        logger.warning(String.format(Messages.TEAMCITY_WORKSPACE_VARIABLE, TeamCityWorkSpace));

        try{
            File reportFile = new File(JUnitReportFilePath);
            if(!reportFile.exists()) reportFile.createNewFile();
                //todo check, maybe possibly do it better
            reportFile.setLastModified(reportFile.lastModified() - 2000);


        try(StringWriter writer = new StringWriter())
        {
            JAXBContext context = JAXBContext.newInstance(RunCollection.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(buildResult, writer);

            try(StringWriter formattedWriter  =  new StringWriter())
            {
                formattedWriter.append(writer.getBuffer().toString().replace("&amp;#xA;","&#xA;"));

                try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()))) {
                    out.print(formattedWriter);
                    out.close();
                }
            }

        }
        return reportFile;
    }
        catch (FileNotFoundException e) {
        logger.error(Messages.REPORT_FILE_NOT_FOUND);
        logger.error(e.getMessage());
        throw new Exception(e);
    } catch (IOException e) {
        logger.error(Messages.REPORT_FILE_CREATION_FAILURE);
        logger.error(e.getMessage());
        throw new Exception(e);
    } catch (JAXBException e) {
        logger.error(Messages.REPORT_FILE_CREATION_FAILURE);
        logger.error(e.getMessage());
        throw new Exception(e);
    }
    }

    public String getRunStatus(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {

        String uri = String.format(Messages.GET_RUN_STATUS_URI, controllerApiHttpAddress, runId.toString());

        Response response = client.prepareGet(uri).setHeader("AccessKey",accessKey).execute().get();

        switch (response.getStatusCode())
        {
            case 200:
                JsonParser parser = new JsonParser();
                JsonObject runStatusObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                JsonElement jsonRunStatus = runStatusObject.get("Status");
                String runStatus = Utils.defaultStringIfNull(jsonRunStatus, "Queued");
                return runStatus;

            case 401:
                StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
                throw new Exception(errorMessage401.toString());

            case 404:
                StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage404,String.format(Messages.NO_SUCH_RUN, runId));
                throw new Exception(errorMessage404.toString());

            case 455:
                StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                throw new Exception(errorMessage455.toString());

            case 500:
                StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                throw new Exception(errorMessage500.toString());

            default:
                String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                throw new Exception(errorMessage);

        }
    }

    public List<UUID> getRunRunItems(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {
        String uri = String.format(Messages.GET_RUN_ITEMS_IDS_URI, controllerApiHttpAddress, runId.toString());

        Response response = client.prepareGet(uri).setHeader("AccessKey",accessKey).execute().get();

        switch (response.getStatusCode())
        {
            case 200:

                JsonParser parser = new JsonParser();
                JsonObject jsonRunItemsObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                JsonElement jsonRunItemsElement = jsonRunItemsObject.get("RunItemIds");

                List<UUID> runItems = new ArrayList<>();

                if(jsonRunItemsElement != null)
                {
                    JsonArray jsonRunItems = jsonRunItemsElement.getAsJsonArray();
                    for(int i = 0; i < jsonRunItems.size(); i++)
                    {
                        UUID runItemId = UUID.fromString(jsonRunItems.get(i).getAsString());
                        runItems.add(runItemId);
                    }
                }

                return runItems;

            case 401:
                StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                throw new Exception(errorMessage401.toString());

            case 404:
                StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage404,String.format(Messages.NO_SUCH_RUN, runId));
                throw new Exception(errorMessage404.toString());

            case 446:
                StringBuilder errorMessage446 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage446,Messages.NO_DISK_SPACE);
                throw new Exception(errorMessage446.toString());

            case 455:
                StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                throw new Exception(errorMessage455.toString());

            case 500:
                StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                throw new Exception(errorMessage500.toString());

            default:
                String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                throw new Exception(errorMessage);

        }
    }

    public RunItem getRunItem(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey,  UUID runItemId, String scheduleTitle,boolean doneStatusAsSuccess, boolean writePassedKeyframes,final BuildProgressLogger logger) throws Exception {

        String uri = String.format(Messages.GET_RUN_ITEM_URI, controllerApiHttpAddress, runItemId.toString());

        Response response = client.prepareGet(uri).setHeader("AccessKey", accessKey).execute().get();

        switch (response.getStatusCode()) {
            case 200:

                JsonParser parser = new JsonParser();
                JsonObject jsonRunItem = parser.parse(response.getResponseBody()).getAsJsonObject();

                //FlowInfo
                JsonElement jsonFlowInfo = jsonRunItem.get("FlowInfo");
                JsonObject flowInfo = jsonFlowInfo.getAsJsonObject();
                JsonElement jsonFlowId = flowInfo.get("FlowId");
                UUID flowId = Utils.defaultUuidIfNull(jsonFlowId, UUID.randomUUID());
                JsonElement jsonFlowTitle = flowInfo.get("FlowTitle");
                String flowTitle = Utils.defaultStringIfNull(jsonFlowTitle);
                JsonElement jsonFlowStatus = flowInfo.get("Status");
                String flowStatus = Utils.defaultStringIfNull(jsonFlowStatus, "NoStatus");

                //EnvironmentInfo
                JsonElement jsonEnvironmentInfo = jsonRunItem.get("EnvironmentInfo");
                JsonObject environmentInfo = jsonEnvironmentInfo.getAsJsonObject();
                JsonElement jsonEnvironmentId = environmentInfo.get("EnvironmentId");
                UUID environmentId = Utils.defaultUuidIfNull(jsonEnvironmentId, UUID.randomUUID());
                JsonElement jsonEnvironmentTitle = environmentInfo.get("EnvironmentTitle");
                String environmentTitle = Utils.defaultStringIfNull(jsonEnvironmentTitle);
                JsonElement jsonEnvironmentConnectionType = environmentInfo.get("ConnectionType");
                String environmentConnectionType = Utils.defaultStringIfNull(jsonEnvironmentConnectionType, "Not defined");

                JsonElement jsonRunId = jsonRunItem.get("AutomationRunId");
                UUID runId = Utils.defaultUuidIfNull(jsonRunId, UUID.randomUUID());

                String elapsed = Utils.defaultElapsedIfNull(jsonRunItem.get("Elapsed"));
                double milliseconds = Utils.defaultDoubleIfNull(jsonRunItem.get("ElapsedMilliseconds"), 0);

                RunItem runItem = new RunItem(flowTitle, flowStatus, milliseconds, scheduleTitle);

                if(flowStatus.contentEquals("Initializing") ||
                        flowStatus.contentEquals("Connecting")   ||
                        flowStatus.contentEquals("Connected")    ||
                        flowStatus.contentEquals("Running")      ||
                        flowStatus.contentEquals("NoStatus")     ||
                        (flowStatus.contentEquals("Passed") && !writePassedKeyframes)  ||
                        (flowStatus.contentEquals("Done") && doneStatusAsSuccess && !writePassedKeyframes))
                {
                    return runItem;
                }
                else
                {
                    Failure keyframes = getRunItemKeyFrames(client,controllerApiHttpAddress,accessKey,runItemId,runItem,scheduleTitle,environmentTitle,logger);
                    runItem.failure = keyframes;
                    return runItem;
                }

            case 401:
                StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                throw new Exception(errorMessage401.toString());

            case 404:
                StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage404,String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
                throw new Exception(errorMessage404.toString());

            case 446:
                StringBuilder errorMessage446 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage446,Messages.NO_DISK_SPACE);
                throw new Exception(errorMessage446.toString());

            case 455:
                StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                throw new Exception(errorMessage455.toString());

            case 500:
                StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                throw new Exception(errorMessage500.toString());

            default:
                String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                throw new Exception(errorMessage);
        }
    }

    public Failure getRunItemKeyFrames(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runItemId, RunItem runItem, String scheduleTitle, String environmentTitle, final BuildProgressLogger logger) throws Exception {

        String uri = String.format(Messages.GET_RUN_ITEM_KEYFRAMES_URI, controllerApiHttpAddress, runItemId.toString());

        Response response = client.prepareGet(uri).setHeader("AccessKey", accessKey).execute().get();

        switch (response.getStatusCode()) {
            case 200:

                JsonArray jsonKeyframes = TryParseKeyframeJson(response.getResponseBody(),logger);

                if(jsonKeyframes != null)
                {
                    StringBuilder fullKeyframes = new StringBuilder(Messages.CASE_CONSOLE_LOG_SEPARATOR);
                    appendLine(fullKeyframes,String.format(Messages.CASE_INFORMATION, runItem.getCaseName(), runItem.getCaseStatus(), runItem.getElapsedTime()));

                    for (JsonElement jsonKeyFrame : jsonKeyframes) {
                        String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "Trace");
                        if (!level.contentEquals("") && !level.contentEquals("Trace")) {
                            String keyFrameTimeStamp = jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsJsonObject().get("Value").getAsString();
                            String keyFrameLogMessage = jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString();
                            String keyFrame = String.format(Messages.CASE_STACKTRACE_FORMAT, keyFrameTimeStamp, keyFrameLogMessage);
                            appendLine(fullKeyframes,keyFrame);
                            //fullKeyframes.append("&#xA;");
                        }

                    }

                    appendLine(fullKeyframes,"Environment: ");
                    fullKeyframes.append(environmentTitle);
                    appendLine(fullKeyframes,"Schedule: ");
                    fullKeyframes.append(scheduleTitle);
                    //logger.warning("Environment: " + environmentTitle);
                    //logger.warning("Schedule: " + scheduleTitle);

                    return new Failure(fullKeyframes.toString());
                }
                else
                {
                    logger.error(Messages.FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY);
                    return new Failure(Messages.FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY);
                }

            case 401:
                logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                logger.error(Messages.INVALID_ACCESS_KEY);
                break;
            case 404:
                logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                logger.error(String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
                break;

            case 446:
                logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                logger.error(Messages.NO_DISK_SPACE);
                break;

            case 455:
                logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                logger.error(Messages.DATABASE_NOT_RESPONDING);
                break;

            case 500:
                logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                logger.error(Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                break;

            default:
                logger.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                break;
        }
        return null;
    }

    private static JsonArray TryParseKeyframeJson(String response,final BuildProgressLogger logger)
    {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonKeyframes = parser.parse(response).getAsJsonArray();
            return jsonKeyframes;
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            return null;
        }
    }

    private void appendLine(StringBuilder stringBuilder, String line)
    {
        if(stringBuilder != null)
        {
            stringBuilder.append(System.getProperty("line.separator"));
            stringBuilder.append(line);
        }
    }

    public String getReportFileName(String rawReportName, String defaultReportName)
    {
        String reportName =  Utils.isBlank(rawReportName) ? defaultReportName : rawReportName;

        if(reportName.contains(".xml") == false)
        {
            reportName = reportName.trim().concat(".xml");
        }

        return reportName;
    }
}
