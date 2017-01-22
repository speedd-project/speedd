function formatData()
{
    // sort json by region
    countriesJson.children.sort(sort_by('region', true, function (a) { return a.toUpperCase() }));
    //////////////////////
    for(i=0; i < countriesJson.children.length; i++)
    {
        countriesJson.children[i].transVolume = 1;//randomInt(5000,1000000);
        countriesJson.children[i].transAmount = 1;//randomInt(5, 1000);
        countriesJson.children[i].flaggedTransactions = randomInt(1, 20);
    }
}

//var node, treemap;
var currentTreeView = "flagged";

function drawTreeMap()
{
    // from example http://bl.ocks.org/mbostock/4063582
    var margin = { top: 40, right: 10, bottom: 10, left: 10 },
        width = parseInt(d3.select("#divMap").style("width")) ,// - margin.left - margin.right,
        height = parseInt(d3.select("#divMap").style("height")) ;// - margin.top - margin.bottom;

    var color = d3.scale.category10();

    var treemap = d3.layout.treemap()
        .size([width, height])
        .sticky(true)
        .value(function (d) {
            if(currentTreeView == "flagged")
                return d.flaggedTransactions;
            else if (currentTreeView == "amount")
                return d.transAmount;
            else if (currentTreeView == "volume")
                return d.transVolume;
        });

    var div = d3.select("#divMap").append("div").attr("id","divTree")
        .style("position", "absolute")
        .style("width", (width) + "px")
        .style("height", (height) + "px")
        .style("left", 0 + "px")
        .style("top", 0 + "px");
//    var div = d3.select("#divMap");

//    d3.json("data/treemapcountries.json", function (error, root) {
        var node = div.datum(countriesJson).selectAll(".node")
            .data(treemap.nodes)
          .enter().append("div")
            .attr("class", "node")
            .call(position)
            .style("background", function (d) { return d.currency ? color(d.region) : null; })
            .text(function (d, i) { return d.name.common; })// ? null : d.name.common; });
            .on("mouseover", function () { d3.select(this).style("cursor", "pointer") })
            // adds on click event to show info
            .on("click", function (d) {
                // clear Selection Info
                d3.select("#textSelectionContainer").selectAll("p").remove();
                // make all text black
                removeTextHighlights();
                    
                // populate info
                d3.select("#textSelectionContainer").append("p").text("Country: "+d.name.common);
                d3.select("#textSelectionContainer").append("p").text("Average Amount: "+d.transAmount);
                d3.select("#textSelectionContainer").append("p").text("Average Volume: "+d.transVolume);
                d3.select("#textSelectionContainer").append("p").text("Flagged: "+d.flaggedTransactions);
            })
        
    //////////////////////////////////////////////////////////////////////////////////////        
    //setting up events
        d3.select("#divTop2").on("mouseover", function () { d3.select(this).style("cursor", "pointer") });
        d3.select("#divTop2").on("click", function () {
            currentTreeView = "flagged";

            var value = this.value === "ccn3"
                    ? function () { return 1; }
                    : function (d) { return d.flaggedTransactions; };
            node
                .data(treemap.value(value).nodes)
                .transition()
                .duration(1500)
                .call(position);
        });


        d3.select("#divTop3").on("mouseover", function () { d3.select(this).style("cursor", "pointer") });
        d3.select("#divTop3").on("click", function () {
            currentTreeView = "amount";

            var value = this.value === "ccn3"
                    ? function () { return 1; }
                    : function (d) { return d.transAmount; };
            node
                .data(treemap.value(value).nodes)
                .transition()
                .duration(1500)
                .call(position);
        });

        
        d3.select("#divTop4").on("mouseover", function () { d3.select(this).style("cursor", "pointer") });
        d3.select("#divTop4").on("click", function () {
            currentTreeView = "volume";

            var value = this.value === "ccn3"
                    ? function () { return 1; }
                    : function (d) { return d.transVolume; };
            node
                .data(treemap.value(value).nodes)
                .transition()
                .duration(1500)
                .call(position);
        });

    /////////////////////////////////////////////////////////
    function position() {
        this.style("left", function (d) { return d.x + "px"; })
            .style("top", function (d) { return d.y + "px"; })
            .style("width", function (d) { return Math.max(0, d.dx - 1) + "px"; })
            .style("height", function (d) { return Math.max(0, d.dy - 1) + "px"; });
    }
}
window.onresize = redrawTreeMap;

function redrawTreeMap()
{
    d3.select("#divTree").remove();
    drawTreeMap();
}


function updateTreemapData(data)
{
    for (i = 0; i < countriesJson.children.length; i++)
    {
//        console.log(countriesJson.children[i].callingCode[0])
        if (countriesJson.children[i].callingCode[0] == data.country)
        {
//            console.log(countriesJson.children[i].callingCode[0]);
//            countriesJson.children[i].transVolume = randomInt(5000, 1000000);
			countriesJson.children[i].transVolume = data.transaction_volume;
//            countriesJson.children[i].transAmount = randomInt(5, 1000);            
			countriesJson.children[i].transAmount = data.average_transaction_amount_eur;
//			countriesJson.children[i].flaggedTransactions = randomInt(50, 10000);
//            countriesJson.children[i].flaggedTransactions += randomInt(50, 10000);
        }
    }
    redrawTreeMap();
}



function resizeTreemapBy(item)/////////doesn't WORK
{
    var value = item;
    node
    .data(treemap.value(d.transAmount).nodes)
        .transition()
        .duration(1500)
        .call(position);
}


// function that sorts a json file
// function from http://stackoverflow.com/questions/979256/sorting-an-array-of-javascript-objects
var sort_by = function (field, reverse, primer) {

    var key = primer ?
        function (x) { return primer(x[field]) } :
        function (x) { return x[field] };

    reverse = [-1, 1][+!!reverse];

    return function (a, b) {
        return a = key(a), b = key(b), reverse * ((a > b) - (b > a));
    }
}

function randomInt(min, max) // function that generates a random int between min and max
{
    return Math.floor(Math.random() * (max - min + 1) + min);
}