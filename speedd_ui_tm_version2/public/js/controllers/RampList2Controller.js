app.controller('RampList2Controller', ['$scope','$interval', function($scope, $interval){
    
	$scope.isCollapsed = false;
	
	$scope.rampData = [{id:0, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:1, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:2, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:3, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:4, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:5, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:6, rate:randomInt(1,100), density:randomInt(1,100)}
			,{id:7, rate:randomInt(1,100), density:randomInt(1,100)}];
			
	function randomInt(min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
	
}]);