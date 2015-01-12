
function drawSelection()
{
    var title = d3.select("#divSelectionHeader").append("text")
        .attr("id", "titleSelection")
        .text("Selection Info");

    var text = d3.select("#divSelectionContainer")
        .append("p").append("text").attr("id", "textSelectionContainer");

}
