function drawSuggestions()
{
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
 , width = parseInt(d3.select('#divSuggest').style('width')) - margin.left - margin.right
 , height = parseInt(d3.select('#divSuggest').style('height')) - margin.top - margin.bottom;


    var color = d3.scale.linear()
          .domain([100, 0])
          .range(['green', 'red'])

    var svg = d3.select("#svgSuggest")
        .attr('width', width + margin.right + margin.left)
        .attr('height', height + margin.top + margin.bottom);

    var title = d3.select("#divSuggestHead")//.append("svg").attr("width", width + margin.right + margin.left).attr("height", height + margin.top + margin.bottom)
        .append("text").attr("id", "titleSuggest")
        .text("Suggestions")
 //       .style("color","grey")
        .style("font-weight", "bold")
        .style("font-size", "20px");//.style("font-size", function () { return height / 10 + "px";});

    var g = svg.append('g');

    var s1 = g.append("text").attr("id", "s1")
//        .style("font-size", function () { return height/10})
        .text("1. Reduce the speed to 70 km/h")
        .attr("x", 0)
        .attr("y", 40);
    
    var s2 = g.append("text").attr("id", "s2")
        .text("2. Change ramp metering rate at J11")
        .attr("x", 0)
        .attr("y", 80);

    var s3 = g.append("text").attr("id", "s3")
        .text("3. Set message to: Beware, Accident Ahead! ")
        .attr("x", 0)
        .attr("y", 120);

    var c = g.append("text")
        .text("Confidence Level")
        .attr("x", 0)
        .attr("y", 180);

    var clevel = g.append("rect").attr("id", "clevel")
        .attr("x", 150)
        .attr("y", 165)
        .attr("height", 25)
        .attr("width", 60)
        .style("fill", function () { return color(93) });

    var clabel = g.append("text")
        .text("93%")
        .attr("x", 180)
        .attr("y", 180)
}


// DOESN't WORK 
function redrawSuggestions()
{
    var margin = { top: 20, right: 15, bottom: 60, left: 60 }
 , width = parseInt(d3.select('#divSuggestHead').style('width')) - margin.left - margin.right
 , height = parseInt(d3.select('#divSuggestHead').style('height')) - margin.top - margin.bottom;

    var title = d3.select("#titleSuggest").style("font-size", function () { return height + "px";});
}

// suggestions is an array of 3 strings
function updateSuggestions(suggestions) {
    d3.select("#s1").text("1. " + suggestions[0]);
    d3.select("#s2").text("2. " + suggestions[1]);
    d3.select("#s3").text("3. " + suggestions[2]);
}
