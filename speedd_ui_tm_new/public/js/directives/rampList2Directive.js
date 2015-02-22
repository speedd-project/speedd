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
					
			var barscale = d3.scale.linear()
					.domain([1, 100])
					.range([0,90]);
			
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
					.data(data).enter().append("div").attr("class", function(d,i){return "chart2 col-xs-4 col-md-4 col-sm-4";}) //.style("position","relative").style("left", function (d,i){ if (i>8) return "50%"; else return "0px";})
					.append("text").text(function(d) { return "Ramp "+d.id; });
				
				divs.append("div")//.attr("class", "chart2")
						.transition().ease("elastic")
						.style("width", function(d) { return barscale(d.density) + "%"; })
//						.style("opacity",function(d) { return opacityscale(d.density);})
						.text(function(d) { return d.density + "%"; })
						.style("background-color", "#F68275");//function(d) { return colorscale(d.density); });
				divs.append("div")//.attr("class", "chart2")
						.transition().ease("elastic")
						.style("width", function(d) { return barscale(d.rate) + "%"; })
//						.style("opacity",function(d) { return opacityscale(d.rate);})
						.text(function(d) { return d.rate + "%"; })
						.style("background-color", "#1C9CA7");//function(d) { return colorscale(d.rate); });
			}
			
			function drawRampsNoAnimation(){
				chart.select("#container").remove();
				var container = chart.append("div").attr("id","container")//.style("height","50px");
				var divs = container.selectAll('div')
					.data(data).enter().append("div").attr("class", function(d,i){return "chart2 col-xs-4 col-md-4 col-sm-4";})
					.append("text").text(function(d) { return "Ramp "+d.id; }).style("color","#D5D8DB");
				
				divs.append("div")//.attr("class", "chart2")
						.style("width", function(d) { return barscale(d.density) + "%"; })
//						.style("opacity",function(d) { return opacityscale(d.density);})
						.text(function(d) { return d.density + "%"; })
						.style("background-color", "#F68275")//function(d) { return colorscale(d.density); });
				divs.append("div")//.attr("class", "chart2")
						.style("width", function(d) { return barscale(d.rate) + "%"; })
//						.style("opacity",function(d) { return opacityscale(d.rate);})
						.text(function(d) { return d.rate + "%"; })
						.style("background-color", "#1C9CA7");//function(d) { return colorscale(d.rate); });
				
				var legend = container.append("div").attr("class", "chart2 col-xs-4 col-md-4 col-sm-4")
					.style("background-color","white")
					.append("text").text("Legend").style("color","#444C57");//.attr("class", "chart2")
						
				legend.append("div").style("width", function(d) { return barscale(100) + "%"; })
//						.style("opacity",function(d) { return opacityscale(d.density);})
						.text(function(d) { return "density%"; })
						.style("background-color", "#F68275")
						
				legend.append("div").style("width", function(d) { return barscale(100) + "%"; })
//						.style("opacity",function(d) { return opacityscale(d.density);})
						.text(function(d) { return "rate%"; })
						.style("background-color", "#1C9CA7")
			}
			drawRamps();
		}  
    };
});