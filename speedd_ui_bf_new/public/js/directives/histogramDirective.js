app.directive('histogramDirective', function($parse, $window){
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
/*			
			// $watchCollection executes on data change 			
			scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
               dataToPlot=newVal;
//			   addPointToDataset();
//			   console.log("CHANGED");
			});
	*/
			// append a div only once
			d3.select(elem[0]).append('div').attr("id","histogram");

//			console.log(dataToPlot);
			
			var container = document.getElementById('histogram');
			var items = [
				{x: '2014-06-11', y: 10},
				{x: '2014-06-12', y: 25},
				{x: '2014-06-13', y: 30},
				{x: '2014-06-14', y: 10},
				{x: '2014-06-15', y: 15},
				{x: '2014-06-16', y: 30}
			];

			var dataset = new vis.DataSet(items);
			var options = {
				style:'bar',
//				width:  '100%',
//				height: '300px',
				barChart: {width:50, align:'center'}, // align: left, center, right
				drawPoints: false,
				dataAxis: {
//					icons:true,
					visible:true
				},
				orientation:'top',
				start: '2014-06-10',
				end: '2014-06-18'
			};
			var graph2d = new vis.Graph2d(container, items, options);

		}	
    };
});