var activities = ["Road Works at Ramp 2", "Ramp 15 is closed", "Ramp 7 sensor not working", "Accident at Ramp 5", "Ramp 3 in partial control", "Ramp 9 in partial control", "Ramp 12 in partial control", "Ramp 1 in full control"];

// override push function to redraw the log
activities.push = function () {
    for( var i = 0, l = arguments.length; i < l; i++ )
    {
        this[this.length] = arguments[i];
        // when an element is added to activities, log is redrawn
        redrawLog();
    }
    return this.length;
}

function drawLog()
{
    var title = d3.select("#divLogHead").append("text").attr("id", "titleLog").text("Activity Log").style("font-weight", "bold").style("font-size", "20px").style("color", "black");

    var div = d3.select("#divLog2").style("overflow-y", "auto");

    var textLines = div.selectAll("text").data(activities)
      .enter()
        .append("text")
        .attr("id", function (d, i) { return "textLog" + i;})
        .text(function (d, i) { return (i+1) + ". " + activities[i];})
        .style("font-size", "18px")
        .style("position", "absolute")
        .style("left", "30px")
        .style("top", function (d, i) { return 30 * (i + 1) + "px"; });

}


function redrawLog() {
    
    var div = d3.select("#divLog2").style("overflow-y", "auto");

    var textLines = div.selectAll("text")   
        .data(activities)
        // modify existing lines if data changes
            .attr("id", function (d, i) { return "textLog" + i; })
            .text(function (d, i) { return (i + 1) + ". " + activities[i]; })
            .style("font-size", "18px")
            .style("position", "absolute")
            .style("left", "30px")
            .style("top", function (d, i) { return 30 * (i + 1) + "px"; })
        // adds extra lines 
        .enter()
            .append("text")
            .attr("id", function (d, i) { return "textLog" + i; })
            .text(function (d, i) { return (i + 1) + ". " + activities[i]; })
            .style("font-size", "18px")
            .style("position", "absolute")
            .style("left", "30px")
            .style("top", function (d, i) { return 30 * (i + 1) + "px"; });

    // if data is removed from activities var, line gets removed from log
    div.selectAll("text")
         .data(activities)
              .exit().remove();
}