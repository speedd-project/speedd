// status: 0 - off, 1 - low, 2 - mid, 3 - high
var dataRampMetering = [{ id: 0, location: { lat: 45.159026051681344, lng: 5.699438089504838 }, status: randomInt(0,3) },
                        { id: 1, location: { lat: 45.158661266317345, lng: 5.7016228325665 }, status: randomInt(0,3) },
                        { id: 2, location: { lat: 45.15855156797535, lng: 5.703748483210802 }, status: randomInt(0,3) },
                        { id: 3, location: { lat: 45.15879225685452, lng: 5.707521177828312 }, status: randomInt(0, 3) },
                        { id: 4, location: { lat: 45.157015769011934, lng: 5.712385196238756 }, status: randomInt(0, 3) },
                        { id: 5, location: { lat: 45.15417151476012, lng: 5.715500749647617 }, status: randomInt(0, 3) },
                        { id: 6, location: { lat: 45.1515531338257, lng: 5.721260625869036 }, status: randomInt(0, 3) },
                        { id: 7, location: { lat: 45.15075347550556, lng: 5.729609169065952 }, status: randomInt(0, 3) },
                        { id: 8, location: { lat: 45.151232509294566, lng: 5.736177731305361 }, status: randomInt(0, 3) },
                        { id: 9, location: { lat: 45.15257741780866, lng: 5.740821976214647 }, status: randomInt(0, 3) },
                        { id: 10, location: { lat: 45.154805621135495, lng: 5.744838584214449 }, status: randomInt(0, 3) },
                        { id: 11, location: { lat: 45.157097586932146, lng: 5.748298801481724 }, status: randomInt(0, 3) }]

//var drawing;


function drawRampMetering()
{
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
  , width = parseInt(d3.select('#content').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#content').style('height')) - margin.top - margin.bottom;

    // setting up color scale for junction rects
    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["white", "green"]);

/*    var statusScale = d3.scale.linear()
                .domain([1, 2, 3, 4])
                .range(["o", "l", "m", "h"]);
                */
    var container = d3.select("#rampMeteringDiv");

    var svgRamp = container.select("#rampMeteringSvg")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom);

    var g = svgRamp.append("g");                

    var title = svgRamp.append("text").text("Ramp Metering").style("font-weight", "bold").attr("y", 15);
    
    var drawing = g.selectAll("rect").data(dataRampMetering).enter()
            .append("rect")
                .attr("x", function (d,i) { return ( (((width / 3) / 2) + 30 ) + (i % 3) * (width / 3)) })
                .attr("y", function (d, i) {
                    if (Math.floor(d.id / 3) == 0)
                        return (((height / 4) / 2) + 50);
                    else if (Math.floor(d.id / 3) == 1)
                        return (3*((height / 4) / 2) + 50 );
                    else if (Math.floor(d.id / 3) == 2)
                        return (5 * ((height / 4) / 2) +50 );
                    else if (Math.floor(d.id / 3) == 3)
                        return (7 * ((height / 4) / 2) +50);
                })
                .attr("width", 30)
                .attr("height", 30)
                .style("fill", function (d, i) { return colorscale(d.status); })
            /// EVENTS
            .on("click", function (d) { moveMapToLocation(d.location.lat, d.location.lng); drawControlRamp(d.id); })
            .on("mouseover", function (d) { return d3.select(this).style("fill", "grey") })
            .on("mouseout", function (d) { return d3.select(this).style("fill", function (d, i) { return colorscale(d.status); }) });

    // text on mouse hover
    drawing.append("title").text(function (d) { return d.status });//statusScale(d.status)});

    var labels = g.selectAll("text").data(dataRampMetering).enter()
            .append("text")
                .text(function (d,i) { return d.id; })
                .attr("x", function (d, i) { return ((((width / 3) / 2) + 30) + (i % 3) * (width / 3)) })
                .attr("y", function (d, i) {
                    if (Math.floor(d.id / 3) == 0)
                        return (((height / 4) / 2) + 50);
                    else if (Math.floor(d.id / 3) == 1)
                        return (3 * ((height / 4) / 2) + 50);
                    else if (Math.floor(d.id / 3) == 2)
                        return (5 * ((height / 4) / 2) + 50);
                    else if (Math.floor(d.id / 3) == 3)
                        return (7 * ((height / 4) / 2) + 50);
                });

//    var legendText = svgRamp.append("text").text("Legend").attr("x", 0).attr("y", 35);

             
                
}


function redrawRampMetering()
{
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
  , width = parseInt(d3.select('#content').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#content').style('height')) - margin.top - margin.bottom;


    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["white", "green"]);

    var container = d3.select("#rampMeteringDiv");

    var svgRamp = container.select("#rampMeteringSvg")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom);

    var g = svgRamp.select("g");

    var drawing = g.selectAll("rect").data(dataRampMetering)
        .attr("x", function (d, i) { return ((((width / 3) / 2) +30) + (i % 3) * (width / 3)) })
               .attr("y", function (d, i) {
                   if (Math.floor(d.id / 3) == 0)
                       return (((height / 4) / 2) + 50);
                   else if (Math.floor(d.id / 3) == 1)
                       return (3*((height / 4) / 2) + 50 );
                   else if (Math.floor(d.id / 3) == 2)
                       return (5 * ((height / 4) / 2) +50 );
                   else if (Math.floor(d.id / 3) == 3)
                       return (7 * ((height / 4) / 2) +50);
               })
        .enter()
           .append("rect")
               .attr("x", function (d, i) { return ((((width / 3) / 2) +30) + (i % 3) * (width / 3)) })
               .attr("y", function (d, i) {
                   if (Math.floor(d.id / 3) == 0)
                       return (((height / 4) / 2) + 50);
                   else if (Math.floor(d.id / 3) == 1)
                       return (3 * ((height / 4) / 2) + 50);
                   else if (Math.floor(d.id / 3) == 2)
                       return (5 * ((height / 4) / 2) + 50);
                   else if (Math.floor(d.id / 3) == 3)
                       return (7 * ((height / 4) / 2) + 50);
               })
               .attr("width", 30)
               .attr("height", 30)
               .style("fill", function (d, i) { return colorscale(d.status); });


    var labels = g.selectAll("text").data(dataRampMetering)
        .attr("x", function (d, i) { return ((((width / 3) / 2) + 30) + (i % 3) * (width / 3)) })
            .attr("y", function (d, i) {
                if (Math.floor(d.id / 3) == 0)
                    return (((height / 4) / 2) + 50);
                else if (Math.floor(d.id / 3) == 1)
                    return (3 * ((height / 4) / 2) + 50);
                else if (Math.floor(d.id / 3) == 2)
                    return (5 * ((height / 4) / 2) + 50);
                else if (Math.floor(d.id / 3) == 3)
                    return (7 * ((height / 4) / 2) + 50);
            })
        .enter()
        .append("text")
            .text(function (d, i) { return d.id + 1; })
            .attr("x", function (d, i) { return ((((width / 3) / 2) + 30) + (i % 3) * (width / 3)) })
            .attr("y", function (d, i) {
                if (Math.floor(d.id / 3) == 0)
                    return (((height / 4) / 2) + 50);
                else if (Math.floor(d.id / 3) == 1)
                    return (3 * ((height / 4) / 2) + 50);
                else if (Math.floor(d.id / 3) == 2)
                    return (5 * ((height / 4) / 2) + 50);
                else if (Math.floor(d.id / 3) == 3)
                    return (7 * ((height / 4) / 2) + 50);
            });

}

