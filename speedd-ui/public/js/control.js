

function drawControl() // draws default control panel
{
//    removeControl();

    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
    , width = parseInt(d3.select('#divControl2').style('width')) - margin.left - margin.right
    , height = parseInt(d3.select('#divControl2').style('height')) - margin.top - margin.bottom;

    var svgControl = d3.select("#svgControl");
    var divControl = d3.select("#divControl2");


    var title = d3.select("#divControlHead").append("text").attr("id", "titleControl").text("Ramp Metering Control").style("font-weight", "bold").style("font-size", "20px").style("color", "black");

    // controller id display        
    divControl.append("text").attr("id", "textRampId")
        .text("Controller ID: ")
        .style("font-weight", "bold")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "20px")
        .style("left", "10px");

    divControl.append("text").attr("id", "rampId")
        .text("No Controller Selected")
        .style("font-weight", "bold")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "20px")
        .style("right", "5%");

    // ramp max rate text

    // ramp min rate text

    // ramp rate control
    divControl.append("text").attr("id", "textCurrentRampMaxRate")
        .text("Current Maximum Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "60px")
        .style("left", "30px");
    divControl.append("text").attr("id", "currentRampMaxRate")
        .text("N/A")
        .style("position", "absolute")
        .style("top", "60px")
        .style("right", "5%");


    divControl.append("text").attr("id", "textNewRampMaxRate")
        .text("New Maximum Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "90px")
        .style("left", "30px");
    divControl.append("input").attr("id", "newRampMaxRate")
        .attr("type", "number")
        .style("width", "80px")
        .style("position", "absolute")
        .style("top", "90px")
        .style("right", "5%");
///////////////////////////////////////////////////////////////////////
    divControl.append("text").attr("id", "textCurrentRampMinRate")
        .text("Current Minimum Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "150px")
        .style("left", "30px");
    divControl.append("text").attr("id", "currentRampMinRate")
        .text("N/A")
        .style("position", "absolute")
        .style("top", "150px")
        .style("right", "5%");

    
    divControl.append("text").attr("id", "textNewRampMinRate")
        .text("New Minimum Rate")
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("top", "180px")
        .style("left", "30px");
    divControl.append("input").attr("id", "newRampMinRate")
        .attr("type", "number")
        .style("width", "80px")
        .style("position", "absolute")
        .style("top", "180px")
        .style("right", "5%");
/////////////////////////////////////////////////////////////////////////
    // submit changes button
    divControl.append("button").attr("id", "submitButton")
        .text("Submit Changes")
        .style("width", "130px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "20px")
        .style("right", "5%")
            .on("click", submitControl)
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer");});
    
}

function selectController(ramp)
{
    var divControl = d3.select("#divControl2");

    divControl.select("#rampId").text(ramp.id+ " (" + ramp.sensorId + ")" );

    divControl.select("#currentRampMaxRate").text(ramp.upperLimit);
    divControl.select("#currentRampMinRate").text(ramp.lowerLimit);       
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
//            console.log(messageToSend);
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
//            console.log(messageToSend);
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
//            console.log(messageToSend);
        }
    }
    else if (changes == 0 && isControllerSelected != 0)
        alert("ERROR: No values entered");
    else
        alert("ERROR: No controller selected!")
}
