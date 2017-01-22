var express = require("express");
var fs = require('fs');
var Converter=require("csvtojson").core.Converter;
var http = require('http');
var path = require('path');
var kafka = require('kafka-node');
var io;
var Consumer, client, consumer, Producer, producer;
/////////////////////////
var analysts = [];
var analystAction;

analysts.push = function (){
    
    // emits an analyst array to all connections
    setTimeout(function(){
        io.emit("analyst", analysts);
    },100);
    
    
    return Array.prototype.push.apply(this,arguments);
}

function changeFlag(data){
    // emits an analyst array to all connections
    setTimeout(function(){
        io.emit("analystActions", data);
        console.log(data);
    },100);
    
}

////////////////////////
var outputFile;

var eventList=[];
var rampLoc;

//////////////FOR ZURICH
var readline = require('readline');

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

var testMessages = [
	{
        "timestamp": 1457011687602,
        "name": "SuddenCardUseNearExpirationDate",
        "attributes": {
            "is_cnp": 0,
            "timestamps": [
            2224699200000,
            2224699203000,
            2224699206000
            ],
            "transaction_ids": [
            "9d47b44380024e82aedcf359f8be8dfb",
            "8727070543664142bc7d2ba980dc1f19",
            "b1dedbeb44ad431f995076153c7bdcc8"
            ],
            "EventId": "8d778bac-a7be-4435-9d74-eb9fca610672",
            "TransactionsCount": 3,
            "Chronon": null,
            "DetectionTime": 1457011687602,
            "Name": "SuddenCardUseNearExpirationDate",
            "Certainty": 0.5998883688639982,
            "Cost": 100,
            "EventSource": "",
            "OccurrenceTime": 2224699206000,
            "Annotation": "",
            "Duration": 0,
            "card_country": 81,
            "ExpirationTime": null,
            "acquirer_country": [
            39,
            39,
            39
            ],
            "card_pan": "8de8552de9b94c1da2f798fcefe1ac16"
        }
    },
    {
        "timestamp": 1457011685599,
        "name": "TransactionsInFarAwayPlaces",
        "attributes": {
            "timestamps": [
            "2224699201000",
            "2224699204000"
            ],
            "transaction_ids": [
            "755efd3a1d9049158cb4340d9ca97bb7",
            "4017b70b1d0f4b36a77078ed4c620ed9"
            ],
            "EventId": "f23c53fa-85ba-4815-82cd-852c61b35809",
            "Chronon": null,
            "DetectionTime": 1457011685599,
            "Name": "TransactionsInFarAwayPlaces",
            "Certainty": 1,
            "Cost": 50,
            "EventSource": "",
            "OccurrenceTime": 2224699204000,
            "Annotation": "",
            "Duration": 0,
            "card_country": 81,
            "ExpirationTime": null,
            "acquirer_country": [
            "81",
            "39"
            ],
            "card_pan": "926653643724413fbc28b896f799985d"
        }
    },
    {"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 49,"average_transaction_amount_eur": "20","transaction_volume": 15}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 46,"average_transaction_amount_eur": "10","transaction_volume": 10}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 1,"average_transaction_amount_eur": "20","transaction_volume": 2}},
	{"name": "TransactionStats","timestamp": 1409901066030,"attributes": {"country": 886,"average_transaction_amount_eur": "5","transaction_volume": 1}},
    {
        "name": "IncreasingAmounts",
        "timestamp": 1409901066030,
        "attributes": {				
            "terminal_id": 12345567,
            "TrendCount": 1.2,
            "is_cnp": 0,
            "timestamps": [
            2224699200000,
            2224699203000,
            2224699206000
            ],
            "amounts": [15, 50, 150],
            "transaction_ids": [
            "9d47b44380024e82aedcf359f8be8dfb",
            "8727070543664142bc7d2ba980dc1f19",
            "b1dedbeb44ad431f995076153c7bdcc8"
            ],
            "EventId": "8d778bac-a7be-4435-9d74-eb9fca610672",
            "Chronon": null,
            "DetectionTime": 1457011687602,
            "Name": "IncreasingAmounts",
            "Certainty": 0.5998883688639982,
            "Cost": 0,
            "EventSource": "",
            "OccurrenceTime": 2224699206000,
            "Annotation": "",
            "Duration": 0,
            "card_country": 81,
            "ExpirationTime": null,
            "acquirer_country": [
            "81",
            "81",
            "81"
            ],
            "card_pan": "8de8552de9b94c1da2f798fcefe1ac16"
        }
    }
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
app.set('port', 3009);
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());

// Serve up our static resources
app.get('/', function(req, res) {
  fs.readFile('./public/index.html', function(err, data) {
    res.end(data);
  });

});

app.get('/analyst', function(req, res) {
    res.sendfile('./public/index_analyst.html');

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
		
		socket.emit("analyst", analysts);
		
		console.log("Connection Established");
		
		// sends a list of already identified events
		socket.emit('event-list',{ eventList: JSON.stringify(eventList)});
		
		socket.on('my other event', function (data) {
			console.log(data);
		});
        
        socket.on('analyst', function (data) {
			console.log(data);
            analysts.push(data);
      //      socket.emit('analyst',data);
		});
        
        socket.on('analystAction', function (data) {
			//console.log(data);
           
            changeFlag(data);
		});
        
		socket.on('speedd-fraud-admin', function (data) {
			console.log(data);

		});
		
	});
}
