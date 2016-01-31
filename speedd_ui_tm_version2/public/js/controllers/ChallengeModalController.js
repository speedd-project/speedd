app.controller('ChallengeModalController', ['$scope','$interval','$window','dataService','$modalInstance', function($scope, $interval,$window,dataService,$modalInstance){
    
	$scope.isCollapsed = false;
	
	$scope.rampSelected = dataService.rampSelected;
	
	$scope.maxRate;
	$scope.minRate;
	$scope.comments;

	$scope.convertEventToDataPoint = function(event){
		var dataPoint = {};
		
		dataPoint.time = event.timestamp;
		dataPoint.rate = event.attributes.newMeteringRate;
		dataPoint.density = event.attributes.density;
		dataPoint.ramp = dataService.rampLocationToId(event.attributes.location,event.attributes.lane);
		
		$scope.dataPoints.push(dataPoint);
		
		console.log($scope.dataPoints);
	}
	
	$scope.ok = function () {
		$modalInstance.close("acknowledged");
		console.log($scope.maxRate,$scope.minRate,$scope.comments);
		dataService.changeThresholdsRampSelected($scope.minRate,$scope.maxRate);
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};

	
}]);