app.directive('liveGraph', function($parse, $window){
	return{
		restrict:'EA',
//      template:"<svg id='svg-chart' width='850' height='200'></svg>", // can either use template or (d3.select && d3.append.attr)
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
//			   addPointToDataset();
//			   console.log("CHANGED");
			});
			// append a div only once
			d3.select(elem[0]).append('div').attr("id","visjs");

//			console.log(dataToPlot);
			
/*			
			function addPointToDataset(){
				var index = dataToPlot.time.length-1;
				var xAxis = vis.moment(new Date(dataToPlot.time[index]));
				var rate = dataToPlot.rate[index];
				var density = dataToPlot.density[index];
				
				dataset.add({x:xAxis , y: rate, group: 0});
				dataset.add({x:xAxis , y: density, group: 1});
				
			}
*/			
			function drawChart()
			{ ////// d3 stuff
		   
				
				var DELAY = 1000; // delay in ms to add new data points

				//  var strategy = document.getElementById('strategy');
				  // create a graph2d with an (currently empty) dataset
				  
				var container = document.getElementById('visjs');
//				var dataset = new vis.DataSet();
				var groups = new vis.DataSet();
				
				groups.add({
					id: 0,
					content: "Rate",
					options: {
						drawPoints: {
							style: 'circle' // square, circle
						},
						shaded: {
						  orientation: 'bottom' // top, bottom
						}
					}
				});
				
				groups.add({
					id: 1,
					content: "Density",
					options: {
						drawPoints: {
							style: 'circle' // square, circle
						},
						shaded: {
						  orientation: 'bottom' // top, bottom
						}
					}
				});
				
				var options = {
					defaultGroup: 'ungrouped',
					legend: true,
					graphHeight:500,
					start: vis.moment().add(-30, 'seconds'), // changed so its faster
					end: vis.moment(),
//					start: '2015-02-10',
//					end: '2015-02-20',
					showMajorLabels:true,
					showMinorLabels:true
				};
				/*
				dataPoints = [{x: '2014-06-13', y: 60, group: 0},
					{x: '2014-06-14', y: 40, group: 0},
					{x: '2014-06-15', y: 55, group: 0},
					{x: '2014-06-16', y: 40, group: 0},
					{x: '2014-06-17', y: 50, group: 0},
					{x: '2014-06-13', y: 30, group: 0},
					{x: '2014-06-14', y: 10, group: 0},
					{x: '2014-06-15', y: 15, group: 1},
					{x: '2014-06-16', y: 30, group: 1},
					{x: '2014-06-17', y: 10, group: 1},
					{x: '2014-06-18', y: 15, group: 1},
					{x: '2014-06-19', y: 52, group: 1},
					{x: '2014-06-20', y: 10, group: 1},
					{x: '2014-06-21', y: 20, group: 1},
					{x: '2014-06-22', y: 60, group: 1},
					{x: '2014-06-23', y: 10, group: 1},
					{x: '2014-06-24', y: 25, group: 1},
					{x: '2014-06-25', y: 30, group: 1},
					{x: '2014-06-26', y: 20, group: 0},
					{x: '2014-06-27', y: 60, group: 0},
					{x: '2014-06-28', y: 10, group: 0},
					{x: '2014-06-29', y: 25, group: 0},
					{x: '2014-06-30', y: 30, group: 0}
				];*/
				
				var dataPoints = [];
				for (var i=0;i<dataToPlot.density.length;i++)
				{
					var xAxis = vis.moment(new Date(dataToPlot.time[i]));
					var rate = dataToPlot.rate[i];
					var density = dataToPlot.density[i];
					
					dataPoints.push({x:xAxis , y: rate, group: 0});
					dataPoints.push({x:xAxis , y: density, group: 1});
				}
				console.log(dataPoints);
				// dataset to plot
				var dataset = new vis.DataSet(dataPoints);
/*
				var options = {
					start: vis.moment().add(-30, 'seconds'), // changed so its faster
					end: vis.moment(),
					dataAxis: {
					  customRange: {
						left: {
						  min:-10, max: 10
						}
					  }
					},
					drawPoints: {
					  style: 'circle' // square, circle
					},
					shaded: {
					  orientation: 'bottom' // top, bottom
					}
				};*/
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

				/**
				* Add a new datapoint to the graph
				*/
				function addDataPoint() {
					// add a new data point to the dataset
					var now = vis.moment();
					dataset.add({
						x: now,
						y: y(now / 1000),
						group:1
					});
					
					dataset.add({
						x: now,
						y: y(now / 800),
						group:0
					});

					// remove all data points which are no longer visible
/*					var range = graph2d.getWindow();
					var interval = range.end - range.start;
					var oldIds = dataset.getIds({
						filter: function (item) {
							return item.x < range.start - interval;
						}
					});
					dataset.remove(oldIds);
*/
					setTimeout(addDataPoint, DELAY);
				}
			//	addDataPoint();
				return dataset;
			}
		   
          /* var dataset = */
		    drawChart();
		}	
    };
});