
var app = angular.module('myApp', ['ui.bootstrap','ngGrid']);
   //camel cased directive name
   //in your HTML, this will be named as bars-chart
    
   
app.directive('barsChart', function ($parse) {
     //explicitly creating a directive definition variable
     //this may look verbose but is good for clarification purposes
     //in real life you'd want to simply return the object {...}
	return{
         //We restrict its use to an element
         //as usually  <bars-chart> is semantically
         //more understandable
         restrict: 'E',
         //this is important,
         //we don't want to overwrite our directive declaration
         //in the HTML mark-up
         replace: false,
         //our data source would be an array
         //passed thru chart-data attribute

         link: function (scope, element, attrs) {
           //in D3, any selection[0] contains the group
           //selection[0][0] is the DOM node
           //but we won't need that this time
           var chart = d3.select(element[0]);
		   
		   //this parsing the scope data which is set in the controller
		    var exp = $parse(attrs.chartData);
			var datax = exp(scope);
			
           //to our original directive markup bars-chart
           //we add a div with out chart styling and bind each
           //data entry to the chart
            chart.append("div").attr("class", "chart")
             .selectAll('div')
             .data(datax).enter().append("div")
             .transition().ease("elastic")
             .style("width", function(d) { return d + "%"; })
             .text(function(d) { return d + "%"; });
           //a little of magic: setting it's width based
           //on the data value (d) 
           //and text all with a smooth transition
		}  
    };
});

app.controller('mainController', function($scope, socket) {
  
  // BUTTONS ======================
	
		 // define some random object and button values
		  $scope.bigData = {};
		  
		  $scope.bigData.breakfast = false;
		  $scope.bigData.lunch = false;
		  $scope.bigData.dinner = false;
		  
		  // socket sends messages !!!!!!!!!!!!!!!!!!!!!!!! --- just need to send socket as argument to the controller function
		  socket.on('news', function (data) {
			console.log(data)
			});
});



