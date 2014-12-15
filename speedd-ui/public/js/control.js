
function drawControl() // draws default control panel
{
    //    removeControl();

    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
    , width = parseInt(d3.select('#divControl2').style('width')) - margin.left - margin.right
    , height = parseInt(d3.select('#divControl2').style('height')) - margin.top - margin.bottom;

    var divControl = d3.select("#divControl2");

    var divControlStatus = divControl.append("div")
        .attr("id", "divControlStatus")
        .style("height", "35%")
        .style("width", "100%")
        .style("position", "absolute")
        .style("top", "0px")
//        .style("background-color", "steelblue")
    .attr("class", "border-control-divs");


    var divControlBounds = divControl.append("div")
        .attr("id", "divControlBounds")
        .style("height", "35%")
        .style("width", "100%")
        .style("position", "absolute")
        .style("top", "35%")
//        .style("background-color", "yellow")
        .style("visibility", "hidden")
    .attr("class", "border-control-divs");

    var divControlExplain = divControl.append("div")
        .attr("id", "divControlExplain")
        .style("height", "30%")
        .style("width", "100%")
        .style("position", "absolute")
        .style("top", "70%")
//        .style("background-color", "green")
        .style("visibility", "hidden")
    .attr("class", "border-control-divs");

    var title = d3.select("#divControlHead").append("text").attr("id", "titleControl").text("Ramp Metering Control").style("font-weight", "bold").style("font-size", "20px").style("color", "black");

    // controller id display        
    divControlStatus.append("text").attr("id", "textRampId")
        .text("Controller ID: ")
        .style("font-weight", "bold")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "10px");

    divControlStatus.append("text").attr("id", "rampId")
        .text("No Controller Selected")
        .style("font-weight", "bold")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "5px")
        .style("right", "5%");

    // ramp max rate text

    // ramp min rate text

    // ramp rate control
    divControlStatus.append("text").attr("id", "textCurrentRampMaxRate")
        .text("Current Max Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "30px")
        .style("left", "30px");
    divControlStatus.append("text").attr("id", "currentRampMaxRate")
        .text("N/A")
        .style("position", "absolute")
        .style("top", "30px")
        .style("right", "5%");

    divControlStatus.append("text").attr("id", "textCurrentRampMinRate")
        .text("Current Min Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "60px")
        .style("left", "30px");
    divControlStatus.append("text").attr("id", "currentRampMinRate")
        .text("N/A")
        .style("position", "absolute")
        .style("top", "60px")
        .style("right", "5%");

// puts it in the centre{ return (parseInt(divControl.style("width"))/2-50)+"px"})// 30px

    ////////////////////////////////////////////////////////////////////
    divControlStatus.append("button").attr("id", "challengeButton")
        .text("Challenge")
        .style("width", "100px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("left", "20%")
            .on("click", function () { d3.select("#divControlBounds").style("visibility", "visible");})
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer"); });
    // disables reset levels button
//    document.getElementById("resetLevelsButton").disabled = true;

    divControlStatus.append("button").attr("id", "explainButton")
        .text("Explain")
        .style("width", "100px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("right", "20%")
            .on("click", function () { divControlExplain.style("visibility", "visible") })
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer"); });
    // disables reset levels button
//    document.getElementById("explainButton").disabled = true;

    ////////////////////////////////////////////////////////////////////
    divControlBounds.append("text").attr("id", "textNewRampMaxRate")
        .text("New Max Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "30px");
    divControlBounds.append("input").attr("id", "newRampMaxRate")
        .attr("type", "number")
        .style("width", "80px")
        .style("position", "absolute")
        .style("top", "5px")
        .style("right", "5%");    


    divControlBounds.append("text").attr("id", "textNewRampMinRate")
        .text("New Min Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "30px")
        .style("left", "30px");
    divControlBounds.append("input").attr("id", "newRampMinRate")
        .attr("type", "number")
        .style("width", "80px")
        .style("position", "absolute")
        .style("top", "30px")
        .style("right", "5%");

    // submit changes button
    divControlBounds.append("button").attr("id", "confirmButton")
        .text("Confirm")
        .style("width", "100px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("right", "5%")
            .on("click", submitControl)
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer"); });
    /////////////////////////////////////////////////////////////////////
    divControlExplain.append("text").attr("id", "textNewRampMinRate")
        .text("This is an explanation")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "30px");
    // submit changes button
    divControlExplain.append("button").attr("id", "acknowledgeButton")
        .text("Acknowledge")
        .style("width", "100px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("right", "5%")
            .on("click", function () { divControlExplain.style("visibility", "hidden") })
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer"); });
}

function selectController(ramp)
{
    var divControl = d3.select("#divControl2");

    divControl.select("#rampId").text(ramp.id+ " (" + ramp.sensorId + ")" );

    divControl.select("#currentRampMaxRate").text(ramp.upperLimit);
    divControl.select("#currentRampMinRate").text(ramp.lowerLimit);

    d3.select("#divControlExplain").style("visibility", "hidden");
    d3.select("#divControlBounds").style("visibility", "hidden");

//    document.getElementById("explainButton").disabled = true;
//    document.getElementById("resetLevelsButton").disabled = true;
}


function submitControl() // WORKS but does not update the control panel view immediately
{
    var changes = 0;    // tracks changes
    var upperLimitChanged = 0;
    var lowerLimitChanged = 0;

    var isControllerSelected = 0;
    var rampId = (d3.select("#rampId").text().substring(0, 2)).toInt();

    var divControl = d3.select("#divControl2");

    var id = parseInt(divControl.select("#rampId").text());
    if (divControl.select("#rampId").text() != "No Controller Selected")
        isControllerSelected++;
    

    if ((divControl.select("#newRampMinRate").property("value") != "") && (isControllerSelected!=0))
    {
        // updates control display
        if (divControl.select("#newRampMinRate").property("value") == -1)
            divControl.select("#currentRampMinRate").text("Auto");
        else
            divControl.select("#currentRampMinRate").text(divControl.select("#newRampMinRate").property("value"));

        // updates the ramp
        dataRampMetering[rampId].lowerLimit = divControl.select("#newRampMinRate").property("value");
        lowerLimitChanged++;
        changes++; // track changes
    }
        
    if ((divControl.select("#newRampMaxRate").property("value") != "") && (isControllerSelected!=0))
    {
        // updates control display
        if (divControl.select("#newRampMaxRate").property("value") == -1)
            divControl.select("#currentRampMaxRate").text("Auto");
        else
            divControl.select("#currentRampMaxRate").text(divControl.select("#newRampMaxRate").property("value"));

        // updates the ramp
        dataRampMetering[rampId].upperLimit = divControl.select("#newRampMaxRate").property("value");
        upperLimitChanged++;
        changes++;  // track changes
    }

    if (changes != 0 && isControllerSelected != 0)
    {
        alert("Override Accepted");
        // clear new rate space
        divControl.select("#newRampMinRate").property("value", "")
        divControl.select("#newRampMaxRate").property("value", "");


        // SEND MESSAGE TO SERVER
        if (changes == 2)
        {
            // format the message
            var messageToSend = {
                "name": "setMeteringRateLimits", "attributes":
                    {
                        "location": dataRampMetering[rampId].sensorId,
                        "upperLimit": dataRampMetering[rampId].upperLimit,
                        "lowerLimit": dataRampMetering[rampId].lowerLimit
                    }
            }
            // send the message
			socket.emit('speedd-out-events', JSON.stringify(messageToSend));
        }
        else if (changes==1 && lowerLimitChanged)
        {
            // format the message
            var messageToSend = {
                "name": "setMeteringRateLimits", "attributes":
                    {
                        "location": dataRampMetering[rampId].sensorId,
                        "lowerLimit": dataRampMetering[rampId].lowerLimit
                    }
            }
            // send the message
			socket.emit('speedd-out-events', JSON.stringify(messageToSend));
        }
        else if (changes == 1 && upperLimitChanged) {
            // format the message
            var messageToSend = {
                "name": "setMeteringRateLimits", "attributes":
                    {
                        "location": dataRampMetering[rampId].sensorId,
                        "upperLimit": dataRampMetering[rampId].upperLimit
                    }
            }
            // send the message
//            console.log(JSON.stringify(messageToSend));
			socket.emit('speedd-out-events', JSON.stringify(messageToSend));
        }
    }
    else if (changes == 0 && isControllerSelected != 0)
        alert("ERROR: No values entered");
    else
        alert("ERROR: No controller selected!")
}
