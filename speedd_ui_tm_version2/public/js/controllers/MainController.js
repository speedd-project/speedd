app.controller('MainController', function($scope, socket,rampDataService) {
  
  // BUTTONS ======================
	
		 // define some random object and button values
		  $scope.bigData = {};
		  
		  $scope.bigData.breakfast = false;
		  $scope.bigData.lunch = false;
		  $scope.bigData.dinner = false;
		  
		  $scope.rampList;
		  
		  // socket sends messages !!!!!!!!!!!!!!!!!!!!!!!! --- just need to send socket as argument to the controller function
		  socket.on('news', function (data) {
			console.log(data)
			});
		
		
});