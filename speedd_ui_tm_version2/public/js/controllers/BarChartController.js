app.controller('BarChartController', ['$scope','$interval', function($scope, $interval){
    
	var sc = this;
	$scope.myData = [10,20,95,40,60, 80, 20, 50];
	$scope.myData2 = [5,15,25,35,45, 55, 65, 75];
	$scope.isCollapsed = false;
}]);