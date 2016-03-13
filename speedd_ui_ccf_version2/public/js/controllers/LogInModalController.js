app.controller('LogInModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.selection = dataService.selection;
    $scope.analyst="";

	
	$scope.$on("changeSelection", function(){
		$scope.selection = dataService.selection;
	})
	
	$scope.logIn = function () {
//		$scope.selection.confirmed = true;
        console.log($scope.analyst);
        
		$modalInstance.close(/*"Logged in as "+*/$scope.analyst);
             
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	
	
}]);