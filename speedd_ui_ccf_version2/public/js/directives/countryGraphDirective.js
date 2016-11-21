app.directive('countryGraphDirective', function($parse, $window){
	return{
		restrict:'EA',
				 //angscope, dom, data bound in html
		link: function(scope, elem, attrs){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)

			var exp = $parse(attrs.chartData);
		
					// exp(scope) 		is equivalent to 		scope.main.salesData ------ however the controller needs to be imported into the HTML as main
            var dataToPlot=exp(scope); // don't know what happens to scope if more controllers are added to the same directive (DOM elem)
		   
			var padding = 20;
			var pathClass="path";
			var xScale, yScale, xAxisGen, yAxisGen, lineFun;

			var d3 = $window.d3; // adds d3 library in the context of the directive
		    
//			console.log(dataToPlot);
			
			// $watchCollection executes on data change 			
			scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               dataToPlot=newVal;
			});
			// append a div only once
			d3.select(elem[0]).append('div').attr("id","visjs");

	
			function drawChart()
			{ ////// d3 stuff
		   
				
				var DELAY = 1000; // delay in ms to add new data points

				//  var strategy = document.getElementById('strategy');
				  // create a graph2d with an (currently empty) dataset
				  
				var container = document.getElementById('visjs');
//				var dataset = new vis.DataSet();
				var groups = new vis.DataSet();
				
				groups.add({
//					className: "rate",
					id: 0,
					content: "No. Transactions",
					options: {
						drawPoints: {
							style: 'circle' // square, circle
						}
					}
				});
				
				groups.add({
//					className: "density",
					id: 1,
					content: "No. Flagged",
					options: {
						drawPoints: {
							style: 'circle' // square, circle
						}
					},
					
//					style: "color: red;"//"color: #F68275"
				});
				
				groups.add({
//					className: "density",
					id: 2,
					content: "Average Amount",
					options: {
						drawPoints: {
							style: 'circle' // square, circle
						}
					},
					
//					style: "color: red;"//"color: #F68275"
				});
				
				groups.add({
//					className: "density",
					id: 3,
					content: "Average Volume",
					options: {
						drawPoints: {
							style: 'circle' // square, circle
						}
					},
				});
								
				var dataPoints = [];
				var maxTime=0, maxTimeIndex;
				
				for (var i=0;i<dataToPlot.density.length;i++)
				{
					var xAxis = vis.moment(new Date(dataToPlot.time[i]));
					var rate = dataToPlot.rate[i];
					var density = dataToPlot.density[i];
					
					dataPoints.push({x:xAxis , y: rate, group: 0});
					dataPoints.push({x:xAxis , y: density, group: 1});
					
					if (maxTime<dataToPlot.time[i])
					{
						maxTime = dataToPlot.time[i];
						maxTimeIndex = i;
					}
				}
				console.log(dataPoints);
				// dataset to plot
				var dataset = new vis.DataSet(dataPoints);
				
				
				var options = {
					defaultGroup: 'ungrouped',
					legend: true,
					graphHeight:500,
					start: dataPoints[0].x, // changed so its faster
					end: dataPoints[maxTimeIndex].x,//.add(30, 'seconds'),
//					start: '2015-02-10',
//					end: '2015-02-20',
					showMajorLabels:true,
					showMinorLabels:true
				};

				var graph2d = new vis.Graph2d(container, dataset, groups, options);

				  // a function to generate data points
				function y(x) {
					return (Math.sin(x / 2) + Math.cos(x / 4)) * 5;
				}

				function renderStep() {
					// move the window (you can think of different strategies).
					var now = vis.moment();
//					console.log(now);
					var range = graph2d.getWindow();
					var interval = range.end - range.start;
					switch ('') {
						case 'continuous':
							// continuously move the window
							graph2d.setWindow(now - interval, now, {animate: false});
							requestAnimationFrame(renderStep);
						break;

						case 'discrete':
							graph2d.setWindow(now - interval, now, {animate: false});
							setTimeout(renderStep, DELAY);
						break;

						default: // 'static'
							// move the window 90% to the left when now is larger than the end of the window
							if (now > range.end) {
								graph2d.setWindow(now - 0.1 * interval, now + 0.9 * interval);
							}
							setTimeout(renderStep, DELAY);
						break;
					}
				}
				// function that scrolls the graph every second
//				renderStep();

				return dataset;
			}
		   
          /* var dataset = */
		    drawChart();
		}	
    };
});