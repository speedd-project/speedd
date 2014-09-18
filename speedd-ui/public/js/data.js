var weather;
var roadCond;
var pollution;
var aveSpeed = 100;
var trafficDensity;
var trafficFlow;
var lanesOpen;
var entriesOpen;
//var dataFlowDensity = { flow:[], density:[]};


// function that reads a file and saves contrents to a given array ('saveTo')
function parseData(fileName,saveTo) {
    $(document).ready(function () {
        $.ajax({
            type: "GET",
            url: fileName,
            dataType: "text",
            success: function (data) { processData(data); }
        });
    });

    function processData(allText) {
        var allTextLines = allText.split(/\r\n|\n/);
        var headers = allTextLines[0].split(',');
        var lines = [];

        for (var i = 1; i < allTextLines.length; i++) {
            var data = allTextLines[i].split(',');
            if (data.length == headers.length) {

                var tarr = [];
                for (var j = 0; j < headers.length; j++) {
                    tarr.push(data[j]);
                }
                lines.push(tarr);
                saveTo.push(tarr);
            }
        }
//        console.log(lines.length);
        
    }
}

function computeValues()
{
//    var predictedFlow = [randomInt(400, 1200), randomInt(400, 1200), randomInt(400, 1200), randomInt(400, 1200)];
//    var predictedDensity = [randomInt(5, 40), randomInt(5, 40), randomInt(5, 40), randomInt(5, 40)];

    // remove the previous (4) mock predictions from graph
    dataFlowDensity.flow.splice(dataFlowDensity.flow.length - 4);
    dataFlowDensity.density.splice(dataFlowDensity.density.length - 4);

    // add new flow and density values
    trafficFlow = document.getElementById('sliderFlow').value;
    trafficDensity = document.getElementById('sliderDensity').value;

    // display values on screen labels
    document.getElementById('valFlow').innerHTML = trafficFlow;
    document.getElementById('valDensity').innerHTML = trafficDensity;

    // add flow and density to the array
    dataFlowDensity.flow.push(parseInt(trafficFlow));
    dataFlowDensity.density.push(parseInt(trafficDensity));

    // adds 4 mock predicted values
    for (var i = 0; i < 4; i++)
    {
        dataFlowDensity.flow.push(randomInt(400, 1200));
        dataFlowDensity.density.push(randomInt(5, 40));
    }
    
    // compute speed and distance --- now just random
    speed = trafficFlow / trafficDensity;

    distance = [randomInt(1, 11), randomInt(1, 11), randomInt(1, 11), randomInt(1, 11)];

//    console.log(aveSpeed);

    // update all graphs
    updateGraphs();
    // this is to implement scroll through data history
    timeSlide();
}

function randomInt(min, max) // function that generates a random int between min and max
{
    return Math.floor(Math.random() * (max - min + 1) + min);
}