app.controller('DriverComplianceModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.selection = dataService.eventSelection;
	
	
	var speedE = (dataService.polylineSelected.start.speed + dataService.polylineSelected.end.speed)/2;
	var speedW = speedE - 20;
	
	var distE = randomInt(1,10);
	var distW = randomInt(1,10);
	
	$scope.data = [{distance: distE, speed: speedE},{distance: distW, speed: speedW}];
	
	
	

	function randomInt(min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
	
	$scope.onClick = function(item) { // this is applied to the whole svg and we don't want that --- to be changed
		if (item === undefined);
		else
			console.log(item);
	};
	
	$scope.$on("changeSelection", function(){
		$scope.selection = dataService.eventSelection;
	})
	
	$scope.close = function () {
		$modalInstance.dismiss('cancel');
	};
	
}]);