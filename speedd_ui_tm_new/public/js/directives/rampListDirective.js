app.directive('rampList', function($parse, $window){
   return{
      restrict:'EA',
//      template:"<svg id='svg-chart' width='850' height='200'></svg>", // can either use template or (d3.select && d3.append.attr)
				 //angscope, dom, data bound in html
      link: function(scope, elem, attrs){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)

           var exp = $parse(attrs.rampListData);
		
					// exp(scope) 		is equivalent to 		scope.main.salesData ------ however the controller needs to be imported into the HTML as main
           var rampListToPlot=exp(scope); // don't know what happens to scope if more controllers are added to the same directive (DOM elem)
		   
           var padding = 20;
           var pathClass="path";
           var xScale, yScale, xAxisGen, yAxisGen, lineFun;

           var d3 = $window.d3; // adds d3 library in the context of the directive
		   

			// listens for WINDOW RESIZE
		   scope.$watch(function(){
			console.log("resize");
			drawRamps();
			})
			
 //          var rawSvg=elem.find('svg');		// don't actually need this - duplicates the function of d3.select("#id")
           var svg = d3.select("#svg-ramp");
	
			// $watchCollection executes on data change 			
		scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               rampListToPlot=newVal;
               drawRamps();
           });

		var colorscale = d3.scale.linear()
					.domain([0, 100])
					.range(["#444C57", "#FFF7E8"]);
					
		   function drawRamps()
		   { ////// d3 stuff
		   
		   d3.select(elem[0]).select("#svg-ramp").remove();
		 
			// get container size
			var margin = { top: -10, right: 10, bottom: -10, left: 10 }
			, width = parseInt(elem[0].clientWidth) - margin.left - margin.right
			, height = parseInt(elem[0].clientHeight) - margin.top - margin.bottom;
  
			// set SVG size
			var s = d3.select(elem[0]).append('svg').attr('id','svg-ramp')
				.attr('width', width + margin.right + margin.left)
				.attr('height', height + margin.top + margin.bottom);
			var svg = d3.select("#svg-ramp");	
//				svg.style("background-color","#64DE89");
				
				var g = svg.append("g");                

				var squareWidth = parseInt(width / 5);
				var squareXDist = parseInt((squareWidth - 30) / 2);
				var squareHeight = parseInt(height / 4);
				var squareYDist =  parseInt((squareHeight - 30) / 2);
				
				var drawing = g.selectAll("rect").data(rampListToPlot).enter()
						.append("rect")
							.attr("x", function (d, i) { return (squareXDist + ((30 + squareXDist * 2) * (i % 5))) })
							.attr("y", function (d, i) { return (squareYDist + ((30 + squareYDist * 2) * Math.floor(i / 5))) })
							.attr("width", 30)
							.attr("height", 30)
							.style("fill", function (d, i) { return colorscale(d.rate); })
				/// NEWWW -- gives rect an ID to be able to select and modify    
				.attr("id", function (d, i) { return "rectRamp" + i })
				.on("mouseover", function (d) { d3.select(this).style("cursor", "pointer"); return d3.select(this).style("fill-opacity", "0.7").style("fill", "white") })
				.on("mouseout", function (d) { return d3.select(this).style("fill-opacity", "1").style("fill", function (d, i) { return colorscale(d.rate)/*colorBasedOnRate(d.status);*/ }) })
				.on("click",function (d) { return scope.onClick({item: d});}); // forwards event to the controller -- sends the actual data point
		
				// text on mouse hover
				drawing.append("title").text(function (d) { return d.rate });//statusScale(d.status)});

				var labels = g.selectAll("text").data(rampListToPlot).enter()
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