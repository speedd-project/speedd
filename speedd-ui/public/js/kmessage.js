var kafkaRuntimeLog = [];
var updateMeteringLog = [];
var predictedCongestionLog = [];
var activeMapCircles = [];

var mesP = { "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 1, "Certainty": 0.2 } };
var mesP2 = { "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 2, "Certainty": 0.8 } };
var mesC = { "name": "Congestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 0.5 } };
var mesCC = { "name": "ClearCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 0.2 } };

var mesRate = { "name": "UpdateMeteringRateAction", "timestamp": 12151, "attributes": { "density": 15 ,"location": "0024a4dc0000343b", "newMeteringRate": 3, "controlType": "auto" } };

function parseKafkaMessage(message)
{
//	console.log(message.Name);
	
	switch(message.name)
	{
	    case "UpdateMeteringRateAction":
	        updateMeteringLog.push(message);
	        updateRateFromMessage(message);
			break;
	    case "PredictedCongestion":
	        displayCongestion(message);
//			updateSuggestions(["suggestion1","suggestion2","suggestion3"]);
			predictedCongestionLog.push(message);
			break;
	    case "ClearCongestion":
	        clearCongestion(message);
	        break;
	    case "Congestion":
	        displayCongestion(message);
	        break;
	    default:
	        console.log("just testing...");
	        break;
	}
		

	kafkaRuntimeLog.push(message);
}


// TRANSFORMS sensor ID to GPS location
function sensorIdToLatLng(sensorID)
{
    var once = 0;
    var lat = 0;
    var lng = 0;
    var rampId = 0;

    for (var i = 0; i < dataRampMetering.length; i++)
    {
        if (dataRampMetering[i].sensorId == sensorID && once == 0)
        {
            lat = parseFloat(dataRampMetering[i].location.lat);
            lng = parseFloat(dataRampMetering[i].location.lng);
            rampId = dataRampMetering[i].id;
            once++;
        }	
    }
    return { "lat": lat, "lng": lng, "rampId": rampId};
}


// draws a circle at the position of detected and predicted congestion
function displayCongestion(m) 
{
    var pos = sensorIdToLatLng(m.attributes.location);
    var circle = drawCirclesAlert(pos.lat, pos.lng, m.name, m.attributes.Certainty);

    var problem = { name: 0, timestamp: 0, sensorID: 0, problemID: 0, certainty:0, mapCircle: 0 };
    problem.name = m.name;
    problem.timestamp = m.timestamp;
    problem.sensorID = m.attributes.location;
    problem.problemID = m.attributes.problem_id;
    problem.mapCircle = circle;
    problem.certainty = m.attributes.Certainty;

    // stores all detected and predicted congestions
    activeMapCircles.push(problem);
}


// clears Congestion display on map
function clearCongestion(m)
{
    var pos = sensorIdToLatLng(m.attributes.location);
    var problemID = m.attributes.problem_id;

    for (var i = 0; i < activeMapCircles.length; i++)
    {
        if (activeMapCircles[i].problemID == problemID)
        {
            activeMapCircles[i].mapCircle.setMap(null);
        }
    }
}

function updateRateFromMessage(m)
{
    // find the ramp in question
    var posId = sensorIdToLatLng(m.attributes.location);

    // update rates
    dataRampMetering[posId.rampId].status = m.attributes.newMeteringRate;
    dataRampMetering[posId.rampId].densityHistory.push(m.attributes.density);
    dataRampMetering[posId.rampId].rateHistory.push(m.attributes.newMeteringRate);
    dataRampMetering[posId.rampId].controlTypeHistory.push(m.attributes.controlType);


    
    // redraw Graphs
    redrawRampMetering();
    redrawRampGraph();
    // update header colors on rate change
    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["yellow", "green"]);
    d3.select("#divPlotHead").style("background-color", colorscale(dataRampMetering[posId.rampId].status));
    d3.select("#divControlHead").style("background-color", colorscale(dataRampMetering[posId.rampId].status));

    return dataRampMetering[posId.rampId];

    
}
