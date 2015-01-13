// status: 0 - off, 1 - low, 2 - mid, 3 - high

var dataRampMetering = []; // ramp meters are saved here

var rampRateStatus = {"min": 100, "max":101, "minThresh": 100, "maxThresh": 101};

function checkRampStatus(currentRate)
{
	if(currentRate > rampRateStatus.max)
		rampRateStatus.max = currentRate;
	if(currentRate < rampRateStatus.min)
		rampRateStatus.min = currentRate;
		
	rampRateStatus.minThresh = (rampRateStatus.max - rampRateStatus.min) * 0.2 + rampRateStatus.min;
	rampRateStatus.maxThresh = (rampRateStatus.max - rampRateStatus.min) * 0.8 + rampRateStatus.min;
	
	console.log(rampRateStatus);
}

function colorBasedOnRate(rate)
{
	var color = "white";
		
	if(rate <= rampRateStatus.maxThresh && rate >= rampRateStatus.minThresh)
	{
		var colorscale = d3.scale.linear()
                .domain([rampRateStatus.minThresh, rampRateStatus.maxThresh])
                .range(["green", "yellow"]);
		color = colorscale(rate);
	}
	else if (rate > rampRateStatus.maxThresh)
	{
		var colorscale = d3.scale.linear()
                .domain([rampRateStatus.maxThresh, rampRateStatus.max])
                .range(["yellow", "red"]);
		color = colorscale(rate);
	}
	else if (rate < rampRateStatus.minThresh)
	{
		var colorscale = d3.scale.linear()
                .domain([rampRateStatus.min, rampRateStatus.minThresh])
                .range(["white", "green"]);
		color = colorscale(rate);
	}
	else
		color = "white"
		
	return color;
}


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
            var m = new MarkerWithLabel({
                position: new google.maps.LatLng(ramp.location.lat, ramp.location.lng),
                map: map,
                visible: true,
                icon: imgRamp,
                title: ramp.id.toString(),
                labelAnchor: new google.maps.Point(3, -13),
                labelContent: ramp.id.toString(),
                labelClass: "markerlabel" // the CSS class for the label
            });
            // add event to see cam at that point
            google.maps.event.addListener(m, 'click', seeCam);

            ramp.marker = m;


            dataRampMetering.push(ramp);
        }
    }

//    console.log(dataRampMetering);
    setTimeout(drawRampMetering, 200);
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
                .style("fill", function (d, i) { return /*colorscale(d.status)*/colorBasedOnRate(d.status); })
            /// EVENTS                                                                       
            .on("click", function (d) {                              
                moveMapToLocation(d.location.lat, d.location.lng); 
                selectController(d);
                drawRampGraph(d.id);
                // change head color of plot ----- to be changed
                d3.select("#divPlotHead").style("background-color", /*colorscale(d.status)*/colorBasedOnRate(d.status));
                d3.select("#divControlHead").style("background-color", /*colorscale(d.status)*/colorBasedOnRate(d.status));
                // puts border around square
                redrawRampMetering(); //// NOT THE MOST EFFICIENT --- should change to the pilot version method (give rects ids)
                d3.select(this).style("stroke-width", "2px").style("stroke", "black");
            })
            .on("mouseover", function (d) { d3.select(this).style("cursor", "pointer"); return d3.select(this).style("fill", "grey") })
            .on("mouseout", function (d) { return d3.select(this).style("fill", function (d, i) { return /*colorscale(d.status)*/colorBasedOnRate(d.status); }) });

    // text on mouse hover
    drawing.append("title").text(function (d) { return d.status });//statusScale(d.status)});

    var labels = g.selectAll("text").data(dataRampMetering).enter()
            .append("text")
                .text(function (d, i) { return d.id; })
                .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
                .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) });

    //    var legendText = svgRamp.append("text").text("Legend").attr("x", 0).attr("y", 35); 


    ///// INIT TO RAMP 0
//    moveMapToLocation(dataRampMetering[0].location.lat, dataRampMetering[0].location.lng);
    selectController(dataRampMetering[0]);
    drawRampGraph(dataRampMetering[0].id);
    // change head color of plot
    d3.select("#divPlotHead").style("background-color", colorBasedOnRate(dataRampMetering[0].status));
    d3.select("#divControlHead").style("background-color", colorBasedOnRate(dataRampMetering[0].status));
    // puts border around square
//    redrawRampMetering(); //// NOT THE MOST EFFICIENT --- should change to the pilot version method (give rects ids)
//    d3.select(this).style("stroke-width", "2px").style("stroke", "black");
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
        .style("fill", function (d, i) { return colorBasedOnRate(d.status); })
        // removes square border
        .style("stroke-width", "0px")
        .enter()
           .append("rect")
               .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
               .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
               .attr("width", 30)
               .attr("height", 30)
               .style("fill", function (d, i) { return colorBasedOnRate(d.status); });


    var labels = g.selectAll("text").data(dataRampMetering)
        .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
        .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
        .enter()
        .append("text")
            .text(function (d, i) { return d.id + 1; })
                .attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
                .attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) });

}