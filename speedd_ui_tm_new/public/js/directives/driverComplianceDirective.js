app.directive('driverCompliance', function($parse, $window){
   return{
      restrict:'EA',
//      template:"<svg id='svg-chart' width='850' height='200'></svg>", // can either use template or (d3.select && d3.append.attr)
				 //angscope, dom, data bound in html
      link: function(scope, elem, attrs){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)

           var exp = $parse(attrs.distanceData);
		
					// exp(scope) 		is equivalent to 		scope.main.salesData ------ however the controller needs to be imported into the HTML as main
           var distanceDataToPlot=exp(scope); // don't know what happens to scope if more controllers are added to the same directive (DOM elem)
		   
           var d3 = $window.d3; // adds d3 library in the context of the directive
		   
		
			// listens for WINDOW RESIZE
		   scope.$watch(function(){
//			console.log("resize");
			drawDrivers();
			updateGauge("nspeed",distanceDataToPlot[0].speed);
			updateGauge("sspeed",distanceDataToPlot[1].speed);
			})
			
 //          var rawSvg=elem.find('svg');		// don't actually need this - duplicates the function of d3.select("#id")
 //          var svg = d3.select("#svg-drivers");
	
			// $watchCollection executes on data change 			
		scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               distanceDataToPlot=newVal;
               drawDrivers();
			   updateGauge("nspeed",distanceDataToPlot[0].speed);
			   updateGauge("sspeed",distanceDataToPlot[1].speed);
           });

		   function drawDrivers()
		   { ////// d3 stuff
		   
			   d3.select("#svg-drivers").remove();
				// get container size
				var margin = { top: -10, right: 10, bottom: -10, left: 10 }
				, width = parseInt(elem[0].clientWidth) - margin.left - margin.right
				, height = parseInt(elem[0].clientHeight) - margin.top - margin.bottom;
	  
				// set SVG size
				var s = d3.select(elem[0]).append('svg').attr('id','svg-drivers')
					.attr('width', width + margin.right + margin.left)
					.attr('height', height + margin.top + margin.bottom);
				var svg = d3.select("#svg-drivers");	
//					svg.style("background-color","#FF8673");
/*			   
				var y = d3.scale.linear()
			  .domain([0, 350])
			  .range([height, 0]);
*/
				var g = svg.append('g')
						.attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
						.attr('width', width)
						.attr('height', height)
						.attr('class', 'main');
/*
				// draws the y axis and appends it to chart
				var yAxis = d3.svg.axis()
						.scale(y)
						.orient('left');

				g.append('g')
						.attr('transform', 'translate(0,0)')
						.attr('class', 'y axis')
						.call(yAxis)
					.append("text")
						.attr("class", "label")
						.attr("transform", "rotate(-90)")
						.attr("y", 6)
						.attr("dy", ".71em")
						.style("text-anchor", "end")
						.text("Speed");

				// DRAW speed indicator (horizontal line)
//				var linePoints = [[{ x: 0, y: speed }, { x: width / 4, y: speed }]];

				var line = d3.svg.line()
					.interpolate("basis")
					.x(function (d) { return (d.x); })
					.y(function (d) { return y(d.y); });

				g.selectAll(".line")
					.data(linePoints)
				  .enter().append("path")
					.attr("class", "line")
					.attr("d", line)
					.style('stroke', 'black')
					.style('stroke-width', 4);
*/
				// add the car image to the svg
				var img = g.append("svg:image")
					.attr("xlink:href", "img/2d_car_lightgrey_s.png")
					.attr("width", 50)
					.attr("height", 100)
					.attr("x", width / 2 - 60)
					.attr("y", height / 3);

				var img2 = g.append("svg:image")
					.attr("xlink:href", "img/2d_car_lightgrey.png")
					.attr("width", 50)
					.attr("height", 100)
					.attr("x", width / 2 + 20 )
					.attr("y", height / 3);
					
				// add distance indicators
				var color = d3.scale.linear()
					  .domain([11, 0])
					  .range(['#1C9CA7', '#FF6B6B'])

				var dist = g.selectAll('rect').data(distanceDataToPlot)
						.enter().append('rect')
							.attr('x', function (d, i) {if (i == 0) return width / 2 + 20; else return width / 2 - 60;})//{ if (i == 0 || i == 1) return width / 2 + 20; else return width / 2 - 60; })
							.attr('y', function (d, i) { if (i == 0) return (height / 3 - 5) - d.distance * 7; else return (height / 3 + 105); })//{ if (i == 0 || i == 1) return (height / 3 - 5) - d * 7; else return (height / 3 + 105); })
							.attr('width', function (d, i) {/* if (i == 1 || i == 3) return 30; else */return 50 })
							.attr('height', function (d) { return d.distance * 7 })
							.style('fill', function (d, i) { /*if (i == 1 || i == 3) return "steelblue"; else */return color(d.distance) })
							.append("title").text(function (d) { return d.distance+"m"});
/*				
				var gauge = g.append("g").attr("id","nspeedGaugeContainer")
					.attr("width", 120)
						.attr("height", 120)
						.attr("x", width / 2 + 100 )
						.attr("y", height / 3)
						.style("fill-opacity","0");
*/
				//    d3.select(window).on('resize', redrawDrivers);
				
			}
		    
			///////////////////////////////////////////////////////////
			//////////////////// GAUGES			/// using example: http://bl.ocks.org/tomerd/1499279
			var gauges = [];
			
			function createGauge(name, label, min, max)
			{
				var config = 
				{
					size: 120,
					label: label,
					min: undefined != min ? min : 0,
					max: undefined != max ? max : 250,
					minorTicks: 5
				}
				
				var range = config.max - config.min;
				config.yellowZones = [{ from: config.min + range*0.75, to: config.min + range*0.9 }];
				config.redZones = [{ from: config.min + range*0.9, to: config.max }];
				
				gauges[name] = new Gauge(name + "GaugeContainer", config);
				gauges[name].render();
			}
			
			function createGauges()
			{
				createGauge("nspeed", "N Speed");
				createGauge("sspeed", "S Speed");
			}
			
			function updateGauge(id, value)
			{
				gauges[id].redraw(value);
			}
			/*
			function updateGauges(value)
			{
				for (var key in gauges)
				{
					gauges[key].redraw(value);
					console.log(key);
				}
			}*/
			
			function initialize()
			{
				createGauges();
				//setInterval(updateGauges, 5000);
			}
			
		    console.log(gauges);
            drawDrivers();
			initialize();
       }
   };
});