app.controller('EventListController', ['$scope','$interval','$window','dataService', function($scope, $interval,$window,dataService){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list
	
	
	$scope.isCollapsed = false;
	
	$scope.eventList = [];
	$scope.formattedEventList = [];
	$scope.listSelection;
	
	$scope.$on("broadcastRawEventList", function(){
		$scope.eventList = clone(dataService.rawEventList);
//		console.log($scope.eventList);
		formatEventList($scope.eventList)
	});
	
	$scope.$on("broadcastMapEvent", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);
		formatEvent(event);
	});
	$scope.$on("broadcastRampEvent", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);

		formatEvent(event);
	});
	
	$scope.$on("broadcastUserEvent", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);
		formatEvent(event);
	});
    
	function formatEvent(dataToFormat){ // modifies the raw events in a format easy to read for the display in a table
		var event={};
		
		event.id = $scope.formattedEventList.length;
		// converts ms to date and time
		event.time = (dataToFormat.timestamp != undefined)? new Date(dataToFormat.timestamp).toString() : -1;
		event.name = (dataToFormat.name != undefined)? dataToFormat.name : -1;
		event.ramp_id = (dataToFormat.attributes.location != undefined)? dataService.rampLocationToId(dataToFormat.attributes.location) : -1;
		event.density = (dataToFormat.attributes.density != undefined)? dataToFormat.attributes.density : dataToFormat.attributes.average_density;
		event.problem_id = (dataToFormat.attributes.problem_id != undefined)? dataToFormat.attributes.problem_id : -1;
		
		$scope.formattedEventList.push(event);
	}
	function formatEventList(dataToFormat){
		for(var i=0; i<dataToFormat.length;i++)
		{
			formatEvent(dataToFormat[i]);
		}
	}
	
	$scope.gridOptions = { 
        data: 'formattedEventList',
		enablePinning: true, // pin columns
		selectedItems: $scope.listSelection, // enables selection of row
		multiSelect: false
		/*
        showGroupPanel: true,
        jqueryUIDraggable: true*/
    };
	
}]);