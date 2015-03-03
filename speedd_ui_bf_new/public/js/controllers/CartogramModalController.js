app.controller('CartogramModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.countrySelection = dataService.countrySelection;
	$scope.countryDetails = dataService.map_data.get(dataService.countrySelection);
	
//	console.log($scope.countryDetails.financial);
	
	$scope.$on("broadcastCountrySelectionChanged", function(){
		$scope.countrySelection	= dataService.countrySelection;
	})
	
	$scope.ok = function () {
		$modalInstance.close("acknowledged");
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	
	
}]);