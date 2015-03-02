app.controller('CountryGraphController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	
	$scope.countrySelection = dataService.countrySelection;
	
	$scope.ok = function () {
		$modalInstance.close("acknowledged");
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	// serves country financial data to directive
	$scope.dataToPlot = dataService.map_data[dataService.countrySelection].financial;
	
	
}]);