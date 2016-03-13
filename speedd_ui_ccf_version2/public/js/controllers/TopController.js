app.controller('TopController', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list

	$scope.selection=dataService.selection;
	
	$scope.transactions = [1234124,14325];
	$scope.flagged = [5951,132];
	$scope.amount = [51,12];
	$scope.volume = [1,45465];
	
	$scope.$on("broadcastSelectionChanged", function(){
		$scope.selection = dataService.selection;
	});
	$scope.data = [1,50,26,73,15,34,26,20,45,96,45,32,15,79,78,45,12,65,1,50,26,73,15,34,26,20,45,96,45,32,15,79,78,45,12,65];
	
	$scope.onClick = function(){
		console.log("clicked");
		$scope.open('lg','views/graphModal.html','HistogramController')
	}
	$scope.transClick = function() {
		dataService.changeStatsClick("transactions");
	}
	$scope.flagClick = function() {
		dataService.changeStatsClick("flagged");
	}
	$scope.amountClick = function() {
		dataService.changeStatsClick("amount");
	}
	$scope.volumeClick = function() {
		dataService.changeStatsClick("volume");
	}
	
	$scope.$on("broadcastFraudAtATM", function(){
//		$scope.transactions.push(dataService.rawEventList[dataService.rawEventList.length-1]);
		$scope.transactions[$scope.transactions.length-1]++;
		$scope.flagged[$scope.flagged.length-1]++;
	});
	
	$scope.$on("broadcastIncreasingAmounts", function(){
		$scope.transactions[$scope.transactions.length-1]++;
		$scope.flagged[$scope.flagged.length-1]++;
	});
	
	$scope.$on("broadcastTransaction", function(){
		$scope.transactions[$scope.transactions.length-1]++;
	});
	
	$scope.$on("broadcastTransactionStats", function(){
		$scope.amount[$scope.amount.length-1] = dataService.rawEventList[dataService.rawEventList.length-1].attributes.average_transaction_amount_eur;
		$scope.volume[$scope.volume.length-1] = dataService.rawEventList[dataService.rawEventList.length-1].attributes.transaction_volume;
	});
	
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