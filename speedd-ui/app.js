var express = require("express");
var fs = require('fs');
var http = require('http');
var path = require('path');
var kafka = require('kafka-node');

/// setting up kafka consummer
var Consumer = kafka.Consumer,
    client = new kafka.Client('localhost:2181/'),
    consumer = new Consumer(
	client, 
	// payloads
        [//{ topic: 'speedd-out-events', partition: 0, offset: 0 },
         { topic: 'speedd-out-events', partition: 0, offset: 0 }],
	// options
	{fromOffset: false} // true = read messages from beginning
    );
    

//// Setting up Kafka Producer
var Producer = kafka.Producer;
var producer = new Producer(client);
var payloads = [
        { topic: 'speedd-out-events', messages: 'THIS IS THE NEW APP', partition: 0 }
    ];
producer.on('ready', function () {
    producer.send(payloads, function (err, data) {
        console.log(data);
    });
	producer.createTopics(['speedd-admin'], function (err, data) {
		console.log(err);
	});
});

///

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
});

var io = require('socket.io')(ser);

io.on('connection', function (socket) {
	socket.emit('news', { hello: 'world' });
	socket.on('my other event', function (data) {
		console.log(data);
	});
 
	socket.on('speedd-out-events', function (data) {
		console.log(data);
		var toSend = [{ topic: 'speedd-admin', messages: data, partition: 0 }];
		producer.send(toSend, function (err, data) {
			console.log(toSend);
		});
	});

});
////
 
consumer.on('error', function (err) {
	console.log("Kafka Error: Consumer - " + err);
});
consumer.on('message', function (message) {
	console.log(message.value);
	io.emit('speedd-out-events', message.value);
});
