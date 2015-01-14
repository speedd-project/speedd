
function drawTopBar()
{
    var text1 = d3.select("#divTop1").append("text")
        .attr("id", "textTop1")
        .style("font-size", "20px")
        .text("0");
    var text1title = d3.select("#divTop1").append("p")
        .append("text")
        .style("font-size","14px")
        .text("Transactions Investigated");

    var text2 = d3.select("#divTop2").append("text")
        .attr("id", "textTop2")
        .style("font-size", "20px")
        .text("0");
    var text2title = d3.select("#divTop2").append("p")
        .append("text")
        .style("font-size", "14px")
        .text("Flagged Transactions");

    var text3 = d3.select("#divTop3").append("text")
        .attr("id", "textTop3")
        .style("font-size", "20px")
        .text("0");
    var text3title = d3.select("#divTop3").append("p")
        .append("text")
        .style("font-size", "14px")
        .text("Average Transaction Amount");

    var text4 = d3.select("#divTop4").append("text")
        .attr("id", "textTop4")
        .style("font-size", "20px")
        .text("0");
    var text4title = d3.select("#divTop4").append("p")
        .append("text")
        .style("font-size", "14px")
        .text("Average Transaction Volume");

}

