app.controller('LiveGraphController', ['$scope','$interval','$window','dataService','$modalInstance','rampDataService','socket', function($scope, $interval,$window,dataService,$modalInstance,rampDataService,socket){
    
	$scope.isCollapsed = false;
	
	$scope.rampSelected = dataService.rampSelected;
	
	$scope.ok = function () {
		$modalInstance.close("acknowledged");
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	$scope.dataToPlot = rampDataService.rampList[$scope.rampSelected];
	console.log($scope.dataToPlot);
	
	socket.on('speedd-out-events', function (socketData) {
		var event = JSON.parse(socketData);
//		$scope.rawEventList.push(event);
		// get ramp id
		var rampId = dataService.rampLocationToId(event.attributes.location);
		
		if(event.name=="UpdateMeteringRateAction" && rampId == $scope.rampSelected)
		{
			getRampInfoFromEvent(event);
		}
		
	});
	
	function getRampInfoFromEvent(event){
		
		// update current density and rate for that ramp
		$scope.dataToPlot.rate.push(event.attributes.newMeteringRate);
		$scope.dataToPlot.density.push(event.attributes.density);
		$scope.dataToPlot.time.push(event.timestamp);
	}
	
}]);