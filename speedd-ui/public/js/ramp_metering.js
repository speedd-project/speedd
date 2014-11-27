// status: 0 - off, 1 - low, 2 - mid, 3 - high

//var drawing;

var dataRampMetering = []; // ramp meters are saved here



function getRamps()
{
    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["yellow", "green"]);

    /// get ramp metering locations
    for (var i = 0; i < sensorPos.length; i++) {
        if (sensorPos[i].lane == "onramp" || sensorPos[i].lane == "offramp") {
            var ramp = { id: 0, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0};
            ramp.id = dataRampMetering.length;
            ramp.location.lat = sensorPos[i].gps.lat;
            ramp.location.lng = sensorPos[i].gps.lng;
            ramp.sensorId = sensorPos[i].location;

            // adds markers
            var m = new google.maps.Marker({
                position: new google.maps.LatLng(ramp.location.lat, ramp.location.lng),
                map: map,
                visible: true,
                icon: imgRamp,
                title: ramp.id.toString()
            });
            // add event to see cam at that point
//            google.maps.event.addListener(m, 'click', function () {
//                moveMapToLocation(d.location.lat, d.location.lng);
//                selectController(ramp.id);
//                drawRampGraph(ramp.id);
                // change head color of plot
//                d3.select("#divPlotHead").style("background-color", colorscale(ramp.status));
//                d3.select("#divControlHead").style("background-color", colorscale(ramp.status));
//            });//seeCam);

            ramp.marker = m;


            dataRampMetering.push(ramp);
        }
    }

//    console.log(dataRampMetering);
    setTimeout(drawRampMetering, 100);
}

function drawRampMetering()
{
/// doesn't work ------    getRamps();

    var margin = { top: 10, right: 10, bottom: 10, left: 10 }
  , width = parseInt(d3.select('#divRamp').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#divRamp').style('height')) - margin.top - margin.bottom;

    // setting up color scale for junction rects
    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["yellow", "green"]);

/*    var statusScale = d3.scale.linear()
                .domain([1, 2, 3, 4])
                .range(["o", "l", "m", "h"]);
                */
    var container = d3.select("#divRamp");

    var svgRamp = container.select("#svgRamp")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom);

    var g = svgRamp.append("g");                

    var title = d3.select("#divRampHead").append("text").attr("id", "titleRamp").text("Ramp Metering").style("font-weight", "bold").style("font-size", "20px").style("color", "black");

    var squareWidth = parseInt(width / 5);
    var squareXDist = parseInt((squareWidth - 30) / 2);
    var squareHeight = parseInt(height / 4);
    var squareYDist =  parseInt((squareHeight - 30) / 2);
    
    var drawing = g.selectAll("rect").data(dataRampMetering).enter()
            .append("rect")
                .attr("x", function (d, i) { return ( squareXDist + ((30 + squareXDist * 2)* (i%5))) })
                .attr("y", function (d, i) { return ( squareYDist + ((30 + squareYDist * 2)* Math.floor(i/5))) })
                .attr("width", 30)
                .attr("height", 30)
                .style("fill", function (d, i) { return colorscale(d.status); })
            /// EVENTS                                                                       
            .on("click", function (d) {                              //   needs to be CHANGED
                moveMapToLocation(d.location.lat, d.location.lng); /*drawControlRamp(d.id); */
                selectController(d);
                drawRampGraph(d.id);
                // change head color of plot
                d3.select("#divPlotHead").style("background-color", colorscale(d.status));
                d3.select("#divControlHead").style("background-color", colorscale(d.status));
            })
            .on("mouseover", function (d) { d3.select(this).style("cursor", "pointer"); return d3.select(this).style("fill", "grey") })
            .on("mouseout", function (d) { return d3.select(this).style("fill", function (d, i) { return colorscale(d.status); }) });

    // text on mouse hover
    drawing.append("title").text(function (d) { return d.status });//statusScale(d.status)});

    var labels = g.selectAll("text").data(dataRampMetering).enter()
            .append("text")
                .text(function (d, i) { return d.id; })
                .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
                .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) });

//    var legendText = svgRamp.append("text").text("Legend").attr("x", 0).attr("y", 35);               
}


function redrawRampMetering()
{
    var margin = { top: 10, right: 10, bottom: 10, left: 10 }
  , width = parseInt(d3.select('#divRamp').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#divRamp').style('height')) - margin.top - margin.bottom;


    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["yellow", "green"]);

    var container = d3.select("#divRamp");

    var svgRamp = container.select("#svgRamp")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom);

    var g = svgRamp.select("g");


    var squareWidth = parseInt(width / 5);
    var squareXDist = parseInt((squareWidth - 30) / 2);
    var squareHeight = parseInt(height / 4);
    var squareYDist = parseInt((squareHeight - 30) / 2);

    var drawing = g.selectAll("rect").data(dataRampMetering)
        .attr("x", function (d, i) { return ( squareXDist + ((30 + squareXDist * 2)* (i%5))) })
        .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
        .style("fill", function (d, i) { return colorscale(d.status); })
        .enter()
           .append("rect")
               .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
               .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
               .attr("width", 30)
               .attr("height", 30)
               .style("fill", function (d, i) { return colorscale(d.status); });


    var labels = g.selectAll("text").data(dataRampMetering)
        .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
        .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
        .enter()
        .append("text")
            .text(function (d, i) { return d.id + 1; })
                .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
                .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) });

}