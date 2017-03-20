<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="constants" class="LeapTestTeamCityBridgeGroup.StringConstants" />
<jsp:useBean id="runners" class="LeapTestTeamCityBridgeGroup.Runners" />
<jsp:useBean id="donestatuses" class="LeapTestTeamCityBridgeGroup.DoneStatuses" />
<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>




<l:settingsGroup title="Runner Parameters">

    <tr>
        <th>
            <label for="${constants.parameterName_Version}">Plugin core version: </label>
        </th>
        <td>
            <props:selectProperty
                    name="${constants.parameterName_Version}"
                    enableFilter="true"
                    id="LeapTestTeamCityBridgeGroup-version-selector"
                    className="mediumField">
                <c:forEach items="${runners.supportedVersions}" var="ver">
                    <props:option value="${ver}">${ver}</props:option>
                </c:forEach>
            </props:selectProperty>
        </td>
    </tr>

        <tr>
                <th><label>Server URL:</label></th>
                <td>
                    <props:textProperty name="${constants.parameterName_URLs}" className="longField"   />
                    <span class="smallNote">URL of your LeapTest Server</span>
                    <span class="error" id="error_${constants.parameterName_URLs}"></span></td>
                </td>
        </tr>
         <tr>
              <th><label>Time Delay (in seconds):</label></th>
              <td>
                   <props:textProperty name="${constants.parameterName_TimeDelay}" className="longField" value="5"  />
                   <span class="smallNote">How much time to wait before trying to get schedule state. If state is still running, plugin will wait again! </span>
                   <span class="error" id="error_${constants.parameterName_TimeDelay}"></span></td>
              </td>
         </tr>
         <tr>
                 <th>
                     <label for="${constants.parameterName_DoneStatus}">Done status value: </label>
                 </th>
                 <td>
                     <props:selectProperty
                             name="${constants.parameterName_DoneStatus}"
                             enableFilter="true"
                             id="LeapTestTeamCityBridgeGroup-DoneStatus-selector"
                             className="mediumField">
                         <c:forEach items="${donestatuses.supportedDoneStatuses}" var="status">
                             <props:option value="${status}">${status}</props:option>
                         </c:forEach>
                     </props:selectProperty>
                 </td>
         </tr>
        <tr>

            <th>
                <label for="${constants.parameterName_TestNames}">Schedule Names:</label>
            </th>
            <td style="position:relative;">

                <props:multilineProperty
                        name="${constants.parameterName_TestNames}"
                        className="longField"
                        linkTitle="Schedule Names"
                        rows="3"
                        cols="49"
                        expanded="${true}"

                        />

                <span class="error" id="error_${constants.parameterName_TestNames}"></span>
                <span class="smallNote">
                    Press button "Select Schedules" and get all available schedules to run grouped by projects!
                </span>
                <input type="button" class="btn btn-mini" id="mainButton" value="Select Schedules" onclick="GetSch()" style="position:relative; top: -100px; left: 430px;"/>
                <div id="container" class="popupDiv" style="display:none; position:absolute; top: 60px; left: 435px; min-width:250px; max-width:500px"></div>
            </td>
        </tr>
        <tr>
             <td>
                 <props:multilineProperty
                          name="${constants.parameterName_Ids}"
                          className="longField"
                          linkTitle=""
                          rows="3"
                           cols="49"
                           expanded="${true}"

                           />
            </td>
      </tr>
</l:settingsGroup>

<script type="text/javascript" language="JavaScript" src="${teamcityPluginResourcesPath}/buttonscript.js"></script>
<bs:linkScript>
    ${teamcityPluginResourcesPath}/buttonscript.js
</bs:linkScript>

<script type="text/javascript" language="JavaScript">
   var script = document.createElement('script');
   script.type = "text/javascript";
   script.language="JavaScript" ;
   script.src = "${teamcityPluginResourcesPath}/buttonscript.js";
   document.body.appendChild(script);

   script.onload = script.onerror = function() {
     if (!this.executed) {
       this.executed = true;
     }

    var css = 'ul.ul-treefree { padding-left:25px; font-weight: bold; } ul.ul-treefree ul { margin:0; padding-left:6px; } ul.ul-treefree li { position:relative; list-style:none outside none; border-left:solid 1px #999; margin:0; padding:0 0 0 19px; line-height:23px; } ul.ul-treefree li:before { content:""; display:block; border-bottom:solid 1px #999; position:absolute; width:18px; height:11px; left:0; top:0; } ul.ul-treefree li:last-child { border-left:0 none; } ul.ul-treefree li:last-child:before { border-left:solid 1px #999; } ul.ul-dropfree div.drop { width:11px; height:11px; position:absolute; z-index:10; top:6px; left:-6px; background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABYAAAALCAIAAAD0nuopAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAE1JREFUeNpinDlzJgNlgAWI09LScEnPmjWLoAImrHpIAkwMFAMqGMGC6X44GzkIsHoQooAFTTVQKdbAwxOigyMsmIh3MC7ASHnqBAgwAD4CGeOiDhXRAAAAAElFTkSuQmCC"); background-position:-11px 0; background-repeat:no-repeat; cursor:pointer;}',
    head = document.head || document.getElementsByTagName('head')[0],
    style = document.createElement('style');

    style.type = 'text/css';
    if (style.styleSheet){
    style.styleSheet.cssText = css;
    } else {
    style.appendChild(document.createTextNode(css));
    }

    head.appendChild(style);

     var schtextarea = document.getElementById("Ids");
         schtextarea.style.display = 'none';
         schtextarea.readOnly='true';
         var teststextarea = document.getElementById("TestNames");
         teststextarea.readOnly='true';
   };
</script>