app.directive('rampList2Directive', function ($parse) {
	return{
        restrict: 'EA',
        replace: false,
 
        link: function (scope, element, attrs) {
        
			var chart = d3.select(element[0]);
		   
		    var exp = $parse(attrs.chartData);
			var data = exp(scope);
			
			var colorscale = d3.scale.linear()
					.domain([1, 100])
					.range(["green", "#f57e20"]);
			var opacityscale = d3.scale.linear()
					.domain([1, 100])
					.range([0.3, 1]);
			
			scope.$watchCollection(exp, function(newVal, oldVal){	// redraw graph if data is added or removed
               data=newVal;
               drawRamps();
           });
		   
		   scope.$watch(function(){
//			console.log("resize");
			drawRampsNoAnimation();
			})
			
			scope.$watch(exp, function () {	// redraw graph if a data point attribute is changed
			  drawRamps();
			}, true);
			
			function drawRamps(){
				chart.select("#container").remove();
				var container = chart.append("div").attr("id","container")//.style("height","50px");
				var divs = container.selectAll('div')
					.data(data).enter().append("div").attr("class", "chart2")
					.append("text").text(function(d) { return "Ramp "+d.id; });
				
				divs.append("div")//.attr("class", "chart2")
						.transition().ease("elastic")
						.style("width", function(d) { return d.density + "%"; })
						.style("opacity",function(d) { return opacityscale(d.density);})
						.text(function(d) { return d.density + "%"; })
						.style("background-color", "#f57e20");//function(d) { return colorscale(d.density); });
				divs.append("div")//.attr("class", "chart2")
						.transition().ease("elastic")
						.style("width", function(d) { return d.rate + "%"; })
						.style("opacity",function(d) { return opacityscale(d.rate);})
						.text(function(d) { return d.rate + "%"; })
						.style("background-color", "steelblue");//function(d) { return colorscale(d.rate); });
			}
			
			function drawRampsNoAnimation(){
				chart.select("#container").remove();
				var container = chart.append("div").attr("id","container")//.style("height","50px");
				var divs = container.selectAll('div')
					.data(data).enter().append("div").attr("class", "chart2")
					.append("text").text(function(d) { return "Ramp "+d.id; });
				
				divs.append("div")//.attr("class", "chart2")
						.style("width", function(d) { return d.density + "%"; })
						.style("opacity",function(d) { return opacityscale(d.density);})
						.text(function(d) { return d.density + "%"; })
						.style("background-color", "#f57e20");//function(d) { return colorscale(d.density); });
				divs.append("div")//.attr("class", "chart2")
						.style("width", function(d) { return d.rate + "%"; })
						.style("opacity",function(d) { return opacityscale(d.rate);})
						.text(function(d) { return d.rate + "%"; })
						.style("background-color", "steelblue");//function(d) { return colorscale(d.rate); });
			}
			drawRamps();
		}  
    };
});