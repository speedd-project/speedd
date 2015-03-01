var express = require("express");
var fs = require('fs');
var Converter=require("csvtojson").core.Converter;
var http = require('http');
var path = require('path');
var kafka = require('kafka-node');
var io;
var Consumer, client, consumer, Producer, producer;

var outputFile;

var eventList=[];
var rampLoc;
/*
var csvFileName="data/sensorpos.csv";
var fileStream=fs.createReadStream(csvFileName);
//new converter instance 
var csvConverter=new Converter({constructResult:true});
 
//end_parsed will be emitted once parsing finished 
csvConverter.on("end_parsed",function(jsonObj){
   rampLoc=jsonObj;
});
fileStream.pipe(csvConverter);
*/

var app = express();
app.set('port', 3000);
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
  
  setKafka();
  setSocket();
  setConsumerEvents();
});

function setSocket(){
	io = require('socket.io')(ser);
	console.log("Setting up Client-Server communication");
	
	io.on('connection', function (socket) {
		socket.emit('news', { hello: 'world' });
		
		console.log("Connection Established");
		
		
		// sends a list of already identified events
		socket.emit('event-list',{ eventList: JSON.stringify(eventList)});
		
		socket.on('my other event', function (data) {
			console.log(data);
		});
		socket.on('speedd-out-events', function (data) {
			console.log(data);
			var toSend = [{ topic: 'speedd-fraud-admin', messages: data, partition: 0 }];
			producer.send(toSend, function (err, data) {
				console.log(toSend);
			});
		});
	});
}

function setKafka(){
	/// setting up kafka consummer
	console.log("Setting up Kafka clients");
	
	Consumer = kafka.Consumer;
	client = new kafka.Client('localhost:2181/');
	consumer = new Consumer(
		client, 
		// payloads
			[{ topic: 'speedd-fraud-actions', partition: 0, offset: 0 },
			 { topic: 'speedd-fraud-out-events', partition: 0, offset: 0 },
			 { topic: 'speedd-fraud-in-events', partition: 0, offset: 0 }
			 ],
		// options
		{fromOffset: true} // true = read messages from beginning
	);

	//// Setting up Kafka Producer

	Producer = kafka.Producer;
	producer = new Producer(client);
	payloads = [
			{ topic: 'speedd-fraud-out-events', messages: 'THIS IS THE NEW APP', partition: 0 }
		];
	producer.on('ready', function () {
		producer.send(payloads, function (err, data) {
			console.log(data);
		});
		producer.createTopics(['speedd-fraud-admin'], function (err, data) {
			console.log(err);
		});
	});
}

function setConsumerEvents(){

	console.log("Setting up Consumer on-message event");
	
	consumer.on('ready', function () {
		console.log("consumer listening");
	});
	consumer.on('error', function (err) {
		console.log("Kafka Error: Consumer - " + err);
	});
	consumer.on('message', function (message) {
		console.log(message.value);
		io.emit('speedd-out-events', message.value);
		eventList.push(JSON.parse(message.value));
	});
}