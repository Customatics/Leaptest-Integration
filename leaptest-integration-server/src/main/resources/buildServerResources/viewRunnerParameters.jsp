<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
      <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
      <jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
      <jsp:useBean id="constants" class="com.customatics.leaptest_integration.StringConstants" />

      <div class="parameter">

          Leaptest controller URL: <strong><props:displayValue name="${constants.ParameterName_LeaptestControllerURL}" /></strong>
          Time Delay (in seconds): <strong><props:displayValue name="${constants.ParameterName_TimeDelay}" /></strong>
          Done status value: <strong><props:displayValue name="${constants.ParameterName_DoneStatus}" /></strong>
          Schedule Names: <strong><props:displayValue name="${constants.ParameterName_ScheduleNames}" emptyValue="${constants.ParameterName_ScheduleNames}"/></strong>
          <strong><props:displayValue name="${constants.ParameterName_ScheduleIds}" emptyValue="${constants.ParameterName_ScheduleIds}"/></strong>
      </div>