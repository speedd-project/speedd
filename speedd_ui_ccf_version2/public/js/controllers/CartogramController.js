app.controller('CartogramController', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	var sc = this;
	
	$scope.isCollapsed = false;
	$scope.data;
	$scope.inTerms = dataService.inTerms;
	$scope.countrySelected;
    $scope.countries = ["United Kingdom","France"]; // variable stores a 2 elem array containting countries to draw an arc to and from
	$scope.flaggedTransactions = [];
    
    $scope.transactionCountries = ["RO","DE"];
    
    
//    $scope.transactionCountries = ["RO","DE"];
    
	angular.element($window).on('resize', function(){ $scope.$apply() }); // IF WINDOW IS RESIZED reapplies scope

	// applies all changes to ramp density and rate based on previous events
	
	$scope.onCountryClick = function (item) {
		var country = item;
//		console.log(item)
//		dataService.changeSelection(country);
		$scope.countrySelected = item;
		dataService.changeCountrySelection(country);
		
	}
	
	$scope.$on("broadcastMapCountriesData", function(){
		$scope.data = dataService.map_data;   // variable for cartogram d3js vanilla 
        //$scope.data = dataService.map_data_name; //variable for datamaps
		console.log($scope.data);
	});
	
	$scope.$on("broadcastStatsClick", function(){
		$scope.inTerms = dataService.inTerms;
	});
    $scope.$on("broadcastSelectionChanged", function(){
		$scope.transactionCountries = dataService.selection.countries;
	});
    
    
    ////////////// NEW EVENTS
    $scope.$on("SuddenCardUseNearExpirationDate", function(){
		$scope.flaggedTransactions.push(dataService.rawEventList[dataService.rawEventList.length-1])
	});
    $scope.$on("TransactionsInFarAwayPlaces", function(){
		$scope.flaggedTransactions.push(dataService.rawEventList[dataService.rawEventList.length-1])
	});
    
    
    ///////////////////////////
	
	$scope.refreshClick = function() {
        //make it random so "mapsDirective" detects changes for inTerms even if same (triggers default case)
        var refresh = dataService.randomInt(0,100).toString();
        
		dataService.changeStatsClick(refresh);//("area");
	}
	
	$scope.onBtnClick = function(){
		console.log("clicked");
		$scope.open('lg','views/countryModal.html','CartogramModalController')
	}
	
	
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