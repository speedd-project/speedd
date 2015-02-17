app.controller('DriverComplianceController', ['$scope','$interval','$window', function($scope, $interval,$window){
    
	$scope.isCollapsed = false;
	
	var sc = this;
	angular.element($window).on('resize', function(){ $scope.$apply() }); // IF WINDOW IS RESIZED reapplies scope
	
//	$scope.distance = [10,7,3,5]; // not used
//	$scope.speed = 100;	// not used
					// northbound             southbound
	$scope.data = [{distance:10, speed:150},{distance:5, speed:75}];

	function randomInt(min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
	
	$scope.onClick = function(item) { // this is applied to the whole svg and we don't want that --- to be changed
		if (item === undefined);
		else
			console.log(item);
	};
	
}]);