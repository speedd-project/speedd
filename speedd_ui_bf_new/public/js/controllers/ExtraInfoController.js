app.controller('ExtraInfoController', ['$scope','$interval','$window','dataService', function($scope, $interval,$window,dataService){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list
	
	
	$scope.isCollapsed = false;
	
	$scope.selection=dataService.selection;
	
	$scope.$on("broadcastSelectionChanged", function(){
		$scope.selection = dataService.selection;
	});
	
	
}]);