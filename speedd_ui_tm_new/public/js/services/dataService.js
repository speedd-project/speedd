app.factory('dataService', function ($rootScope,socket) { // this service broadcasts all data items received from server --- controllers need to listen for 'broadcastRamps'
  var data = {};
  
  data.rampList;
  data.rampSelected;
  data.rawEventList=[];
  data.eventSelection;;
  
  data.mapEventList=[];
  data.currentMapEvent;
  data.rampEventList=[];
  data.currentRampEvent;
  data.userEvent;
  
   socket.on('ramp-list', function (socketData) {
			data.rampList=socketData.rampLoc;
			data.formatRamps();
//			console.log(data.rampList);
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
		}
	};
	
	data.parseEvent = function(event){
		if (event.name == "Congestion" || event.name == "PredictedCongestion" || event.name == "ClearCongestion")
		{
			data.mapEventList.push(event);
			data.currentMapEvent = event;
			data.broadcastMapEvent();
		}
		else if (event.name == "UpdateMeteringRateAction")
		{
			data.rampEventList.push(event);
			data.currentRampEvent = event;
			data.broadcastRampEvent();
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
  
  data.broadcastRawEventList = function(){
	$rootScope.$broadcast('broadcastRawEventList');
  };
  
  data.broadcastUserEvent = function(){
	$rootScope.$broadcast('broadcastUserEvent');
  };
  
  return data;
});