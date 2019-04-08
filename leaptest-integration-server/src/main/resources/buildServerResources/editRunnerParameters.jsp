<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="constants" class="com.customatics.leaptest_integration.StringConstants" />
<jsp:useBean id="donestatuses" class="com.customatics.leaptest_integration.DoneStatuses" />
<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>




<l:settingsGroup title="Runner Parameters">

        <tr>
                <th><label>Leapwork Controller Hostname:</label></th>
                <td>
                    <props:textProperty name="${constants.parameterName_Hostname}" className="longField"/>
                    <span class="smallNote">Leapwork controller hostname</span>
                    <span class="error" id="error_${constants.parameterName_Hostname}"></span></td>
                </td>
        </tr>
         <tr>
                <th><label>Leapwork Controller Port:</label></th>
                    <td>
                            <props:textProperty name="${constants.parameterName_Port}" className="longField"/>
                            <span class="smallNote">Leapwork controller port</span>
                            <span class="error" id="error_${constants.parameterName_Port}"></span></td>
                    </td>
         </tr>
          <tr>
                <th><label>Access Key:</label></th>
                    <td>
                            <props:textProperty name="${constants.parameterName_AccessKey}" className="longField"/>
                            <span class="smallNote">Access key</span>
                            <span class="error" id="error_${constants.parameterName_AccessKey}"></span></td>
                    </td>
        </tr>
         <tr>
               <th><label>Report File:</label></th>
                   <td>
                           <props:textProperty name="${constants.parameterName_Report}" className="longField"/>
                           <span class="smallNote">Report file name</span>
                           <span class="error" id="error_${constants.parameterName_Report}"></span></td>
                   </td>
         </tr>

         <tr>
              <th><label>Time Delay (in seconds):</label></th>
              <td>
                   <props:textProperty name="${constants.parameterName_TimeDelay}" className="longField"/>
                   <span class="smallNote">How much time to wait before trying to get schedule state. If schedule is still running, plugin will wait again! By default it is 5 seconds.</span>
                   <span class="error" id="error_${constants.parameterName_TimeDelay}"></span></td>
              </td>
         </tr>
         <tr>
                 <th><label for="${constants.parameterName_DoneStatus}">Done status as: </label></th>
                 <td>
                     <props:selectProperty
                             name="${constants.parameterName_DoneStatus}"
                             enableFilter="true"
                             id="leaptest-integration-DoneStatus-selector"
                             className="mediumField">
                         <c:forEach items="${donestatuses.supportedDoneStatuses}" var="status">
                             <props:option value="${status}">${status}</props:option>
                         </c:forEach>
                     </props:selectProperty>
                 </td>
         </tr>
         <tr>
                 <th></th>
                     <td>
                             <props:checkboxProperty name="${constants.parameterName_PassedKeyframes}" />
                              <label for="${constants.parameterName_PassedKeyframes}">Write keyframes for passed flows to report?</label>
                             <span class="error" id="error_${constants.parameterName_PassedKeyframes}"></span></td>
                     </td>
         </tr>
         <tr>

             <th><label for="${constants.parameterName_ScheduleVariables}">Schedule Variables:</label></th>
             <td style="position:relative;">

                 <props:multilineProperty
                         name="${constants.parameterName_ScheduleVariables}"
                         className="longField"
                         linkTitle="Schedule Variables"
                         rows="3"
                         cols="49"
                         expanded="${true}"/>
                 <span class="error" id="error_${constants.parameterName_ScheduleVariables}"></span>
                 <span class="smallNote">
                     Input Schedule Variables
                 </span>
             </td>
         </tr>

        <tr>

            <th><label for="${constants.parameterName_ScheduleNames}">Schedule Names:</label></th>
            <td style="position:relative;">

                <props:multilineProperty
                        name="${constants.parameterName_ScheduleNames}"
                        className="longField"
                        linkTitle="Schedule Names"
                        rows="3"
                        cols="49"
                        expanded="${true}"/>
                <span class="error" id="error_${constants.parameterName_ScheduleNames}"></span>
                <span class="smallNote">
                    Press button "Select Schedules" and get all available schedules to run grouped by projects!
                </span>
                <input type="button" class="btn btn-mini" id="selectButton" value="Select Schedules" onclick="GetSch()" style="position:relative; top: -100px; left: 430px;"/>
                <div id="LeapworkContainer" class="popupDiv" style="display:none; position:absolute; top: 60px; left: 435px; min-width:250px; max-width:500px"></div>
            </td>
        </tr>
        <tr style="display:none">
             <td>
                 <props:multilineProperty
                          name="${constants.parameterName_ScheduleIds}"
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

     var schtextarea = document.getElementById("leapworkScheduleIds");
         schtextarea.readOnly='true';

   };
</script>