

function drawOtherInfo()
{
    var title = d3.select("#divOtherInfoHeader").append("text")
    .attr("id", "titleOtherInfo")
    .text("Other Info");

    var text = d3.select("#divOtherInfoContainer")
        .append("p").append("text").attr("id", "textOtherInfoContainer").text("Currency info will go here");
}