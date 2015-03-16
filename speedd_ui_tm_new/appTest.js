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


//////////////FOR ZURICH
var readline = require('readline');

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

var testMessages = [
	{ "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 0.2, "average_density": 1.777 } },
	{ "name": "PredictedCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 0.2, "average_density": 1.777 } },
	{ "name": "ClearCongestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 0.2 } },
	{ "name": "Congestion", "timestamp": 12151, "attributes": { "location": "0024a4dc0000343b", "problem_id": 3, "Certainty": 1, "average_density": 1.777 } },
	{ "name": "UpdateMeteringRateAction", "timestamp": 1424439193610, "attributes": { "density": 89 ,"location": "0024a4dc0000343b", "newMeteringRate": 50, "controlType": "auto", "lane": "offramp"} },
	{ "name": "UpdateMeteringRateAction", "timestamp": 1424439205378, "attributes": { "density": 76 ,"location": "0024a4dc0000343b", "newMeteringRate": 12, "controlType": "auto", "lane": "offramp"} },
	{ "name": "UpdateMeteringRateAction", "timestamp": 1424439214019, "attributes": { "density": 27 ,"location": "0024a4dc0000343b", "newMeteringRate": 39, "controlType": "auto", "lane": "onramp"} },
	{ "name": "UpdateMeteringRateAction", "timestamp": 1426341045368, "attributes": { "density": 89 ,"location": "0024a4dc0000343b", "newMeteringRate": 50, "controlType": "auto", "lane": "offramp"} },
	{ "name": "UpdateMeteringRateAction", "timestamp": 1426341055520, "attributes": { "density": 76 ,"location": "0024a4dc0000343b", "newMeteringRate": 12, "controlType": "auto", "lane": "offramp"} },
	{ "name": "UpdateMeteringRateAction", "timestamp": 1426341062831, "attributes": { "density": 27 ,"location": "0024a4dc0000343b", "newMeteringRate": 39, "controlType": "auto", "lane": "onramp"} }
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
		// sends a list of all ramps
		socket.emit('ramp-list', { rampLoc: rampLoc });
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
