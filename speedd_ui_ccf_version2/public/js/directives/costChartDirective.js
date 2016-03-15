app.directive('costChart', function ($parse) {
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
               data=newVal;
               drawChart();
			});
			
            data = 30;
            
                    
			          
            function drawChart(){
                nv.addGraph(function() {  
                    var chart = nv.models.bulletChart();

                    chartCont
                        .datum(exampleData())
                        .transition().duration(1000)
                        .call(chart);
                   // console.log(chartCont.tooltip.contentGenerator);

                    return chart;
                });


                function exampleData() {
                    randomInt = function (min, max) // function that generates a random int between min and max
                    {
                        return Math.floor(Math.random() * (max - min + 1) + min);
                    } 
                    var max = randomInt(200,500);
                    var mean = randomInt(0,max);
                    var min = randomInt(0,mean);
                    
                    
                    return {
                        "title":"Cost",		//Label the bullet chart
                        "subtitle":"EUR €",		//sub-label for bullet chart
                        "ranges":[min,mean,(data > max)? data+50:max ],	 //Minimum, mean and maximum values.
                        "measures":[data],		 //Value representing current measurement (the thick blue line in the example)
                        "markers":[150]			 //Place a marker on the chart (the white triangle marker)
                    };
                }
            }
            
            drawChart();
            
		}  
    };
});