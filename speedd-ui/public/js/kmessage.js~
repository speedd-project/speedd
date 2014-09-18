var kafkaRuntimeLog = [];
var updateMeteringLog = [];
var predictedCongestionLog = [];

function parseKafkaMessage(message)
{
//	console.log(message.Name);
	
	switch(message.Name)
	{
		case "UpdateMeteringRateAction": updateMeteringLog.push(message);
						 
						 break;
		case "PredictedCongestion": displayCongestion(message);
					    updateSuggestions(["suggestion1","suggestion2","suggestion3"]);
					    predictedCongestionLog.push(message);
					    break;
			
		default: console.log("just testing..."); break;
	}
		

	kafkaRuntimeLog.push(message);
}


function displayCongestion(m) // draws a circle at the position of predicted congestion
{
	var once = 0;
	for (var i = 0; i < sensorPos.length; i++)
	{
		if(sensorPos[i].location == m.attributes.location && once == 0)
		{
			drawCirclesAlert(parseFloat(sensorPos[i].gps.lat),parseFloat(sensorPos[i].gps.lng));
			once++;
		}	
	}
}
