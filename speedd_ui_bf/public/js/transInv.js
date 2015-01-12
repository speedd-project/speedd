var patternsInv = ["FraudAtATM","IncreasingAmounts"];

patternsInv.push = function () {
    for (var i = 0, l = arguments.length; i < l; i++) {
        this[this.length] = arguments[i];
        
    }
    // when an element is added to activities, log is redrawn
    redrawTransInv();

    return this.length;
}

// removes the clicked pattern according to the name
function removePattern(name)
{
    var container = d3.select("#divTransInvContainer");

    for (var i = 0; i < patternsInv.length; i++)
    {
        if (name == patternsInv[i])
        {
            patternsInv.splice(i, 1);
        }
    }
    redrawTransInv();
}


// draw the patterns investigated window
function drawTransInv()
{
    var title = d3.select("#divTransInvHeader").append("text")
    .attr("id", "titleTransInv")
    .text("Patterns Investigated");

    var container = d3.select("#divTransInvContainer");

    // lists the investigated patterns
    for (var i = 0; i < patternsInv.length; i++)
    {
        container.append("p").append("text").attr("id", "pattern").text(patternsInv[i])
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                // stores the text of current pattern selection
                var selection = d3.select(this).text();

                // removes all highlights
                removeTextHighlights();

                // highlights selection
                d3.select(this).style("color", "steelblue");

                // highlights all flagged transactions of that type
                flaggedLog = d3.selectAll("#textFlaggedTransactions");

                for (var i = 0 ; i<flaggedLog[0].length ; i++)
                {
                    d3.select(flaggedLog[0][i]).style("color", function () {
                        if (selection == d3.select(flaggedLog[0][i]).text().slice(0, selection.length))
                            return "steelblue"
                        else
                            return "black"
                    });
                }
                

                // adds the remove pattern button
                //d3.select(this).append("input")
            });
    }

}

function redrawTransInv()
{
    // removes all patterns listed
    d3.selectAll("#pattern").remove();
    
    var container = d3.select("#divTransInvContainer");

    // updates patterns
    for (var i = 0; i < patternsInv.length; i++) {
        container.append("p").append("text").attr("id", "pattern").text(patternsInv[i])
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                // stores the text of current pattern selection
                var selection = d3.select(this).text();

                // removes all highlights
                removeTextHighlights();

                // highlights selection
                d3.select(this).style("color", "steelblue");

                // highlights all flagged transactions of that type
                flaggedLog = d3.selectAll("#textFlaggedTransactions");

                for (var i = 0 ; i < flaggedLog[0].length ; i++) {
                    d3.select(flaggedLog[0][i]).style("color", function () {
                        if (selection == d3.select(flaggedLog[0][i]).text().slice(0, selection.length))
                            return "steelblue"
                        else
                            return "black"
                    });
                }


                // adds the remove pattern button
                //d3.select(this).append("input")
            });
    }
}

