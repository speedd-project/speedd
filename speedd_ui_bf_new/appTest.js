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

//////////////FOR ZURICH
var readline = require('readline');

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

var testMessages = [
	{ "name": "Transaction","timestamp": 1409901066030,"attributes": {"card_pan": "1234-1234","terminal_id": "123456578","cvv_validation": 10,"amount_eur": "120.5","acquirer_country": 972,"is_cnp": 1}},
	{ "name": "Transaction","timestamp": 1409901066030,"attributes": {"card_pan": "1234-1234","terminal_id": "123456578","cvv_validation": 10,"amount_eur": "120.5","acquirer_country": 972,"is_cnp": 1}},
	{"name": "IncreasingAmounts","timestamp": 1409901066030,"attributes": {"Certainty": "1","card_pan": "1235-1234","terminal_id": 12345567,"TrendCount": 1.2, "acquirer_country": 972}},
	{"name": "FraudAtATM","timestamp": 1409901066030,"attributes": {"Certainty": "0.8","terminal_id": 12345678,"acquirer_country": 972}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 49,"average_transaction_amount_eur": "20","transaction_volume": 15}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 46,"average_transaction_amount_eur": "10","transaction_volume": 10}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 1,"average_transaction_amount_eur": "20","transaction_volume": 2}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 886,"average_transaction_amount_eur": "5","transaction_volume": 1}}
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
  fs.readFile('./public/index.html', function(err, data) {
    res.end(data);
  });

});

var ser = http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
  
//  setKafka();
  setSocket();
//  setConsumerEvents();
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
		
//		socket.emit('news', { hello: 'world' });
		
		// sends a list of already identified events
		socket.emit('event-list',{ eventList: JSON.stringify(eventList)});
		
		socket.on('my other event', function (data) {
			console.log(data);
		});
		socket.on('speedd-fraud-admin', function (data) {
			console.log(data);

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