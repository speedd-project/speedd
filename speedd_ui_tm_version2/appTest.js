var express = require("express");
var fs = require('fs');
var Converter=require("csvtojson").core.Converter;
var http = require('http');
var path = require('path');
var io;
var outputFile;

var eventList=[];
var rampLoc;
var csvFileName="data/sensorpos.csv";
var fileStream=fs.createReadStream(csvFileName);
//new converter instance 
var csvConverter=new Converter({constructResult:true});
 
//end_parsed will be emitted once parsing finished 
csvConverter.on("end_parsed",function(jsonObj){
   rampLoc=jsonObj;
});
fileStream.pipe(csvConverter);


var mainRoadSensors;
var csvFileName2="data/mainroad.csv";
var fileStream2=fs.createReadStream(csvFileName2);
//new converter instance 
var csvConverter2=new Converter({constructResult:true});
 
//end_parsed will be emitted once parsing finished 
csvConverter2.on("end_parsed",function(jsonObj){
   mainRoadSensors=jsonObj;
});
fileStream2.pipe(csvConverter2);


//////////////FOR ZURICH
var readline = require('readline');

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

var testMessages = [
	{ "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343f", "problem_id": 3, "Certainty": 0.2, "average_density": 0.6 } },
	{ "name": "ClearCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343f", "problem_id": 3, "Certainty": 0.2 , "average_density": 0.3} },
	{ "name": "Congestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343f", "problem_id": 3, "Certainty": 1, "average_density": 0.78 } },
	{ "name": "SetTrafficLightPhases", "timestamp": 1424439193610, "attributes": { "density": 0.89 ,"location": "0024a4dc0000343b", "phase_id": 2, "phase_time": 50, "controlType": "auto", "lane": "offramp"} },
	{ "name": "SetTrafficLightPhases", "timestamp": 1424439205378, "attributes": { "density": 0.76 ,"location": "0024a4dc0000343b", "phase_id": 2, "phase_time": 12, "controlType": "auto", "lane": "offramp"} },
	{ "name": "PossibleIncident", "timestamp": 1424439205378, "attributes": {"problem_id": "4052", "density": 0.76 ,"location": "0024a4dc0000343b", "Certainty": 0.6, "OccurrenceTime": 1426341062831} },
	{ "name": "AverageOffRampValuesOverInterval", "timestamp": 1426341062831, "attributes": { "OccurrenceTime": 1426341062831, "average_occupancy": 0.7 ,"location": "0024a4dc00003356", "average_speed": 150, "average_flow": 90} },
    { "name": "AverageOnRampValuesOverInterval", "timestamp": 1426341062831, "attributes": {"OccurrenceTime": 1426341062831, "average_occupancy": 0.7 ,"location": "0024a4dc00003356", "average_speed": 150, "average_flow": 90} },
	{ "name": "ClearRampOverflow", "timestamp": 1426341055520, "attributes": { "density": 76 ,"location": "0024a4dc0000343b", "newMeteringRate": 12, "controlType": "auto", "lane": "offramp"} },
	{ "name": "PredictedRampOverflow", "timestamp": 1426341062831, "attributes": { "density": 27 ,"location": "0024a4dc0000343b", "newMeteringRate": 39, "controlType": "auto", "lane": "onramp"} },
	{ "name": "AverageDensityAndSpeedPerLocation", "timestamp": 1426341062831, "attributes": {"OccurrenceTime": 1426341055520, "average_density": 0.1 ,"location": "0024a4dc00003356", "average_speed": 39, "average_flow": 30} },
	{ "name": "AverageDensityAndSpeedPerLocation", "timestamp": 1426341062831, "attributes": {"OccurrenceTime": 1426341055520, "average_density": 0.7 ,"location": "0024a4dc00003356", "average_speed": 150, "average_flow": 90} }
];

var stdin = process.openStdin(); 
require('tty').setRawMode(true);    

stdin.on('keypress', function (chunk, key) {
  process.stdout.write('Get Chunk: ' + chunk + '\n');
  console.log(JSON.stringify(testMessages[chunk]));
  io.emit('speedd-out-events', JSON.stringify(testMessages[chunk]));
  if (testMessages[chunk]!= undefined)
	eventList.push(testMessages[chunk]);
  if (key && key.ctrl && key.name == 'c') process.exit();
});
/////

var app = express();
app.set('port', 3000);
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());

// Serve up our static resources

app.get('/', function(req, res) {
  fs.readFile('index.html', function(err, data) {
    res.end(data);
  });

});
var ser = http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));

  setSocket();

});

function setSocket(){
	io = require('socket.io')(ser);
	console.log("Setting up Client-Server communication");
	
	
	io.on('connection', function (socket) {
		
		// get client address
		var address = socket.handshake.address;
		// create id to send to client
		var clientId = address.toString() + "_" + new Date().getTime().toString();
		console.log(clientId);
		socket.emit('news', { yourId: clientId });
		
		console.log("Connection Established");
		// sends a list of all ramps and main road sensors
		socket.emit('ramp-list', { rampLoc: rampLoc, mainRoadSensors: mainRoadSensors });
		// sends a list of already identified events
		socket.emit('event-list',{ eventList: JSON.stringify(eventList)});

		socket.on('my other event', function (data) {
			console.log(data);
		});
		socket.on('speedd-out-events', function (data) {
			console.log(data);
			// on data received from client upate eventList
			eventList.push(JSON.parse(data));
		});
		
	});
	
}
