app.controller('TransactionModalController', ['$scope','$interval','$window','dataService','$modalInstance','socket', function($scope, $interval,$window,dataService,$modalInstance,socket){
    
	$scope.isCollapsed = false;
	$scope.selectionId = dataService.transactionSelected;
    $scope.selection = dataService.findTransactionById([$scope.selectionId])[0];
    $scope.rowSelection = dataService.selection;
    
 //   console.log($scope.selection);
    
    $scope.cardCountry = (dataService.map_data2.get($scope.selection.attributes.card_country) != undefined)? dataService.map_data2.get($scope.selection.attributes.card_country).name.common : $scope.selection.attributes.card_country;
    $scope.occurrenceTime = ((new Date($scope.selection.attributes.OccurrenceTime) )!= null)? dateFormat(new Date($scope.selection.attributes.OccurrenceTime), "dddd, mmmm dS, yyyy, h:MM:ss TT"):$scope.selection.attributes.OccurrenceTime;
    $scope.cardExpiration = ((new Date($scope.selection.attributes.card_exp_date) )!= null)? dateFormat(new Date($scope.selection.attributes.card_exp_date), "dddd, mmmm dS, yyyy, h:MM:ss TT"):$scope.selection.attributes.card_exp_date;
	$scope.detectionTime = ((new Date($scope.selection.attributes.DetectionTime) )!= null)? dateFormat(new Date($scope.selection.attributes.DetectionTime), "dddd, mmmm dS, yyyy, h:MM:ss TT"):$scope.selection.attributes.DetectionTime;
    $scope.acquirer_country = (dataService.map_data2.get($scope.selection.attributes.acquirer_country) != undefined)? dataService.map_data2.get($scope.selection.attributes.acquirer_country).name.common : $scope.selection.attributes.card_country;
   

	$scope.fraud = function () {
		$scope.selection.confirmed = true;
		$modalInstance.close("fraud");
	};

	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};
	
	
	
}]);