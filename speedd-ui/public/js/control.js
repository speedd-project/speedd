

function drawControl() // draws default control panel
{
    removeControl();

    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
    , width = parseInt(d3.select('#controlInfo').style('width')) - margin.left - margin.right
    , height = parseInt(d3.select('#controlInfo').style('height')) - margin.top - margin.bottom;


    var divContainer = d3.select("#controlInfo").append("div").attr("id", "control1")
                    .attr('width', width)
                    .attr('height', height);

    var controlSvg = divContainer.append("svg")
                    .attr('width', 60)
                    .attr('height', 220)

    // add icons for controllable signs
    controlSvg.append("image")
        .attr("xlink:href", "img/speed_icon2.png")
        .attr("width", 60)
        .attr("height", 60)
        .attr("x", 0)
        .attr("y", 20);

    controlSvg.append("image")
        .attr("xlink:href", "img/road_icon.png")
        .attr("width", 60)
        .attr("height", 60)
        .attr("x", 0)
        .attr("y", 90);

    controlSvg.append("image")
        .attr("xlink:href", "img/gp_sign.png")
        .attr("width", 60)
        .attr("height", 60)
        .attr("x", 0)
        .attr("y", 160);

    divContainer.append("text")
        .text("Current")
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "30%");

    divContainer.append("text")
        .text("New")
        .style("position", "absolute")
        .style("top", "5px")
        .style("right", "20%");

    // speed control
    divContainer.append("text").attr("id", "currentSpeed")
        .style("position", "absolute")
        .style("top", "35px")
        .style("left", "30%");
    divContainer.append("input").attr("id", "newSpeed")
        .attr("type","number")
        .style("position", "absolute")
        .style("top", "35px")
        .style("right", "5%");

    // lanes control
/*    divContainer.append("text").attr("id", "currentLanes")
        .style("position", "absolute")
        .style("top", "105px")
        .style("left", "30%");
    divContainer.append("input").attr("id", "newLanes")
        .attr("type", "text")
        .style("position", "absolute")
        .style("top", "105px")
        .style("right", "5%");
        */

    ////////////////////////////////////////////////////////////////////////////
 /*   
    var lanesSvg = divContainer.append("svg").attr("id", "lanesSvg")
        .attr("width", 60)
        .attr("height", 60)
        .style("position", "absolute")
        .style("top", "95px")
        .style("right", "40%");

    lanesSvg.append("line")
        .attr("x1", 0)
        .attr("y1", 0)
        .attr("x2", 0)
        .attr("y2", 60)
        .style("stroke", "black")
        .style("stroke-width", 3);
    lanesSvg.append("rect")
        .attr("height", 30)
        .attr("width", 15)
        .attr("x", 2.5)
        .attr("y", 15)
        .style("fill", "green")
        .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
        //.on("mouseout", function (d) { d3.select(this).style("fill", "green"); })
        .on("click", function () {
            if (d3.select(this).style("fill") == "rgb(0, 128, 0)")
                d3.select(this).style("fill", "red");
            else
                d3.select(this).style("fill", "green");
        });
    lanesSvg.append("image")
        .attr("xlink:href", "img/2d_car_lightgrey.png")
        .attr("width", 20)
        .attr("height", 40)
        .attr("x", 20)
        .attr("y", 15)
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            //.on("mouseout", function (d) { d3.select(this).style("fill", "green"); })
            .on("click", function () {d3.select(this).remove();
            });
 
    lanesSvg.append("line")
        .attr("x1", 20)
        .attr("y1", 0)
        .attr("x2", 20)
        .attr("y2", 60)
        .style("stroke", "black")
        .style("stroke-width", 2)
        .style("stroke-dasharray", ("4,3"));
    lanesSvg.append("line")
        .attr("x1", 40)
        .attr("y1", 0)
        .attr("x2", 40)
        .attr("y2", 60)
        .style("stroke", "black")
        .style("stroke-width", 2)
        .style("stroke-dasharray", ("4,3"));
    lanesSvg.append("line")
        .attr("x1", 60)
        .attr("y1", 0)
        .attr("x2", 60)
        .attr("y2", 60)
        .style("stroke", "black")
        .style("stroke-width", 3);
        
        */
    /////////////////////////////////////////////////////////////////////////////////////////

    // general purpose control
    divContainer.append("text").attr("id", "currentGeneralPurpose")
        .style("position", "absolute")
        .style("top", "175px")
        .style("left", "30%");
    divContainer.append("input").attr("id", "newGeneralPurpose")
        .attr("type", "text")
        .style("position", "absolute")
        .style("top", "175px")
        .style("right", "5%");

    // submit changes button
    divContainer.append("button").attr("id", "submitButton")
        .text("Submit Changes")
        .style("width", "130px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "10px")
        .style("right", "40%")
            .on("click", submitControl);
    // controller id display        
    divContainer.append("text").attr("id","controlId")
        .style("position", "absolute")
        .style("bottom", "40px")
        .style("left", "0px");
}

function removeControl()
{
    // removes defaul control panel
    d3.select("#controlInfo").select("#control1").remove();
    // removes ramp metering control
    d3.select("#controlInfo").select("#control2").remove();
}

function drawControlRamp(id)
{
    removeControl();

    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
    , width = parseInt(d3.select('#controlInfo').style('width')) - margin.left - margin.right
    , height = parseInt(d3.select('#controlInfo').style('height')) - margin.top - margin.bottom;


    var divContainer = d3.select("#controlInfo").append("div").attr("id", "control2")
                    .attr('width', width)
                    .attr('height', height);

    var controlSvg = divContainer.append("svg")
                    .attr('width', width)
                    .attr('height', height)

    var text = controlSvg.append("text")
                    .text(function () { return "Controller Selected:  " + id })
                    .attr("x", 0)
                    .attr("y", 15);



    // submit changes button
    divContainer.append("button").attr("id", "submitButton")
        .text("Submit Changes")
        .style("width", "130px")
        .style("height", "25px")
        .style("position", "absolute")
        .style("bottom", "10px")
        .style("right", "40%")
            .on("click", function () { alert("button Pushed"); });//submitControl);
}