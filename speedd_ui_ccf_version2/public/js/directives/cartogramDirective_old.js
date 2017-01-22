app.directive('cartogramDirective', function ($parse) {

	return{
        restrict: 'EA',
		replace: false,
		link: function (scope, element, attrs) {
           
           var map = d3.select(element[0]);
		   
		    var exp = $parse(attrs.chartData);
			var datax = exp(scope);
			console.log(map);
           
			var margin = { top: -10, right: 10, bottom: -10, left: 10 }
			, width = parseInt(element[0].clientWidth) - margin.left - margin.right
			, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;

			var color = d3.scale.category10();

			var projection = d3.geo.mercator()
							.translate([560, 390])
							.scale(150);
							
			var projection2 = d3.geo.albers();

			var path = d3.geo.path()
				.projection(projection);

			var svg = map.append("svg")
				.attr("width", width)
				.attr("height", height)
				.call(d3.behavior.zoom()
				.on("zoom", redraw))
				.append("g");
			

			function redraw() {
				svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
			}

				
			var tooltip = d3.select("#map").append("div")
				.attr("class", "tooltip");

			queue()
				.defer(d3.json, "/data/world-50m.json")
				.defer(d3.tsv, "/data/world-country-names.tsv")
				.await(ready);

			function ready(error, world, names) {

			  var countries = topojson.object(world, world.objects.countries).geometries,
				  neighbors = topojson.neighbors(world, countries),
				  i = -1,
				  n = countries.length;

			  countries.forEach(function(d) { 
				var tryit = names.filter(function(n) { return d.id == n.id; })[0];
				if (typeof tryit === "undefined"){
				  d.name = "Undefined";
				  //console.log(d);
				} else {
				  d.name = tryit.name; 
				}
			  });

			var country = svg.selectAll(".country").data(countries);

			  country
			   .enter()
				.insert("path")
				.attr("class", "country")    
				  .attr("title", function(d,i) { return d.name; })
				  .attr("d", path)
				  .style("fill", function(d, i) { return color(d.color = d3.max(neighbors[i], function(n) { return countries[n].color; }) + 1 | 0); });

				//Show/hide tooltip
				country
				  .on("mousemove", function(d,i) {
					var mouse = d3.mouse(svg.node()).map( function(d) { return parseInt(d); } );

					tooltip
					  .classed("hidden", false)
					  .attr("style", "left:"+(mouse[0]+25)+"px;top:"+mouse[1]+"px")
					  .html(d.name)
				  })
				  .on("mouseout",  function(d,i) {
					tooltip.classed("hidden", true)
				  });

			}
			

           //a little of magic: setting it's width based
           //on the data value (d) 
           //and text all with a smooth transition
		}  
    };
});