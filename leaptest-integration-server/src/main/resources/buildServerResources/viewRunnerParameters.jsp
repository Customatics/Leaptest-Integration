<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
      <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
      <jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
      <jsp:useBean id="constants" class="com.customatics.leaptest_integration.StringConstants" />

      <div class="parameter">

          Leapwork controller hostname: <strong><props:displayValue name="${constants.ParameterName_Hostname}" /></strong>
          Leapwork controller port: <strong><props:displayValue name="${constants.ParameterName_Port}" /></strong>
          Access key: <strong><props:displayValue name="${constants.ParameterName_AccessKey}" /></strong>
          Report File: <strong><props:displayValue name="${constants.ParameterName_Report}" /></strong>
          Time Delay (in seconds): <strong><props:displayValue name="${constants.ParameterName_TimeDelay}" /></strong>
          Done status value: <strong><props:displayValue name="${constants.ParameterName_DoneStatus}" /></strong>
          Write keyframes for passed flows to report? <strong><props:displayValue name="${constants.ParameterName_PassedKeyframes}" /></strong>
          Schedule Variables: <strong><props:displayValue name="${constants.ParameterName_ScheduleVariables}" /></strong>
          Schedule Names: <strong><props:displayValue name="${constants.ParameterName_ScheduleNames}" emptyValue="${constants.ParameterName_ScheduleNames}"/></strong>
          <strong><props:displayValue name="${constants.ParameterName_ScheduleIds}" emptyValue="${constants.ParameterName_ScheduleIds}"/></strong>
      </div>