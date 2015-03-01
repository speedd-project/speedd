app.directive('barChart', function ($parse) {
	return{
         restrict: 'EA',
         replace: false,
         link: function (scope, element, attrs) {
           var chart = d3.select(element[0]);

		    var exp = $parse(attrs.chartData);
			var data = exp(scope);

			// listens for WINDOW RESIZE
			scope.$watch(function(){
				draw();
			})
			
			scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               data=newVal;
               draw();
			});
			
			function draw(){
				
				chart.select("#histogramd3").remove();
				// gets latest 14 data points
				dataToPlot = data.slice(data.length-14);
				
				// get container size
				var margin = { top: -10, right: 10, bottom: -10, left: 10 }
				, width = parseInt(element[0].clientWidth) - margin.left - margin.right
				, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;

				// set SVG size
				var s = chart.append('svg').attr('id','histogramd3')
					.attr('width', width + margin.right + margin.left)
					.attr('height', height + margin.top + margin.bottom);
				var svg = chart.select("#histogramd3").append("g");
				
				// forward event to controller to open MODAL
				svg.on("click", function(d){ return scope.onClick();});
	
				var barscale = d3.scale.linear()
						.domain([0,d3.max(dataToPlot)])
						.range([0,height-40]);
				
	
				var bar = svg.selectAll("rect")
					.data(dataToPlot)
				  .enter().append("g");

		//			console.log(width);	
				
				bar.append("rect")
					.attr("class","bar")
					.attr("x", function(d,i){return (width/2 - dataToPlot.length*15/2)+i*15;})
					.attr("y",function(d){ return height-barscale(d)-30})
					.attr("width", function (d,i) {return (10);})
					.attr("height", function(d) { return barscale(d); });
				// add text on mouse hover (shows datapoint value) -- for styled tooltips see: http://bl.ocks.org/ilyabo/1373263
				bar.append("title").text(function(d){return d;});
					
					
			}
			
			draw();
		}  
    };
});