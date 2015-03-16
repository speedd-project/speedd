app.controller('EventListController', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list
	
	
	$scope.isCollapsed = false;
	
	$scope.eventList = [];
	$scope.formattedEventList = [];
	$scope.listSelection=[];
	
	$scope.$on("broadcastRawEventList", function(){
		$scope.eventList = clone(dataService.rawEventList);
//		console.log($scope.eventList);
		formatEventList($scope.eventList);
		
//		console.log($scope.formattedEventList);
	});
	
	$scope.$on("broadcastFraudAtATM", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);
		formatEvent(event);
		console.log(event);
	});
	$scope.$on("broadcastIncreasingAmounts", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);

		formatEvent(event);
		
		console.log(event);
	});
/*	
	$scope.$on("TransactionStats", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);
		formatEvent(event);
	});
*/    
	function formatEvent(dataToFormat){ // modifies the raw events in a format easy to read for the display in a table
		var event={};
		
		event.id = $scope.formattedEventList.length;
		// converts ms to date and time
		event.time = (dataToFormat.timestamp != undefined)? new Date(dataToFormat.timestamp).toString() : -1;
		event.name = (dataToFormat.name != undefined)? dataToFormat.name : -1;
		event.country = (dataToFormat.attributes.acquirer_country != undefined)? dataService.map_data2.get(dataToFormat.attributes.acquirer_country).name.common : -1;
		event.certainty = (dataToFormat.attributes.Certainty != undefined)? dataToFormat.attributes.Certainty : -1;
		event.reason = (dataToFormat.attributes.reason != undefined)? dataToFormat.attributes.reason : "";
		event.confirmed = "false"
		
		$scope.formattedEventList.push(event);
	}
	
	function formatEventList(dataToFormat){
		for(var i=0; i<dataToFormat.length;i++)
		{
			if(dataToFormat[i].name != "Transaction" && dataToFormat[i].name != "TransactionStats")
				formatEvent(dataToFormat[i]);
		}
	}
	
	// on row click gets data
	$scope.onClickRow = function(rowItem) {
		var item = rowItem.entity;
 //       console.log(item);
		dataService.changeSelection(item);
    };
	
	$scope.gridOptions = { 
        data: 'formattedEventList',
		enablePinning: true, // pin columns
		selectedItems: $scope.listSelection, // enables selection of row
		multiSelect: false,
		enableColumnResize: true,
		// adds ng-click event ng-click="onClickRow(row)"
		rowTemplate: '<div ng-click="onClickRow(row)" style="height: 100%" ng-class="{ selected: row.selected, green: row.getProperty(\'confirmed\') == 0}"><div ng-style="{ \'cursor\': row.cursor }" ng-repeat="col in renderedColumns" ng-class="col.colIndex()" class="ngCell {{col.cellClass}}"><div class="ngVerticalBar" ng-style="{height: rowHeight}" ng-class="{ ngVerticalBarVisible: !$last }">&nbsp;</div><div ng-cell></div></div></div>'
		/*
		rowTemplate: '<div  ng-repeat="col in renderedColumns" ng-class="col.colIndex()" class="ngCell {{col.cellClass}}"><div class="ngVerticalBar" ng-style="{height: rowHeight}" ng-class="{ ngVerticalBarVisible: !$last }">&nbsp;</div><div ng-cell></div></div>'
        showGroupPanel: true,
        jqueryUIDraggable: true*/
    };
	
	
	// explain click
	$scope.onInspect = function(){
		console.log("clicked");
		$scope.open('lg','views/listModal.html','ListModalController')
	}
	// on fraud
	$scope.onFraud = function(){
		dataService.selection.confirmed = "true";
	}
	
	// MODAL CONTROL POP
	$scope.open = function (size,template,controller) {

		var modalInstance = $modal.open({
			templateUrl: template,
			controller: controller,
			size: size,
			resolve: {
				items: function () {
				return $scope.items;
				}
			}
		});

		modalInstance.result.then(function (selectedItem) {
			$scope.response = selectedItem;
			console.log($scope.response);
		}, function () {
			$log.info('Modal dismissed at: ' + new Date());
		});
	
	};
	
}]);