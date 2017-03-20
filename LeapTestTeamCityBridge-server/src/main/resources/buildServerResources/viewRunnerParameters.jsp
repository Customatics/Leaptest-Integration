<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
      <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
      <jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
      <jsp:useBean id="constants" class="LeapTestTeamCityBridgeGroup.StringConstants" />

      <div class="parameter">
          Plugin core version: <strong><props:displayValue name="Plugin core version" /></strong>
          Server URL: <strong><props:displayValue name="${constants.ParameterName_URLs}" /></strong>
          Time Delay (in seconds): <strong><props:displayValue name="${constants.ParameterName_TimeDelay}" /></strong>
          Done status value: <strong><props:displayValue name="Done status value" /></strong>
          Schedule Names: <strong><props:displayValue name="${constants.ParameterName_TestNames}" emptyValue="${constants.ParameterName_TestNames}"/></strong>
          <strong><props:displayValue name="${constants.ParameterName_Ids}" emptyValue="${constants.ParameterName_Ids}"/></strong>
      </div>