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

consumer.on('error', function (err) {
    console.log("Kafka Error: Consumer - " + err);
});
consumer.on('message', function (message) {
    console.log(message.value);

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

app.get('/events', function(req, res) {
  // let request last as long as possible
  req.socket.setTimeout(Infinity);
 
  // When we receive a message from the kafka connection
  consumer.on('message', function (message) {
//    console.log(message.value);
    res.write("data: " + message.value + '\n\n'); // Note the extra newline
  });
    
  //send headers for event-stream connection
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  res.write('\n');

});

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});
