app.factory('rampDataService', function ($rootScope,socket) { // this service broadcasts all data items received from server --- controllers need to listen for 'broadcastRamps'
  var data = {};
  
  data.rampList;
  data.rampSelected;
  data.rawEventList=[];
  
   socket.on('ramp-list', function (socketData) {
		data.rampList=clone(socketData.rampLoc);
		data.formatRamps();
		
	});
	
	socket.on('event-list', function (socketData) {
		var events = JSON.parse(socketData.eventList);
		data.rawEventList=events;
//			console.log(data.rawEventList);

		for (var i = 0; i < data.rawEventList.length;i++)
		{
			if(data.rawEventList[i].name=="UpdateMeteringRateAction")
				getRampInfoFromEvent(data.rawEventList[i]);
		}
	});
	
	
	socket.on('speedd-out-events', function (socketData) {
		var event = JSON.parse(socketData);
		data.rawEventList.push(event);
		
		if(event.name=="UpdateMeteringRateAction")
		{
			getRampInfoFromEvent(event);
			console.log(data.rampList);
		}
		
	});
	
	function getRampInfoFromEvent(event){
		// get ramp id
		var rampId = data.rampLocationToId(event.attributes.location);//,event.attributes.lane);
		// update current density and rate for that ramp
//		console.log($scope.dataRamps[rampId].rate);
		data.rampList[rampId].rate.push(event.attributes.newMeteringRate);
		data.rampList[rampId].density.push(event.attributes.density);
		data.rampList[rampId].time.push(event.timestamp);
	}
	
	data.formatRamps = function(){
		for (var i = 0; i< data.rampList.length; i++)
		{
			data.rampList[i].id = i;
			data.rampList[i].rate = [];
			data.rampList[i].density = [];
			data.rampList[i].time = [];
			data.rampList[i].limits = {};
			data.rampList[i].limits = {lowerLimit: "Auto", upperLimit: "Auto"};
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
  
 

  
  return data;
});	
