app.controller('ListModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.selection = dataService.selection;
	
	$scope.$on("changeSelection", function(){
		$scope.selection = dataService.selection;
	})
	
	$scope.fraud = function () {
		$scope.selection.confirmed = true;
		$modalInstance.close("fraud");
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	
	
}]);