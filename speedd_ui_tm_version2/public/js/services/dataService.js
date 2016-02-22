app.factory('dataService', function ($rootScope,socket) { // this service broadcasts all data items received from server --- controllers need to listen for 'broadcastRamps'
  var data = {};
  
  data.cams = {'cam1':'img/traffic1.jpg', 'cam2': 'img/traffic2.jpg', 'cam3': 'img/traffic3.jpg', 'cam4': 'img/traffic4.jpg', 'cam5': 'img/traffic5.jpg', 'cam6': 'img/traffic6.jpg', 'cam7': 'img/traffic7.jpg', 'cam8': 'img/traffic8.jpg'};
  
  data.nodes = [{'id':'node1', 'location':'0024a4dc00003356', 'name':'1 - Meylan', 'camX': 680, 'camY': 160},
        {'id':'node2', 'location':'0024a4dc00003354', 'name':'2 - A41', 'camX': 660, 'camY': 180},
        {'id':'node3', 'location':'0024a4dc0000343c', 'name':'3 - Carronerie', 'camX': 665, 'camY': 225},
        {'id':'node4o', 'location':'0024a4dc0000343b', 'name':'4 - Domaine Uni', 'camX': 665, 'camY': 260},
        {'id':'node4', 'location':'0024a4dc0000343b', 'name':'4 - Domaine Uni', 'camX': 665, 'camY': 275},
        {'id':'node5o', 'location':'0024a4dc00003445', 'name':'5 - Gabriel Peri', 'camX': 655, 'camY': 295},
        {'id':'node5', 'location':'0024a4dc00003445', 'name':'5 - Gabriel Peri', 'camX': 650, 'camY': 305},
        {'id':'node6', 'location':'0024a4dc00001b67', 'name':'6 - Gabriel Peri', 'camX': 635, 'camY': 320},
        {'id':'node7', 'location':'0024a4dc00003357', 'name':'7 - SMH', 'camX': 595, 'camY': 350},
        {'id':'node8', 'location':'0024a4dc00000ddd', 'name':'8 - SMH Centre', 'camX': 570, 'camY': 390},
        {'id':'node9', 'location':'0024a4dc00003355', 'name':'9 - SMH Centre', 'camX': 550, 'camY': 415},
        {'id':'node10', 'location':'0024a4dc000021d1', 'name':'10 - Eybens', 'camX': 520, 'camY': 450},
        {'id':'node11', 'location':'0024a4dc0000343f', 'name':'11 - Eybens', 'camX': 500, 'camY': 470},
        {'id':'node12', 'location':'0024a4dc00001b5c', 'name':'12 - Echirolles', 'camX': 470, 'camY': 480},
        {'id':'node13', 'location':'0024a4dc000025eb', 'name':'13 - Echirolles', 'camX': 440, 'camY': 480},
        {'id':'node14', 'location':'0024a4dc000025ea', 'name':'14 - Etats Generaux', 'camX': 415, 'camY': 470},
        {'id':'node16', 'location':'0024a4dc000013c6', 'name':'16 - Etats Generaux', 'camX': 375, 'camY': 475},
        {'id':'node17', 'location':'0024a4dc00003444', 'name':'17 - Liberation', 'camX': 360, 'camY': 430},
        {'id':'node18', 'location':'0024a4dc000025ec', 'name':'18 - Liberation', 'camX': 350, 'camY': 435},
        {'id':'node19', 'location':'0024a4dc0000343e', 'name':'19 - Rondeau', 'camX': 330, 'camY': 440}
    ];
    
    data.segments = [{'id':'seg12', 'n1':'node1', 'n2':'node2', 'camX': 0, 'camY': 0},
        {'id':'seg23', 'n1':'node2', 'n2':'node3', 'camX': 0, 'camY': 0},
        {'id':'seg34o', 'n1':'node3', 'n2':'node4o', 'camX': 0, 'camY': 0},
        {'id':'seg4o4', 'n1':'node4o', 'n2':'node4', 'camX': 0, 'camY': 0},
        {'id':'seg45o', 'n1':'node4', 'n2':'node5o', 'camX': 0, 'camY': 0},
        {'id':'seg5o5', 'n1':'node5o', 'n2':'node5', 'camX': 0, 'camY': 0},
        {'id':'seg56', 'n1':'node5', 'n2':'node6', 'camX': 0, 'camY': 0},
        {'id':'seg67', 'n1':'node6', 'n2':'node7', 'camX': 0, 'camY': 0},
        {'id':'seg78', 'n1':'node7', 'n2':'node8', 'camX': 0, 'camY': 0},
        {'id':'seg89', 'n1':'node8', 'n2':'node9', 'camX': 0, 'camY': 0},
        {'id':'seg910', 'n1':'node9', 'n2':'node10', 'camX': 0, 'camY': 0},
        {'id':'seg1011', 'n1':'node10', 'n2':'node11', 'camX': 0, 'camY': 0},
        {'id':'seg1112', 'n1':'node11', 'n2':'node12', 'camX': 0, 'camY': 0},
        {'id':'seg1213', 'n1':'node12', 'n2':'node13', 'camX': 0, 'camY': 0},
        {'id':'seg1314', 'n1':'node13', 'n2':'node14', 'camX': 0, 'camY': 0},
        {'id':'seg1416', 'n1':'node14', 'n2':'node16', 'camX': 0, 'camY': 0},
        {'id':'seg1617', 'n1':'node16', 'n2':'node17', 'camX': 0, 'camY': 0},
        {'id':'seg1718', 'n1':'node17', 'n2':'node18', 'camX': 0, 'camY': 0},
        {'id':'seg1819', 'n1':'node18', 'n2':'node19', 'camX': 0, 'camY': 0}
    ];
  
  
  // converts location to node name and runs update function
    data.locationToNode = function (location){
        for(var i = 0; i < data.nodes.length; i++)
        {
            if (data.nodes[i].location == location && data.nodes[i].id.slice(-1)!= "o")
                return data.nodes[i].id;
        }
    }
    
    data.nodeToName = function (nodeId){
        for(var i = 0; i < data.nodes.length; i++)
        {
            if (data.nodes[i].id == nodeId)
                return data.nodes[i].name; 
        }
    }
    
    data.nodeToLocation = function (nodeId){
        for(var i = 0; i < data.nodes.length; i++)
        {
            if (data.nodes[i].id == nodeId)
                return data.nodes[i].location; 
        }
    }
    
    data.nodeToNumber = function (nodeId){
        var num = nodeId.slice(-2);
        if(num[0] == "e")
            num = nodeId.slice(-1);
            
        return num;
    }
    
    data.nameToNode = function (name){
        for(var i = 0; i < data.nodes.length; i++)
        {
            if (data.nodes[i].name == name && data.nodes[i].id.slice(-1)!= "o")
                return data.nodes[i];
        }
    }
    
    data.idToNode = function (nodeId){
        for(var i = 0; i < data.nodes.length; i++)
        {
            if (data.nodes[i].id == nodeId)
                return data.nodes[i]; 
        }
    }
    
    data.segmentsWithNode = function (nodeId){
        var segments = [];
        
        data.segments.forEach(function(elem){
            if (elem.n1 == nodeId || elem.n2 == nodeId)
                segments.push(elem.id);
        });
        
        return segments;
    }
    
    data.nodesOfSegment = function (segment){
        var nodes = [];
        
        data.segments.forEach(function(elem){
            if (elem.id == segment){
                nodes.push(elem.n1);
                nodes.push(elem.n2);
            }
               
        });
        
        return nodes;
    }
    
    
  
  data.rampList;
  data.mainRoadSensors;
  data.rampSelected;
  data.polylineSelected;
  data.rawEventList=[];
  data.eventSelection;;
  
  data.mapEventList=[];
  data.currentMapEvent;
  data.rampEventList=[];
  data.currentRampEvent;
  data.userEvent;
  
  data.mainRoadEventList=[];
  data.currentMainRoadEvent;
  
  data.fullSensorList = [];
 
  
   socket.on('ramp-list', function (socketData) {
			data.rampList=socketData.rampLoc;
			data.formatRamps();
//			console.log(data.rampList);
			data.mainRoadSensors = socketData.mainRoadSensors;
			data.formatRoadSensors();
			data.broadcastItem();
	});
	
	socket.on('event-list', function (socketData) {
			var events = JSON.parse(socketData.eventList);
			data.rawEventList=events;
//			console.log(data.rawEventList);
			data.broadcastRawEventList();
	});
	
	
	socket.on('speedd-out-events', function (socketData) {
			var event = JSON.parse(socketData);
			data.rawEventList.push(event);
			data.parseEvent(event);
//			console.log(event);
	});
	
	
	data.formatRamps = function(){
		for (var i = 0; i< data.rampList.length; i++)
		{
			data.rampList[i].id = i;
			data.rampList[i].rate = 100;
			data.rampList[i].density = 0;
			data.rampList[i].limits = {};
			data.rampList[i].limits = {lowerLimit: "Auto", upperLimit: "Auto"};
			
			data.fullSensorList.push(data.rampList[i]);
		}
	};
	data.formatRoadSensors = function(){
		for (var i = 0; i< data.mainRoadSensors.length; i++)
		{
			data.mainRoadSensors[i].id = i;
			data.mainRoadSensors[i].density = data.randomInt(1,100);
			data.mainRoadSensors[i].flow = data.randomInt(1,100);
			data.mainRoadSensors[i].speed = data.randomInt(1,150);
			
			data.fullSensorList.push(data.mainRoadSensors[i]);
		}
	};
	
	data.parseEvent = function(event){
		if (event.name == "Congestion" || event.name == "PredictedCongestion" || event.name == "ClearCongestion")
		{
			data.mapEventList.push(event);
			data.currentMapEvent = event;
			data.broadcastMapEvent();
		}
		else if (event.name == "SetTrafficLightPhases" || event.name == "ClearRampOverflow" || event.name == "PredictedRampOverflow")
		{
			data.rampEventList.push(event);
			data.currentRampEvent = event;
			data.broadcastRampEvent();
		}
		// updates main road density colour
		else if (event.name == "AverageDensityAndSpeedPerLocation" || event.name =="AverageDensityAndSpeedPerLocationOverInterval" || event.name == "AverageOffRampValuesOverInterval" || event.name == "AverageOnRampValuesOverInterval")
		{
			data.mainRoadEventList.push(event);
			data.currentMainRoadEvent = event;
			data.broadcastMainRoadEvent();
		}
	};
	
	data.rampLocationToId = function(location,lane){// TRANSFORMS sensor location to id
		
		var rampId;
		var once = 0;
		for (var i = 0; i < data.rampList.length; i++)
		{
			if(data.rampList[i].location == location && data.rampList[i].lane == lane)
			{
				rampId = data.rampList[i].id;
			}
			else if(data.rampList[i].location == location && once == 0)
			{
				once ++;
				rampId = data.rampList[i].id;
			}
		}
		return rampId;
	}
	
	data.rampIdToLocation = function(id){
		
		var rampLocation = {};
		
		rampLocation.location = data.rampList[id].location;
		rampLocation.lane = data.rampList[id].lane;
		
		return rampLocation;
	}
  
  ///////////////////////////////////////////////
  data.changeCamFocus = function (node){
      
      // remove highlights of bar containers
      for (var i=0; i < data.nodes.length ; i++)
      { 
            var cc = "#cc"+data.nodeToNumber(data.nodes[i].id);
            var pc = "#pc"+data.nodeToNumber(data.nodes[i].id);
            d3.select(circularMap).select("svg").select(cc).attr("class","st7");
            d3.select(circularMap).select("svg").select(pc).attr("class","st7");
      }
      
      // highlight bar containers
      var cc = "#cc"+data.nodeToNumber(node.id);
      var pc = "#pc"+data.nodeToNumber(node.id);
      d3.select(circularMap).select("svg").select(cc).attr("class","st6");
      d3.select(circularMap).select("svg").select(pc).attr("class","st6");
      
      // move camera icon
      d3.select(circularMap).select("svg").select("#cam")
            .attr('x', node.camX)
            .attr('y', node.camY);
      
      // change image in cam feed window
//      var camImage = "img/traffic"+data.randomInt(1,19)+".jpg";
      var camImage = "img/traffic"+data.nodeToNumber(node.id)+".jpg";

      
      // puts image in the big cam window
      d3.select("#cam8").select("img").attr("src",camImage).attr("width","100%");
      data.cams.cam8 = camImage;
      
  }
  
  
  data.changeRampSelected = function(rampId){	//changes ramp selected based on rampList click (RampListController)
	data.rampSelected = rampId;
  };
  
  data.changeSelection = function(obj){	//changes ramp selected based on rampList click (RampListController)
	data.eventSelection = obj;
	data.broadcastSelectionChanged();
  };
  
  data.changeThresholdsRampSelected = function(lower,upper){	// changes rate thresholds of selected ramp ---- function called by "ChallengeModalController"
	data.rampList[data.rampSelected].limits.lowerLimit = (lower != undefined)? lower:"Auto";
	data.rampList[data.rampSelected].limits.upperLimit = (upper != undefined)? upper:"Auto";
	
	/////////////////////////////// SEND SOCKET EVENTS TO SERVER
	////////////////////////////////////////////////////////////
	var rampLocation = data.rampIdToLocation(data.rampSelected);
	// format the message
	var messageToSend = {
		"name": "setMeteringRateLimits",
		"timestamp": new Date().getTime(),	
		"attributes":
		{
			"location": rampLocation.location,
			"upperLimit": (upper != undefined)? upper:-1,
			"lowerLimit": (lower != undefined)? lower:-1
		}
	}
	// send the message
	socket.emit('speedd-out-events', JSON.stringify(messageToSend));
	// pushes the event to rawEventList
	data.rawEventList.push(messageToSend);
	// tells controllers(only EventListController for now) that user event has occurred
	data.broadcastUserEvent();
  };
/*  
  data.broadcastRampSelectedChanged = function(){
	$rootScope.$broadcast('broadcastRampSelectedChanged');
  };
 */ 
   data.broadcastSelectionChanged = function(){
	$rootScope.$broadcast('broadcastSelectionChanged');
  };
  
  data.broadcastItem = function(){
	$rootScope.$broadcast('broadcastRamps');
  };
    
  data.goToMapLocation = function(){
	$rootScope.$broadcast('broadcastShowRampOnMap');
  };
  
  data.broadcastMapEvent = function(){
	$rootScope.$broadcast('broadcastMapEvent');
  };
  data.broadcastRampEvent = function(){
	$rootScope.$broadcast('broadcastRampEvent');
  };
   data.broadcastMainRoadEvent = function(){
	$rootScope.$broadcast('broadcastMainRoadEvent');
  };
  
  data.broadcastRawEventList = function(){
	$rootScope.$broadcast('broadcastRawEventList');
  };
  
  data.broadcastUserEvent = function(){
	$rootScope.$broadcast('broadcastUserEvent');
  };
  
  
  	data.randomInt = function (min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
  
  data.polylineSelectionChanged = function (poly){
	  data.polylineSelected = poly;
	  console.log(poly);
  }
  
  
  return data;
});