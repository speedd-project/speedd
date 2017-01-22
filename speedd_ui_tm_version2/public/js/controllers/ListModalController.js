app.controller('ListModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.selection = dataService.eventSelection;
    // checks if there is an incident with the problem_id that has not been removed
    $scope.incidentPresent = d3.select(circularMap).select('#incident'+$scope.selection.problem_id)[0][0]!= null;
    
	$scope.$on("changeSelection", function(){
		$scope.selection = dataService.eventSelection;
	})
	
	$scope.close = function () {
		$modalInstance.dismiss('cancel');
	};
    // clears incident alert on btn click
	$scope.clearAlert = function () {
		d3.select(circularMap).select("#incident"+$scope.selection.problem_id).remove();
		$modalInstance.dismiss('incident removed');
	};
}]);