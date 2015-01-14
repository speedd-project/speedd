var kafkaRuntimeLog = [];
var updateMeteringLog = [];
var predictedCongestionLog = [];
var activeMapCircles = [];

var mesP = { "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 1, "Certainty": 0.2, "average_density": 1.777 } };
var mesP2 = { "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 2, "Certainty": 0.8, "average_density": 1.777 } };
var mesC = { "name": "Congestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 1, "average_density": 1.777 } };
var mesCC = { "name": "ClearCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 0.2 } };

var mesRate = { "name": "UpdateMeteringRateAction", "timestamp": 12151, "attributes": { "density": 15 ,"location": "0024a4dc0000343b", "newMeteringRate": 251.5, "controlType": "auto", "lane": "offramp"} };

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


// TRANSFORMS sensor ID and lane to GPS location
function sensorIdToLatLng(sensorID, lane)
{
    var once = 0;
    var lat = 0;
    var lng = 0;
    var rampId = 0;
	var ln = 0;

    for (var i = 0; i < dataRampMetering.length; i++)
    {
        if (dataRampMetering[i].sensorId == sensorID && dataRampMetering[i].lane == lane && once == 0)
        {
            lat = parseFloat(dataRampMetering[i].location.lat);
            lng = parseFloat(dataRampMetering[i].location.lng);
            rampId = dataRampMetering[i].id;
			ln = dataRampMetering[i].lane
            once++;
        }	
    }
    return { "lat": lat, "lng": lng, "rampId": rampId, "lane": ln};
}

function sensorIdToLatLng2(sensorID)
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
    var pos = sensorIdToLatLng2(m.attributes.location);
    var circle = drawCirclesAlert(pos.lat, pos.lng, m.name, m.attributes.Certainty,m.attributes.problem_id);

    var problem = { name: 0, timestamp: 0, sensorID: 0, problemID: 0, certainty:0, density:0, mapCircle: 0 };
    problem.name = m.name;
    problem.timestamp = m.timestamp;
    problem.sensorID = m.attributes.location;
    problem.problemID = m.attributes.problem_id;
    problem.mapCircle = circle;
    problem.certainty = m.attributes.Certainty;
	problem.density = m.attributes.average_density;

    // stores all detected and predicted congestions
    activeMapCircles.push(problem);
	// moves map to location
	moveMapToLocation(pos.lat,pos.lng);
}


// clears Congestion display on map
function clearCongestion(m)
{
    var pos = sensorIdToLatLng2(m.attributes.location);
    var problemID = m.attributes.problem_id;
	
	var circlesToDelete = [];

    for (var i = activeMapCircles.length-1; i >= 0; i--)
    {
        if (activeMapCircles[i].problemID == problemID)
        {
			// removes circle from map
            activeMapCircles[i].mapCircle.setMap(null);
			// removes circle from active circles array
			activeMapCircles.splice(i,1); // doesn't work if multiple circles have same problemID
        }
    }
}

function updateRateFromMessage(m)
{
    // find the ramp in question
    var posId = sensorIdToLatLng(m.attributes.location, m.attributes.lane);

    // update rates
    dataRampMetering[posId.rampId].status = m.attributes.newMeteringRate;
    dataRampMetering[posId.rampId].densityHistory.push(m.attributes.density);
    dataRampMetering[posId.rampId].rateHistory.push(m.attributes.newMeteringRate);
    dataRampMetering[posId.rampId].controlTypeHistory.push(m.attributes.controlType);

	// check for min and max rates
    checkRampStatus(m.attributes.newMeteringRate);
    // redraw Graphs
    redrawRampMetering();
    redrawRampGraph();
    // update header colors on rate change
    var colorscale = d3.scale.linear()
                .domain([0, 3])
                .range(["yellow", "green"]);
//    d3.select("#divPlotHead").style("background-color", colorscale(dataRampMetering[posId.rampId].status));
//    d3.select("#divControlHead").style("background-color", colorscale(dataRampMetering[posId.rampId].status));

    return dataRampMetering[posId.rampId];

    
}
