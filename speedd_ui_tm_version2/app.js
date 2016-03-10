var express = require("express");
var fs = require('fs');
var Converter=require("csvtojson").core.Converter;
var http = require('http');
var path = require('path');
var kafka = require('kafka-node');
var argv = require('minimist')(process.argv.slice(2));
var io;
var Consumer, client, consumer, Producer, producer;

var zk = argv.zk? argv.zk : 'localhost:2181';
var uiport = argv.ui? argv.ui : 3000;

console.log("\nzookeeper url is set to: "+zk);
console.log("ui port is set to: "+uiport+"\n\n");


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


var app = express();
app.set('port', uiport);
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());



// Serve up our static resources
app.get('/', function(req, res) {
  fs.readFile('./public/index.html', function(err, data) {
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
		socket.emit('news', { hello: 'world' });
		
		console.log("Connection Established");
		// sends a list of all ramps
		socket.emit('ramp-list', { rampLoc: rampLoc });
		// sends a list of already identified events
		socket.emit('event-list',{ eventList: JSON.stringify(eventList)});

		setKafka();	
	});
}

function setKafka(){
	/// setting up kafka consummer
	console.log("Setting up Kafka clients");
	
	Consumer = kafka.Consumer;
	client = new kafka.Client(zk);
	
	offset = new kafka.Offset(client);
	
	offset.fetch([
        { topic: 'speedd-traffic-actions', partition: 0, time: Date.now(), maxNum: 1 },
		{ topic: 'speedd-traffic-out-events', partition: 0, time: Date.now(), maxNum: 1 }
    ], function (err, data) {
		consumer = new Consumer(
			client, 
			// payloads
				[{ topic: 'speedd-traffic-actions'},
				 { topic: 'speedd-traffic-out-events'}
				 ],
			// options
			{
				groupId: 'kafka-node-group',//consumer group id, default `kafka-node-group` 
				// Auto commit config 
				autoCommit: true,
				autoCommitIntervalMs: 1000,
				// The max wait time is the maximum amount of time in milliseconds to block waiting if insufficient data is available at the time the request is issued, default 100ms 
				fetchMaxWaitMs: 100,
				// This is the minimum number of bytes of messages that must be available to give a response, default 1 byte 
				fetchMinBytes: 1,
				// The maximum bytes to include in the message set for this partition. This helps bound the size of the response. 
				fetchMaxBytes: 1024 * 10,
				// If set true, consumer will fetch message from the given offset in the payloads 
				fromOffset: true,
				// If set to 'buffer', values will be returned as raw buffer objects. 
				encoding: 'utf8'
			}
		);
		
		setConsumerEvents();	

	});

	//consumer.setOffset('speedd-traffic-out-events', 0, 0);
	//consumer.setOffset('speedd-traffic-actions', 0, 0);
	
}

function setConsumerEvents(){

	console.log("Setting up Consumer on-message event");
	
	consumer.on('error', function (err) {
		console.log("Kafka Error: Consumer: " + err);
	});
	consumer.on('offsetOutOfRange', function (err) {
		console.log("Offset out of range: " + JSON.stringify(err));
	});
	consumer.on('message', function (message) {
		console.log("Got message:" + message.value);
		io.emit('speedd-out-events', message.value);
		
		// checks if event is one that should be displayed in the ui
		var ev = JSON.parse(message.value);
//		if (ev.name == "PredictedCongestion" || ev.name == "ClearCongestion" || ev.name == "Congestion" || ev.name == "UpdateMeteringRateAction")
			eventList.push(JSON.parse(message.value));
	});
}
