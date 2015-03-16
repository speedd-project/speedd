app.controller('ListModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.selection = dataService.eventSelection;
	
	$scope.$on("changeSelection", function(){
		$scope.selection = dataService.eventSelection;
	})
	
	$scope.close = function () {
		$modalInstance.dismiss('cancel');
	};
	
}]);