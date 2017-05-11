


    function GetSch()
    {
        if($j('#container').html() == "")
        {

            var json;
            var url = document.getElementById("ServerURL").value + "/api/v1/runSchedules";
            var XHR = ("onload" in new XMLHttpRequest()) ? XMLHttpRequest : XDomainRequest;
            var xhr = new XHR();
            xhr.open('GET', url , true);
            xhr.onload = function () {
                json = JSON.parse(this.responseText);


                var container = document.getElementById("container");
                container.style.display='block';



                var schName = new Array();
                var schId = new Array();
                var schProjectId = new Array();

                for (var i = 0; i < json.length; i++) {
                    if (json[i].IsDisplayedInScheduleList == true) {
                        schId.push(json[i].Id);
                        schName.push(json[i].Title);
                        schProjectId.push(json[i].ProjectId);
                    }
                }

                    var projects = new Array();
                    var projurl = document.getElementById("ServerURL").value + "/api/v1/Projects";
                    var XHRPr = ("onload" in new XMLHttpRequest()) ? XMLHttpRequest : XDomainRequest;
                    var xhrPr = new XHRPr();
                    xhrPr.open('GET', projurl, true);
                    xhrPr.onload = function ()
                    {

                        var projJson = JSON.parse(this.responseText);

                        for(var i = 0; i < projJson.length; i++)
                        {
                        projects.push(projJson[i].Title);
                        }

                        for(var i = 0; i < schProjectId.length; i++)
                        {
                            for(var j = 0; j < projJson.length; j++)
                            {

                                if(schProjectId[i] == projJson[j].Id)
                                {
                                    schProjectId[i] = projJson[j].Title;
                                }
                            }
                        }
                        projJson = null;

                        container.innerHTML += '<br>';

                        var drpdwn = document.createElement('ul');
                        drpdwn.className = 'ul-treefree ul-dropfree';

                        for(var i = 0; i < projects.length; i++)
                        {
                            var projectli = document.createElement('li');

                            var drop = document.createElement('div');
                            drop.class = 'drop';
                            drop.style = 'background-position: 0px 0px;';
                            projectli.appendChild(drop);
                            projectli.innerHTML+=projects[i];

                            var schul = document.createElement('ul');
                            schul.style = 'display:none; font-weight: normal;';

                            for(var j = 0; j < schProjectId.length; j++)
                            {
                                if(projects[i] == schProjectId[j])
                                {
                                    var schli = document.createElement('li');
                                    var chbx = document.createElement('input');
                                    chbx.type = 'checkbox';
                                    chbx.name = schName[j];
                                    chbx.id = i;
                                    chbx.value = schId[j];

                                    schli.appendChild(chbx);
                                    schli.innerHTML+=schName[j];
                                    schul.appendChild(schli);
                                }
                            }

                            projectli.appendChild(schul);
                            drpdwn.appendChild(projectli);
                        }

                        container.appendChild(drpdwn);
                        container.innerHTML += '<br>';

                        container.style.visibility = 'visible';

                        $j(".ul-dropfree").find("li:has(ul)").prepend('<div class="drop"></div>');
                        $j(".ul-dropfree div.drop").click(function()
                        {
                            if ($j(this).nextAll("ul").css('display')=='none')
                            {
                                $j(this).nextAll("ul").slideDown(400);
                                $j(this).css({'background-position':"-11px 0"});
                            } else
                            {
                                $j(this).nextAll("ul").slideUp(400);
                                $j(this).css({'background-position':"0 0"});
                            }
                        });
                        $j(".ul-dropfree").find("ul").slideUp(400).parents("li").children("div.drop").css({'background-position':"0 0"});


                         var boxes = $j("#container input:checkbox");
                         var existingTests = new Array();
                         existingTests = ScheduleNames.value.split("\n");

                        if (ScheduleNames.value != null && ScheduleIds.value != null) {

                        for (var i = 0; i < existingTests.length; i++) {
                            for (j = 0; j < boxes.length; j++) {
                                console.log(boxes[j].getAttributeNode('name').value)
                                if (existingTests[i] == boxes[j].getAttributeNode('name').value) {
                                    $j(boxes[j]).prop('checked', 'checked');

                                }
                            }

                        }

                    }

                    $j("#container input:checkbox").on("change", function ()
                    {
                    var NamesArray = new Array();
                    var IdsArray = new Array();

                    for (var i = 0; i < boxes.length; i++) {
                        var box = boxes[i];
                        if ($j(box).prop('checked')) {
                            NamesArray[NamesArray.length] = $j(box).attr('name');
                            IdsArray[IdsArray.length] = $j(box).val();
                        }
                    }
                    ScheduleNames.value = NamesArray.join("\n");
                    ScheduleIds.value = IdsArray.join("\n");

                    });

                    $j(document).click(function (event) {
                    if ($j(event.target).closest('#container').length == 0 && $j(event.target).attr('id') != 'selectButton')
                     {
                          $j("#container input:checkbox").remove();
                          $j("#container li").remove();
                          $j("#container ul").remove();
                          $j("#container br").remove();

                           container.style.display='none';
                     }
                });


                }
                xhrPr.onerror = function ()
                {
                    alert('"Error occured! Cannot get the list of Projects! Check connection to your server!"' + this.status);
                }
                xhrPr.send();


            }
            xhr.onerror = function () {
                alert('"Error occured! Cannot get the list of schedules! Check connection to your server!"' + this.status);
            }
            xhr.send();
        }
        else
        {
        $j("#container input:checkbox").remove();
        $j("#container li").remove();
        $j("#container ul").remove();
        $j("#container br").remove();
        GetSch();
        }

    }



