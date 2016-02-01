app.controller('RampListController', ['$scope','$interval','$window','dataService', function($scope, $interval,$window,dataService){
    
	$scope.isCollapsed = false;
	
	var sc = this;
	angular.element($window).on('resize', function(){ $scope.$apply() }); // IF WINDOW IS RESIZED reapplies scope

	// applies all changes to ramp density and rate based on previous events
	$scope.$on("broadcastRawEventList", function(){
		var eventList = dataService.rawEventList;
//		console.log($scope.eventList);
		for (var i = 0; i<eventList.length;i++)
			// checkes if event is regarding ramp rates
			if(eventList[i].name == "UpdateMeteringRateAction" || eventList[i].name == "setMeteringRateLimits")
				getRampInfoFromEvent(eventList[i]);
	});
	
	$scope.$on('broadcastRamps', function(){ // listens for updated ramp list
//		console.log(dataService.rampList);
		$scope.dataRamps = dataService.rampList;
	});
	
	// listens for new ramp events
	$scope.$on('broadcastRampEvent', function(){
		var event = dataService.currentRampEvent;
//		console.log(event);
		getRampInfoFromEvent(event);
	});
	
	function getRampInfoFromEvent(event){
		// get ramp id
		var rampId = dataService.rampLocationToId(event.attributes.location/*,event.attributes.lane*/);
		
		
		// limit set or current rate?
		if (event.name == "UpdateMeteringRateAction"){
			// update current density and rate for that ramp
			$scope.dataRamps[rampId].rate = event.attributes.newMeteringRate;
			$scope.dataRamps[rampId].density = event.attributes.density;
			
		}
		else{ // executes for setMeteringRateLimits
			$scope.dataRamps[rampId].limits.lowerLimit = (event.attributes.lowerLimit == -1)?	"Auto":event.attributes.lowerLimit;
			$scope.dataRamps[rampId].limits.upperLimit = (event.attributes.upperLimit == -1)?	"Auto":event.attributes.upperLimit;
//			console.log($scope.dataRamps[rampId]);
		}
	}
	
	$scope.dataRamps=[];
	
	function randomInt(min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
	
	$scope.rampSelected;
	$scope.rampSelectedLimits;
	
	$scope.onClick = function(item) { // this is applied to the whole svg and we don't want that --- to be changed
		if (item === undefined);
		else{
			$scope.rampSelected = item.item.id;
			$scope.rampSelectedLimits = $scope.dataRamps[$scope.rampSelected].limits;
			console.log($scope.dataRamps[$scope.rampSelected]);
			// send the rampSelected to dataService so other controllers can access it
			dataService.changeRampSelected($scope.rampSelected);				
		}
	};
	
	$scope.goToMapLocation = function (){
		dataService.goToMapLocation();
	};
	
	
}]);