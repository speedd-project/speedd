var speed = 100;

//var img, img2;

var distance = [2,3,3,5];


function drawDrivers() {
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
  , width = parseInt(d3.select('#divDrivers').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#divDrivers').style('height')) - margin.top - margin.bottom;
 //   console.log(height);

    // sets up the scales
    /*    var x = d3.scale.linear()
              .domain([0, 100])
              .range([0, width]);*/
    var y = d3.scale.linear()
          .domain([0, 350])
          .range([height, 0]);

    var svg = d3.select("#svgDrivers")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom)
            .attr('class', 'chart');

    var title = d3.select("#divDriversHead").append("text").attr("id", "titleDrivers").text("Compliant Drivers").style("font-weight", "bold").style("font-size", "20px");

    var g = svg.append('g')
            .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
            .attr('width', width)
            .attr('height', height)
            .attr('class', 'main');

    // draws the y axis and appends it to chart
    var yAxis = d3.svg.axis()
            .scale(y)
            .orient('left');

    g.append('g')
            .attr('transform', 'translate(0,0)')
            .attr('class', 'y axis')
            .call(yAxis)
        .append("text")
            .attr("class", "label")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Speed");

    // DRAW speed indicator (horizontal line)
    var linePoints = [[{ x: 0, y: speed }, { x: width / 4, y: speed }]];

    var line = d3.svg.line()
        .interpolate("basis")
        .x(function (d) { return (d.x); })
        .y(function (d) { return y(d.y); });

    g.selectAll(".line")
        .data(linePoints)
      .enter().append("path")
        .attr("class", "line")
        .attr("d", line)
        .style('stroke', 'black')
        .style('stroke-width', 4);

    // add the car image to the svg
    var img = g.append("svg:image").attr("id",("carDown"))
        .attr("xlink:href", "img/2d_car_lightgrey_s.png")
        .attr("width", 50)
        .attr("height", 100)
        .attr("x", width / 2 - 35)
        .attr("y", height / 3);

    var img2 = g.append("svg:image").attr("id", ("carUp"))
        .attr("xlink:href", "img/2d_car_lightgrey.png")
        .attr("width", 50)
        .attr("height", 100)
        .attr("x", width / 2 + 35 )
        .attr("y", height / 3);
        
    // add distance indicators
    var color = d3.scale.linear()
          .domain([11, 0])
          .range(['green', 'red'])

    var dist = g.selectAll('rect').data(distance)
            .enter().append('rect')
                .attr('x', function (d, i) { if (i == 0 || i == 1) return width / 2 + 35; else return width / 2 - 35; })
                .attr('y', function (d, i) { if (i == 0 || i == 1) return (height / 3 - 5) - d * 7; else return (height / 3 + 105); })
                .attr('width', function (d, i) { if (i == 1 || i == 3) return 30; else return 50 })
                .attr('height', function (d) { return d * 7 })
                .style('fill', function (d, i) { if (i == 1 || i == 3) return "steelblue"; else return color(d) });
    /*                .append('text')
                        .attr('dx', -20)
                        .text(function (d) { return d });
            */


//    d3.select(window).on('resize', redrawDrivers);
}

function redrawDrivers() {
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
  , width = parseInt(d3.select('#divDrivers').style('width')) - margin.left - margin.right
  , height = parseInt(d3.select('#divDrivers').style('height')) - margin.top - margin.bottom;
//    console.log(height);

    // sets up the scales
    /*    var x = d3.scale.linear()
              .domain([0, 100])
              .range([0, width]);*/
    var y = d3.scale.linear()
          .domain([0, 350])
          .range([height, 0]);

    var svg = d3.select("#svgDrivers")
            .attr('width', width + margin.right + margin.left)
            .attr('height', height + margin.top + margin.bottom)
            .attr('class', 'chart');

    var linePoints = [[{ x: 0, y: speed }, { x: width / 4, y: speed }]];

    var line = d3.svg.line()
        .interpolate("basis")
        .x(function (d) { return (d.x); })
        .y(function (d) { return y(d.y); });

    var g = svg.selectAll('g');

    // draws the y axis and appends it to chart
    var yAxis = d3.svg.axis()
            .scale(y)
            .orient('left');
    g.selectAll("g.y.axis").call(yAxis);

    // UPDATE LINE (speed indicator)
    g.selectAll(".line")
        .data(linePoints).transition().duration(700)
            .attr("class", "line")
            .attr("d", line)
            .style('stroke', 'black')
            .style('stroke-width', 4);
    // update car image
    var img = d3.select("#carDown").attr("x", width / 2 - 35)
       .attr("y", height / 3);

    var img2 = d3.select("#carUp").attr("x", width / 2 + 35)
        .attr("y", height / 3);

    //update distance indicators (rectangles)
    var color = d3.scale.linear()
          .domain([11, 0])
          .range(['green', 'red'])

    var dist = g.selectAll('rect').data(distance)
        .transition().duration(700)
                .attr('x', function (d, i) { if (i == 0 || i == 1) return width / 2 + 35; else return width / 2 - 35; })
                .attr('y', function (d, i) { if (i == 0 || i == 1) return (height / 3 - 5) - d * 7; else return (height / 3 + 105); })
                .attr('width', function (d, i) { if (i == 1 || i == 3) return 30; else return 50 })
                .attr('height', function (d) { return d * 7 })
                .style('fill', function (d, i) { if (i == 1 || i == 3) return "steelblue"; else return color(d) });

}

function updateGraphs()
{
    redraw();
    redrawDrivers();
    redrawRampMetering();
    redrawSuggestions();
}