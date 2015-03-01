app.controller('CartogramController', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	var sc = this;
	
	$scope.isCollapsed = false;
	$scope.data;
	$scope.inTerms = dataService.inTerms;
	$scope.countrySelected;
	
	angular.element($window).on('resize', function(){ $scope.$apply() }); // IF WINDOW IS RESIZED reapplies scope

	// applies all changes to ramp density and rate based on previous events
	
	$scope.onCountryClick = function (item) {
		var country = item;
//		dataService.changeSelection(country);
		$scope.countrySelected = item;
		dataService.changeCountrySelection(country);
	}
	
	$scope.$on("broadcastMapCountriesData", function(){
		$scope.data = dataService.map_data;
		console.log($scope.data);
	});
	
	$scope.$on("broadcastStatsClick", function(){
		$scope.inTerms = dataService.inTerms;
	});
	
	$scope.refreshClick = function() {
		dataService.changeStatsClick("area");
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