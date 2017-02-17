app.directive('increasingAmounts', function ($parse) {
	return{
         restrict: 'EA',
         replace: false,
         link: function (scope, element, attrs) {
            var chartCont = d3.select(element[0]);
            
		    var exp = $parse(attrs.data);
			var data = exp(scope);
            
			// listens for WINDOW RESIZE
			scope.$watch(function(){
				drawChart();
			})
			
			scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               data=newVal;
               drawChart();
			});
			
      
            if (data == null)
               data = [40,150,200];
                    
			          
            function drawChart(){
                chartCont.select('#sums').remove();
                
                nv.addGraph(function() {
                    
                    var chart = nv.models.discreteBarChart()
                        .x(function(d) { return d.label })
                        .y(function(d) { return d.value })
                        .staggerLabels(true)
                //        .tooltips(false)
                        .showValues(true)

                    chartCont
                        .datum(exampleData())
                        .transition().duration(500)
                        .call(chart)
                        ;

     //               nv.utils.windowResize(chart.update);

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
                    
                    var vals = [];
                    
                 
                    
                    for(var i=0; i<data.length; i++)
                    {
                        vals.push({ 
                                "label" : "amount "+parseInt(i+1) ,
                                "value" : data[i]
                                });
                    }
                    
                    return [ 
                            {
                            key: "incresing amounts",
                            values: vals
                            }
                        ];
                }
            }
            
            drawChart();
            
		}  
    };
});