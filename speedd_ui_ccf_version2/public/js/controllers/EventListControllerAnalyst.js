app.controller('EventListControllerAnalyst', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list
	
	
	$scope.isCollapsed = false;
	
	$scope.eventList = [];
	$scope.formattedEventList = [];
	$scope.listSelection=[];
    
    $scope.transaction = {ts: [
                    2224699200000,
                    2224699203000,
                    2224699209000
                    ],
                    id: [
                    "9d47b44380024e82aedcf359f8be8dfb",
                    "8727070543664142bc7d2ba980dc1f19",
                    "b1dedbeb44ad431f995076153c7bdcc8"
                    ]};
                    
    $scope.transactionData;
	$scope.transactionTime;
    $scope.transactionCountries = ["RO","DE"]
    $scope.cardCountry = ["IS"];
    $scope.transactionCost = 50;
	$scope.reasonImg;
	$scope.certaintyImg;
    $scope.amountSum = 50;
    
    $scope.tSeq = $scope.transaction.transactions;
	
    
    $scope.analyst=false;
    
    $scope.number = 5;
    $scope.getNumber = function(num) {
        return new Array(num);   
    }
	
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
		event.time = (dataToFormat.attributes.OccurrenceTime != undefined)? new Date(parseInt(dataToFormat.attributes.OccurrenceTime)).toString() : "";
		event.name = (dataToFormat.name != undefined)? dataToFormat.name : "";
		event.country = (dataToFormat.attributes.card_country != undefined)? dataService.map_data2.get(dataToFormat.attributes.card_country).name.common : "";
	//	event.usedIn = (dataToFormat.acquirer_country != undefined)? dataService.map_data2.get(dataToFormat.acquirer_country[0]).name.common : "";
		event.cost = (dataToFormat.attributes.Cost != undefined)? dataToFormat.attributes.Cost : "";
		event.reason = (dataToFormat.reason != undefined)? dataToFormat.reason : "";
        event.certainty = (dataToFormat.attributes.Certainty != undefined)? (parseFloat(dataToFormat.attributes.Certainty)*100).toFixed(2) : "";
		event.confirmed = "false";
        event.analyst = "img/analyst_idle.png";
        
        /// formatting data for transaction view window
        timestamps = [];
        transaction_ids = [];
        
        dataToFormat.attributes.timestamps.forEach(function(e){
            timestamps.push(parseInt(e));
        })
        dataToFormat.attributes.transaction_ids.forEach(function(e){
            transaction_ids.push(e);
        })   
        
 //       var transactions = dataService.findTransactionById(transaction_ids);
        event.data = {ts:timestamps, id:transaction_ids}
        $scope.transaction = event.data;
           
        //
        transactionCountries = dataToFormat.attributes.acquirer_country;
        tc = [];
        transactionCountries.forEach(function(e){
            tc.push(dataService.map_data2.get(e).cca2);
        })
        event.countries = tc;
  //      console.log($scope.transactionCountries)
        ///////////////////////////
        
        $scope.number = 2;
        
        if (parseInt(event.certainty) > 70)
            $scope.certainty = "img/system_fraud.png";
        else
            $scope.certainty = "img/system_yellow.png";    
		
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
        
        $scope.transactionData=item;
        /////
        $scope.transaction = item.data;

        $scope.transactionCountries = item.countries;
     //           console.log(item.countries);
        $scope.transactionCost = item.cost;
		
		$scope.transactionTime = dateFormat(item.time, "dddd, mmmm dS, yyyy, h:MM:ss TT");
		
		// change reason Image
		$scope.reasonImg = (item.name == "TransactionsInFarAwayPlaces")? "img/far_away.png":"img/sudden.png";
		// change certainty Image
		$scope.certaintyImg = (item.certainty > 70)? "img/system_fraud2.png":"img/system_yellow2.png";
		
        
        //determine transactions associated with event
        var transactions = dataService.findTransactionById($scope.transaction.id);
        var s= 0;
        transactions.forEach(function(e){
            s += parseFloat(e.attributes.amount_eur);
        })
        $scope.amountSum = s;
        console.log($scope.amountSum);
    };
    
    //////////////////////////////////////////
    $scope.analysts = [{name: "Vasile", src: "img/analyst_green.png"}, {name: "Gheorghe", src: "img/analyst_fraud.png"}];
   
    function regenAnalystsTemplate(){
        $scope.analysts = [{name: "Vasile", src: "img/analyst_green.png"}, {name: "Gheorghe", src: "img/analyst_fraud.png"},{name: "Vasile", src: "img/analyst_green.png"}];
    }
    
        
	
	$scope.gridOptions = { 
        data: 'formattedEventList',
		enablePinning: true, // pin columns
		selectedItems: $scope.listSelection, // enables selection of row
		multiSelect: false,
		enableColumnResize: true,
        columnDefs: [{ field: 'id'},
                    { field: 'time', displayName: "Time"},
                    { field: 'country', displayName: "Country"},
                    { field: 'cost', displayName: "Cost"},
                    { field: 'name', displayName: "Reason"},
                    { field: 'confirmed', displayName: "Investigated"},
                    { field: 'certainty', displayName: "Certainty", cellTemplate: 'views/certaintyCellTemplate.html'},
                    { field: 'analyst', displayName: "Analyst", width: 100, cellTemplate: 'views/analystCellTemplate2.html' }, //'views/analystCellTemplate2.html'
                    { field: 'data', displayName: "Other Info"},
                    { field: 'countries', displayName: "Used In"}
        ],
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
		
		// modal disabled for 3D DEMO -- 16-10-15
		$scope.open('lg','views/listModal.html','ListModalController')

/*		TREEMAP AND RIVER CODE
		// code that reloads 3D treemap -- 16-10-15
		 filename = f[randomInt(0,6)];    
         reload(); // function in -- public/js/3d_treemap.js
		////////////////////////////////////////
		redrawAccountHistory();   // function in -- public/js/account_history.js
*/		 
	}
	// on fraud
	$scope.onFraud = function(){
		dataService.selection.confirmed = "true";
        
        dataService.selection.analyst = "img/analyst_fraud.png"
        
        // emit transaction
        dataService.emitAnalystAction(dataService.selection);
	}
    $scope.onAllow = function(){
		dataService.selection.confirmed = "true";
        
        dataService.selection.analyst = "img/analyst_green.png";
        
        // emit transaction
        dataService.emitAnalystAction(dataService.selection);
	}
	
    $scope.onSequenceTransactionClick = function(id){
        dataService.transactionSelected = id;
		console.log(id)
        
        $scope.open('lg','views/transactionModal.html','TransactionModalController')
	}
    /*
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
	*/ 
    
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