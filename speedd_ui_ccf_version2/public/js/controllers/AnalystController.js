app.controller('AnalystController', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list
	
	
	$scope.isCollapsed = false;
	
	$scope.eventList = [];
	$scope.formattedEventList = [];
	$scope.listSelection=[];
    
    
    $scope.analyst=false;
    
    
	
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
    
    $scope.$on("SuddenCardUseNearExpirationDate", function(){
		var event = clone(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.eventList.push(event);
		formatEvent(event);
		console.log(event);
	});
    $scope.$on("TransactionsInFarAwayPlaces", function(){
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
		event.time = (dataToFormat.OccurrenceTime != undefined)? new Date(dataToFormat.OccurrenceTime).toString() : "";
		event.name = (dataToFormat.Name != undefined)? dataToFormat.Name : "";
		event.country = (dataToFormat.card_country != undefined)? dataService.map_data2.get(dataToFormat.card_country).name.common : "";
	//	event.usedIn = (dataToFormat.acquirer_country != undefined)? dataService.map_data2.get(dataToFormat.acquirer_country[0]).name.common : "";
		event.certainty = (dataToFormat.Certainty != undefined)? (parseFloat(dataToFormat.Certainty)*100).toFixed(2) : "";
		event.cost = (dataToFormat.Cost != undefined)? dataToFormat.Cost : "";
		event.reason = (dataToFormat.reason != undefined)? dataToFormat.reason : "";
		event.confirmed = "false";
        event.analyst = 5;
		
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
    
    //////////////////////////////////////////
    $scope.analysts = [{name: "Vasile", src: "img/analyst_green.png"}, {name: "Gheorghe", src: "img/analyst_fraud.png"}];
   
    function regenAnalystsTemplate(){
        $scope.analysts = [{name: "Vasile", src: "img/analyst_green.png"}, {name: "Gheorghe", src: "img/analyst_fraud.png"},{name: "Vasile", src: "img/analyst_green.png"}];
    }
    
        

	
	// explain click
	$scope.onInspect = function(){
		console.log("clicked");
		
		// modal disabled for 3D DEMO -- 16-10-15
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
            backdrop: true,
            outsideClick: false,
            keyboard: false,
			resolve: {
				items: function () {
				return $scope.items;
				}
			}
		});

		modalInstance.result.then(function (selectedItem) {
			$scope.response = selectedItem;
            // update analyst name
            $scope.analyst = selectedItem;
            // update analyst list
            dataService.updateAnalyst($scope.analyst);       
            
            
			console.log($scope.response);
		}, function () {
			$log.info('Modal dismissed at: ' + new Date());
		});
	
	};
    
    setTimeout(function(){
        $scope.open('lg','views/analystLogin.html','LogInModalController')
    },300)
    
	
}]);