
function drawAnalysts()
{
    var title = d3.select("#divAnalysts1Header").append("text")
        .attr("id", "titleAnalysts1")
        .text("Analysts' Workspace");


    ////////////////////////////////////////////////////////////////////////////////////
    // SETS UP THE LOGIC BUILDER
    ////////////////////////////////////////////////////////////////////////////////////

    ////////////////////// VARIABLES ////////////////////

    var time = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "time")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "time_range");
            })
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "5px")
        .style("width", "46px")
        .style("height", "23px")
        .style("background-color", "lime")
        .append("text")
            .attr("id", "textTime")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("time");

    var transactions = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "noTransactions")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "no_transactions");
            })
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "60px")
        .style("width", "129px")
        .style("height", "23px")
        .style("background-color", "peachpuff")
        .append("text")
            .attr("id", "textNoTransactions")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("no. transactions");

    var amount = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "amount")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "amount");
            })
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "197px")
        .style("width", "69px")
        .style("height", "23px")
        .style("background-color", "slateblue")
        .append("text")
            .attr("id", "textAmount")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("amount");

    var location = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "location")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "location");
            })
        .style("position", "absolute")
        .style("top", "5px")
        .style("left", "274px")
        .style("width", "71px")
        .style("height", "23px")
        .style("background-color", "peru")
        .append("text")
            .attr("id", "textLocation")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("location");

    var merchant = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "merchant")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "merchant");
            })
        .style("position", "absolute")
        .style("top", "38px")
        .style("left", "5px")
        .style("width", "83px")
        .style("height", "23px")
        .style("background-color", "steelblue")
        .append("text")
            .attr("id", "textMerchant")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("merchant");
			
	var fraudATM = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "fraudATM")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "FraudAtATM");
            })
        .style("position", "absolute")
        .style("top", "38px")
        .style("left", "96px")
        .style("width", "107px")
        .style("height", "23px")
        .style("background-color", "slategrey")
        .append("text")
            .attr("id", "textFraudATM")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("FraudAtATM");
			
	var increasingAmounts = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "increasingAmounts")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "IncreasingAmounts");
            })
        .style("position", "absolute")
        .style("top", "38px")
        .style("left", "211px")
        .style("width", "153px")
        .style("height", "23px")
        .style("background-color", "honeydew")
        .append("text")
            .attr("id", "textIncreasingAmounts")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("IncreasingAmounts");


    ////////////////////// OPERATORS ////////////////////

    var andB = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "andB")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "AND");
            })
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("left", "5px")
        .style("width", "76px")
        .style("height", "23px")
        .style("background-color", "cadetblue")
        .append("text")
            .attr("id", "textAndB")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("AND (&)");

    var orB = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "orB")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "OR");
            })
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("left", "89px")
        .style("width", "59px")
        .style("height", "23px")
        .style("background-color", "cadetblue")
        .append("text")
            .attr("id", "textOrB")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("OR (|)");

    var notB = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "notB")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "NOT");
            })
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("left", "156px")
        .style("width", "69px")
        .style("height", "23px")
        .style("background-color", "cadetblue")
        .append("text")
            .attr("id", "textNotB")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text("NOT (!)");

    var obr = d3.select("#divAnalysts1Container").append("div")
        .attr("id", "obr")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                var currentText = d3.select("#labelSubmitPattern").text();
                d3.select("#labelSubmitPattern").text(currentText + " " + "(");
            })
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("left", "233px")
        .style("width", "21px")
        .style("height", "23px")
        .style("background-color", "orangered")
        .append("text")
            .attr("id", "textObr")
            .style("position", "absolute")
            .style("top", "3px")
            .style("left", "8px")
            .text(" ( ");

    var cbr = d3.select("#divAnalysts1Container").append("div")
    .attr("id", "cbr")
        .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
        .on("click", function () {
            var currentText = d3.select("#labelSubmitPattern").text();
            d3.select("#labelSubmitPattern").text(currentText + " " + ")");
        })
    .style("position", "absolute")
    .style("bottom", "5px")
    .style("left", "262px")
    .style("width", "21px")
    .style("height", "23px")
    .style("background-color", "orangered")
    .append("text")
        .attr("id", "textCbr")
        .style("position", "absolute")
        .style("top", "3px")
        .style("left", "8px")
        .text(" ) ");
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////
    // SETS UP ANALYSTS2
    ////////////////////////////////////////////////////////////////////////////////////
    var buttonSubmitPattern = d3.select("#divAnalysts2Container").append("button")
        .attr("id", "buttonSubmitPattern")
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("right", "5px")
        .text("Submit Pattern")
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            .on("click", function () {
                // gets the input name
                var patternName = d3.select("#inputNamePattern").property("value");
                if (patternName == "")
                    patternName = "Unnamed"
                // adds the pattern to the PATTERNS investigated WINDOW
                patternsInv.push(patternName);
                // clears the logic label
                d3.select("#labelSubmitPattern").text("");
            });

    var logic = d3.select("#divAnalysts2Container").append("div")
        .attr("id", "divLabelSubmitPattern")
        .style("position", "absolute")
        .style("top", "0px")
        .style("left", "0px")
        .style("width", "100%")
        .style("height", "75px")
        .style("overflow-y", "auto")
            .append("label")
            .attr("id", "labelSubmitPattern")
            .style("position", "absolute")
            .style("top", "5px")
            .style("left", "5px")
            .text("");


    var labelNamePattern = d3.select("#divAnalysts2Container").append("label")
        .attr("id", "labelNamePattern")
        .style("position", "absolute")
        .style("bottom", "30px")
        .style("left", "8px")
        .text("Pattern Name:");
    var namePattern = d3.select("#divAnalysts2Container").append("input")
        .attr("id", "inputNamePattern")
        .style("position", "absolute")
        .style("bottom", "5px")
        .style("left", "5px")
        .style("width", "200px")
        .text("");
}