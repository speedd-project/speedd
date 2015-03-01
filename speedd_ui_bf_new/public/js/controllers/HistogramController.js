app.controller('HistogramController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	
	
	$scope.ok = function () {
		$modalInstance.close("acknowledged");
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	$scope.data = [1,50,26,73,15,34,26,20,45,96,45,32,15,79,78,45,12,65,1,50,26,73,15,34,26,20,45,96,45,32,15,79,78,45,12,65];

	
}]);