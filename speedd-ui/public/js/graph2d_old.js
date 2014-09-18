dataFlowDensity = { flow: [200, 56, 897, 468, 300], density: [5, 12, 20, 8, 25] };

// dataFlowDensity2 is used just so history scroll is implemented
var dataFlowDensity2 = { flow: [200, 56, 897, 468, 300], density: [5, 12, 20, 8, 25] };


var main, g;

function draw()
{
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
  , width = parseInt(d3.select('#content').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#content').style('height')) - margin.top - margin.bottom;

    // sets up the scales
    var x = d3.scale.linear()
          .domain([0, d3.max(dataFlowDensity2.density, function (d) { return d; })])
          .range([0, width]);
    var y = d3.scale.linear()
          .domain([0, d3.max(dataFlowDensity2.flow, function (d) { return d; })])
          .range([height, 0]);

    // appends svg canvas to html container element
    svg = d3.select('#content')
        .select('#svg')
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom)
            .attr('class', 'chart')
    // appends chart to svg
    main = svg.append('g')
            .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
            .attr('width', width)
            .attr('height', height)
            .attr('class', 'main')
    

    // draws the x axis and appends it to chart
    var xAxis = d3.svg.axis()
            .scale(x)
            .orient('bottom');

    main.append('g')
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

    main.append('g')
            .attr('transform', 'translate(0,0)')
            .attr('class', 'y axis')
            .call(yAxis)
        .append("text")
            .attr("class", "label")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Flow");

    // appends dots to chart
    g = main.append("svg:g");

    var datapoints = g.selectAll("circle")
      .data(dataFlowDensity2.density)
      .enter().append("circle")
          .attr("cx", function (d, i) { return x(d); })
          .attr("cy", function (d, i) { return y(dataFlowDensity2.flow[i]); })
          .attr("r", 5);
    // add text on mouseover
    datapoints.append("title").text(function (d, i) { return i });

//    d3.select(window).on('resize', redraw);

    //    addMarkers();

}


function redraw()
{
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
  , width = parseInt(d3.select('#content').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#content').style('height')) - margin.top - margin.bottom;

    // updates scales
    var x = d3.scale.linear()
              .domain([0, d3.max(dataFlowDensity2.density, function (d) { return d; })])
              .range([0, width]);

    var y = d3.scale.linear()
              .domain([0, d3.max(dataFlowDensity2.flow, function (d) { return d; })])
              .range([height, 0]);

    // updates axes
    var xAxis = d3.svg.axis()
            .scale(x)
            .orient('bottom');
    var yAxis = d3.svg.axis()
            .scale(y)
            .orient('left');
    main.selectAll("g.x.axis").attr('transform', 'translate(0,' + height + ')').call(xAxis);
    main.selectAll("g.y.axis").call(yAxis);

    g.selectAll("circle")
      .data(dataFlowDensity2.density)
        // updates existing data points
          .attr("cx", function (d, i) { return x(d); })
          .attr("cy", function (d, i) { return y(dataFlowDensity2.flow[i]); })
          .style("fill", function (d, i) { if (i > (dataFlowDensity2.density.length - 5)) return "steelblue"; else if (i == dataFlowDensity2.density.length - 6) return "yellow"; else return "green";})//"green")
        // adds new data points
      .enter().append("circle")
          .attr("cx", function (d, i) { return x(d); })
          .attr("cy", function (d, i) { return y(dataFlowDensity2.flow[i]); })
          .attr("r", 5)
          .style('fill', function (d, i) { if (i > (dataFlowDensity2.density.length - 5)) return "steelblue"; else if (i > (dataFlowDensity2.density.length - 6)) return "yellow"; else return "green"; }); //'yellow');

        // removes redundant data points
    g.selectAll("circle")
       .data(dataFlowDensity2.density)
       .exit()
           .remove();

}


// scrolls through data history
function timeSlide()
{

    var percentToShow = document.getElementById('sliderTime').value;


    dataFlowDensity2.flow = dataFlowDensity.flow.slice(0, Math.floor(percentToShow * (dataFlowDensity.flow.length) / 100))
    dataFlowDensity2.density = dataFlowDensity.density.slice(0, Math.floor(percentToShow * (dataFlowDensity.density.length) / 100))


    updateGraphs();
}