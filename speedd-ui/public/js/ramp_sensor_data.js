dataFlowDensity = { flow: [200, 56, 897, 468, 300], density: [5, 12, 20, 8, 25] };

// dataFlowDensity2 is used just so history scroll is implemented
var dataFlowDensity2 = { flow: [200, 56, 897, 468, 300], density: [5, 12, 20, 8, 25] };


var maingraph, ggraph;

function drawRampGraph(rampId) {
    d3.select("#svgPlot").remove();
    d3.select("#titlePlot").remove();

    var margin = { top: 20, right: 60, bottom: 90, left: 60 }
  , width = parseInt(d3.select('#divPlot').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#divPlot').style('height')) - margin.top - margin.bottom;


    // sets up the scales
    var x = d3.scale.linear()
          .domain([0, d3.max(dataRampMetering[rampId].densityHistory, function (d) { return d; })])
          .range([0, width]);
    var y = d3.scale.linear()
          .domain([251, d3.max(dataRampMetering[rampId].rateHistory, function (d) { return d; })])
          .range([height, 0]);

    var scaleCircles = d3.scale.linear()
                    .domain([0, 10])
                    .range([3, 20]);
    var scaleColors = d3.scale.ordinal()
                .domain(["auto", "partial", "full"])
                .range(["steelblue", "yellow", "green"]);

    // appends svg canvas to html container element
    svg = d3.select('#divPlot')
        .append('svg')
        .attr("id", "svgPlot")
        .style("position", "absolute")
        .style("top", "10%")
        .style("left", "0%")
        .style("height", "90%")
        .style("width", "95%")
        /*
            .attr("id","svgPlot")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom)*/
            .attr('class', 'chart');
    // appends chart to svg
    maingraph = svg.append('g')
            .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
            .attr('width', width)
            .attr('height', height)
            .attr('class', 'main');

    var title = d3.select("#divPlotHead").append("text").attr("id", "titlePlot").text("Sensor Data: Ramp "+rampId).style("font-weight", "bold").style("font-size", "20px").style("color", "black");
    // draws the x axis and appends it to chart
    var xAxis = d3.svg.axis()
            .scale(x)
            .orient('bottom');

    maingraph.append('g')
            .attr('transform', 'translate(0,' + height + ')')
            .attr('class', 'x axis')
            .call(xAxis)
       .append("text")
            .attr("class", "label")
            .attr("x", 40)
            .attr("y", -6)
            .style("text-anchor", "end")
            .text("Density");

    // draws the y axis and appends it to chart
    var yAxis = d3.svg.axis()
            .scale(y)
            .orient('left');

    maingraph.append('g')
            .attr('transform', 'translate(0,0)')
            .attr('class', 'y axis')
            .call(yAxis)
        .append("text")
            .attr("class", "label")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Rate");

    // appends dots to chart
    ggraph = maingraph.append("svg:g");

    var datapoints = ggraph.selectAll("circle")
      .data(dataRampMetering[rampId].densityHistory)
      .enter().append("circle")
          .attr("cx", function (d, i) { return x(d); })
          .attr("cy", function (d, i) { return y(dataRampMetering[rampId].rateHistory[i]); })
          .attr("r", function (d, i) {
              if (i < dataRampMetering[rampId].densityHistory.length - 10)
                  return scaleCircles(0);
              else
              {
                  return scaleCircles(i % 10);
              }
          }) // set to change size for last 10
            // sets color according to control type
          .style("fill", function (d, i) {  return scaleColors(dataRampMetering[rampId].controlTypeHistory[i]) }); 
    // add text on mouseover
    datapoints.append("title").text(function (d, i) { return "rate: "+dataRampMetering[rampId].rateHistory[i] + "\ndensity: " + d });

}

// 100% Working
function redrawRampGraph()
{

    var rampId = (d3.select("#rampId").text().substring(0, 2)).toInt();  // determines ramp selected: NOT perfect as it gets is from CONTROL window

    var margin = { top: 20, right: 60, bottom: 90, left: 60 }
  , width = parseInt(d3.select('#divPlot').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#divPlot').style('height')) - margin.top - margin.bottom;


    // sets up the scales
    var x = d3.scale.linear()
          .domain([0, d3.max(dataRampMetering[rampId].densityHistory, function (d) { return d; })])
          .range([0, width]);
    var y = d3.scale.linear()
          .domain([251, d3.max(dataRampMetering[rampId].rateHistory, function (d) { return d; })])
          .range([height, 0]);

    var scaleCircles = d3.scale.linear()
                    .domain([0, 10])
                    .range([3, 20]);
    var scaleColors = d3.scale.ordinal()
                .domain(["auto", "partial", "full"])
                .range(["steelblue", "yellow", "green"]);


//    var title = d3.select("#divPlotHead").append("text").attr("id", "titlePlot").text("Sensor Data: Ramp " + rampId).style("font-weight", "bold").style("font-size", "20px").style("color", "black");

    // updates axes
    var xAxis = d3.svg.axis()
            .scale(x)
            .orient('bottom');
    var yAxis = d3.svg.axis()
            .scale(y)
            .orient('left');
    maingraph.selectAll("g.x.axis").attr('transform', 'translate(0,' + height + ')').call(xAxis);
    maingraph.selectAll("g.y.axis").call(yAxis);

    // appends dots to chart
    ggraph.selectAll("circle")
         .data(dataRampMetering[rampId].densityHistory)
           // updates existing data points
             .attr("cx", function (d, i) { return x(d); })
          .attr("cy", function (d, i) { return y(dataRampMetering[rampId].rateHistory[i]); })
          .attr("r", function (d, i) {
              if (i < dataRampMetering[rampId].densityHistory.length - 10)
                  return scaleCircles(0);
              else
              {
                  return scaleCircles(i % 10);
              }
          }) // set to change size for last 10
            // sets color according to control type
          .style("fill", function (d, i) {  return scaleColors(dataRampMetering[rampId].controlTypeHistory[i]) })
           // adds new data points
         .enter().append("circle")
            .attr("cx", function (d, i) { return x(d); })
          .attr("cy", function (d, i) { return y(dataRampMetering[rampId].rateHistory[i]); })
          .attr("r", function (d, i) {
              if (i < dataRampMetering[rampId].densityHistory.length - 10)
                  return scaleCircles(0);
              else {
                  return scaleCircles(i % 10);
              }
          }) // set to change size for last 10
            // sets color according to control type
          .style("fill", function (d, i) { return scaleColors(dataRampMetering[rampId].controlTypeHistory[i]) });

    // removes redundant data points
    ggraph.selectAll("circle")
       .data(dataRampMetering[rampId].densityHistory)
       .exit()
           .remove();

}


// scrolls through data history
function timeSlide() {

    var percentToShow = document.getElementById('sliderTime').value;


    dataFlowDensity2.flow = dataFlowDensity.flow.slice(0, Math.floor(percentToShow * (dataFlowDensity.flow.length) / 100))
    dataFlowDensity2.density = dataFlowDensity.density.slice(0, Math.floor(percentToShow * (dataFlowDensity.density.length) / 100))


    updateGraphs();
}