
var app = angular.module('myApp', ['ui.bootstrap','uiGmapgoogle-maps','ngGrid']);
   //camel cased directive name
   //in your HTML, this will be named as bars-chart
   
   
app.controller('Ctrl', ['$scope','$interval', function($scope, $interval){
    
	var sc = this;
	$scope.myData = [10,20,95,40,60, 80, 20, 50];
	$scope.myData2 = [5,15,25,35,45, 55, 65, 75];
	$scope.isCollapsed = false;
}]);
   
   
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



app.controller('gridController', function($scope) {
  
   $scope.myData = [{name: "Moroni", age: 50},
                     {name: "Tiancum", age: 43},
                     {name: "Jacob", age: 27},
                     {name: "Nephi", age: 29},
                     {name: "Enos", age: 34}];
    $scope.gridOptions = { 
        data: 'myData',
        showGroupPanel: true,
        jqueryUIDraggable: true
    };
});



app.controller('OtherController', ['$scope','$interval','$window', function($scope, $interval,$window){
    
	var sc = this;

	$scope.dataRamps=[
        { id: 1, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 2, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 3, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 4, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 5, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 6, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 7, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0},
		{ id: 8, sensorId: 0, location: { lat: 0, lng: 0 }, status: randomInt(0, 3), lowerLimit: "Auto", upperLimit: "Auto", densityHistory: [0,1,15,4,3,9], rateHistory: [6,3,13,1,8,19], controlTypeHistory: ["auto","partial","auto","auto","full","partial"], marker:0}
    ];
	function randomInt(min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
	
}]);

app.directive('otherChart', function($parse, $window){
   return{
      restrict:'EA',
//      template:"<svg id='svg-chart' width='850' height='200'></svg>", // can either use template or (d3.select && d3.append.attr)
				 //angscope, dom, data bound in html
      link: function(scope, elem, attrs){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)

           var exp = $parse(attrs.otherData);
		
					// exp(scope) 		is equivalent to 		scope.main.salesData ------ however the controller needs to be imported into the HTML as main
           var otherDataToPlot=exp(scope); // don't know what happens to scope if more controllers are added to the same directive (DOM elem)
		   
           var padding = 20;
           var pathClass="path";
           var xScale, yScale, xAxisGen, yAxisGen, lineFun;

           var d3 = $window.d3; // adds d3 library in the context of the directive
		   
		   
		   ////////////////////////////////////this is awesome if I don't want to use template/////	  
			// get container size
			var margin = { top: -30, right: 10, bottom: -30, left: 10 }
			, width = parseInt(elem[0].offsetWidth) - margin.left - margin.right
			, height = parseInt(elem[0].offsetHeight) - margin.top - margin.bottom;
			
			// set SVG size
			var s = d3.select(elem[0]).append('svg').attr('id','svg-ramp')
				.attr('width', width + margin.right + margin.left)
				.attr('height', height + margin.top + margin.bottom);

			//////////////////////////////////////////////////////////////////////////////////////
		   
 //          var rawSvg=elem.find('svg');		// don't actually need this - duplicates the function of d3.select("#id")
           var svg = d3.select("#svg-ramp");
	
			// $watchCollection executes on data change 			
		scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               otherDataToPlot=newVal;
               redrawRamps();
           });
        
         
         function drawRamps() {		// d3 stuff
			
				var colorscale = d3.scale.linear()
					.domain([0, 3])
					.range(["yellow", "green"]);
				
				var g = svg.append("g");                

				var squareWidth = parseInt(width / 5);
				var squareXDist = parseInt((squareWidth - 30) / 2);
				var squareHeight = parseInt(height / 4);
				var squareYDist =  parseInt((squareHeight - 30) / 2);
				
				var drawing = g.selectAll("rect").data(otherDataToPlot).enter()
						.append("rect")
							.attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
							.attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
							.attr("width", 30)
							.attr("height", 30)
							.style("fill", function (d, i) { return colorscale(d.status); })
				/// NEWWW -- gives rect an ID to be able to select and modify    
				.attr("id", function (d, i) { return "rectRamp" + i });
		
				// text on mouse hover
				drawing.append("title").text(function (d) { return d.status });//statusScale(d.status)});

				var labels = g.selectAll("text").data(otherDataToPlot).enter()
						.append("text")
							.text(function (d, i) { return d.id; })
							.attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
							.attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
							.style("font-size", "23px");

           }
		   
		   function redrawRamps()
		   {
		   
		   d3.select("#svg-ramp").remove();
		 
			// get container size
			var margin = { top: -30, right: 10, bottom: -30, left: 10 }
			, width = parseInt(elem[0].offsetWidth) - margin.left - margin.right
			, height = parseInt(elem[0].offsetHeight) - margin.top - margin.bottom;
  
			// set SVG size
			var s = d3.select(elem[0]).append('svg').attr('id','svg-ramp')
				.attr('width', width + margin.right + margin.left)
				.attr('height', height + margin.top + margin.bottom);
			var svg = d3.select("#svg-ramp");	
				
		   
			var colorscale = d3.scale.linear()
					.domain([0, 3])
					.range(["yellow", "green"]);
				
				var g = svg.append("g");                

				var squareWidth = parseInt(width / 5);
				var squareXDist = parseInt((squareWidth - 30) / 2);
				var squareHeight = parseInt(height / 4);
				var squareYDist =  parseInt((squareHeight - 30) / 2);
				
				var drawing = g.selectAll("rect").data(otherDataToPlot).enter()
						.append("rect")
							.attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
							.attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
							.attr("width", 30)
							.attr("height", 30)
							.style("fill", function (d, i) { return colorscale(d.status); })
				/// NEWWW -- gives rect an ID to be able to select and modify    
				.attr("id", function (d, i) { return "rectRamp" + i });
		
				// text on mouse hover
				drawing.append("title").text(function (d) { return d.status });//statusScale(d.status)});

				var labels = g.selectAll("text").data(otherDataToPlot).enter()
						.append("text")
							.text(function (d, i) { return d.id; })
							.attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
							.attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
							.style("font-size", "23px");
		   }
		   
		   
           drawRamps();
       }
   };
});


