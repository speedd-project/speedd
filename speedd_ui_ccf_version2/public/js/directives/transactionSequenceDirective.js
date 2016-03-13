app.directive('transactionSequenceDirective', function ($parse) {
	return{
         restrict: 'EA',
         replace: false,
         link: function (scope, element, attrs) {
            var chartCont = d3.select(element[0]);

		    var exp = $parse(attrs.data);
			var data = exp(scope);

			// listens for WINDOW RESIZE
			scope.$watch(function(){
			//	drawChart();
			})
			
			scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               dat=newVal;
               console.log(dat)
               drawChart();
			});
			
            chartCont.append("defs").append("marker") // from
                .attr("id", "arrowhead")
                .attr("refX", 6 + 3) 
                .attr("refY", 2)
                .attr("markerWidth", 6)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("path")
                    .attr("d", "M 0,0 V 4 L6,2 Z"); //this is actual shape for arrowhead
            
            var dat = {ts: [
                    2224699200000,
                    2224699203000,
                    2224699209000
                    ],
                    id: [
                    "9d47b44380024e82aedcf359f8be8dfb",
                    "8727070543664142bc7d2ba980dc1f19",
                    "b1dedbeb44ad431f995076153c7bdcc8"
                    ]};      
			          
            function drawChart(){
                // get container size
				var margin = { top: -10, right: 10, bottom: -10, left: 10 }
				, width = parseInt(element[0].clientWidth) - margin.left - margin.right
				, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;
                
                var spacing = width/dat.ts.length;
                var dataLength = dat.ts.length;
                
                               
                chartCont.select("#drawing").remove();
 //               d3.select("#trans-seq-lbl").remove();
                var drawing = chartCont.append("g").attr("id", "drawing");
  /*              
                var label = d3.select("#trans-seq").append("label").attr('id','trans-seq-lbl')
                    .text("Transaction Sequence")
                    .attr("x",15)
                    .attr("y", height/2)
                    .attr("font-family", "sans-serif")
                    .attr("font-size", "20px")
                    .attr("fill", "#999999");
  */              
                var circles = drawing
                    .selectAll("circle")
                    .data(dat.ts)
                        .enter()
                    .append('circle')
                    .attr("cx", function(d,i){ return width-(spacing*(dataLength-(i)))+spacing/2})
                    .attr("cy", height/2)
                    .attr("r", 6)
                    .attr("class", "transactionDot");
                    
                circles.append('title').text(function(d,i){ return "date: "+new Date(d)+
                                                                 "\ntransaction ID: "+ dat.id[i]})
                
                var circleLabels = drawing//.append("g")
                    .selectAll("text")
                    .data(dat.ts)
                        .enter()
                    .append("text")
                    .text(function(d,i){ return (dat.ts[i+1])? parseFloat((dat.ts[i+1]-dat.ts[i])/1000).toFixed(2)+" sec":""})
                    .attr("x",function(d,i){ return width-(spacing*(dataLength-(i)))+spacing-50})
                    .attr("y", height/2 - 15)
                    .attr("font-family", "sans-serif")
                    .attr("font-size", "20px")
                    .attr("fill", "#999999");
                    
                    
                var xs = [];    
                for (var i = 0; i < circles[0].length; i++)    
                {
                    xs.push(d3.select(circles[0][i]).attr("cx"));
                }
                
                var lines = drawing
                    .selectAll('line')
                    .data(dat.ts)
                        .enter()
                    .append('line')
                    .attr("x1", function(d,i) { return xs[i]})
                    .attr("x2", function(d,i) { return xs[i]})// return (xs[i+1])? xs[i+1]: xs[i]}
                    .attr("y1", height/2)
                    .attr("y2", height/2)
                    .attr("class", "transactionLine")
                    .attr("marker-end", "url(#arrowhead)")
                        .transition()
                    .duration(1000)
                    .attr({
                        x2: function(d,i) { return (xs[i+1])? xs[i+1]: xs[i]},
                        y2: height/2
                    });
                
                
            }
            
            drawChart();
            
		}  
    };
});