app.directive('cartogramDirective', function ($parse) { // inspired from http://prag.ma/code/d3-cartogram/#popest/2010

	return{
        restrict: 'EA',
		replace: false,
		link: function (scope, element, attrs) {
           
           var mapcont = d3.select(element[0]);
		   
		    var exp = $parse(attrs.mapData);
			var datax = exp(scope);
			
			var exp2 = $parse(attrs.inTerms);
			var inTermsOf = exp2(scope);
		   
		   
		   // listens for WINDOW RESIZE
			scope.$watch(function(){
				resize();
			})
			
			// $watchCollection executes on data change 			
			scope.$watchCollection(exp, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
				datax=newVal;
			});
			scope.$watchCollection(exp2, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
				inTermsOf=newVal;
				console.log(inTermsOf);
			   
				// makes sure the "inTermsOf" variable gets here
				setTimeout(function(){update();},100);
			});
		   
		   
		   // get container size
			var margin = { top: -10, right: 10, bottom: -10, left: 10 }
			, width = parseInt(element[0].clientWidth) - margin.left - margin.right
			, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;

			var color = d3.scale.category10();
			
			var projection = d3.geo.mercator()
							.translate([560, 390])
							.scale(150);
							

			var path = d3.geo.path()
				.projection(projection);

			// append svg to map container
			var svg = mapcont.append("svg").attr("id","map")
				.attr("width", width)
				.attr("height", height);
				
			
			// adds zoom behaviour
			var map = d3.select("#map").call(d3.behavior.zoom()
				.on("zoom", redraw))
				.append("g");

			// redraws map on zoom and move
			function redraw() {
				map.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
			}

				
			// resizes the svg 
			function resize() {
				var margin = { top: -10, right: 10, bottom: -10, left: 10 }
				, width = parseInt(element[0].clientWidth) - margin.left - margin.right
				, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;
			
				svg.attr("width", width)
				   .attr("height", height);
			}	
				
			var countries = map.append("g")
				.attr("id", "countries")
				.selectAll("path");

			var proj = d3.geo.mercator()
				.scale(150)
				.translate([600, 400]);

			var topology,
				geometries,
				carto_features;
				
			var carto = d3.cartogram()
				.projection(proj)
	            .properties(function (d) {
					// this add the "properties" properties to the geometries
//	                console.log(d);
	                return d.properties;
	            });

			 // this loads the topojson file using d3.json and creates the world map.
			d3.json("data/worldcountriestopo.json", function (data) {
				topology = data;
				geometries = topology.objects.countries.geometries;

				//these 2 below create the map and are based on the topojson implementation
				var features = carto.features(topology, geometries),
					path = d3.geo.path()
						.projection(proj);

				countries = countries.data(features)
					.enter()
						.append("path")
						.attr("class", "country")
						.attr("fill", function (e) {
							return "#4D97B1"
						})
					.attr("d", path)
					.on("click", function (d,i){ //console.log(d);/*do_update();*/ console.log(features[i].id); 
									/*update();*/ 
									// recolours all countries to default
									countries.style("fill","#4D97B1"); 
									// colours the selected country
									d3.select(this).style("fill","#4ECDC4"); 
									// sends event to controller
									return scope.onCountryClick(d.id); 
								});
				// displays country name on hover
				countries.append("title").text(function(d){return d.id;});
				
			});

			function update() {

				switch(inTermsOf) {
					
					case "transactions" :   carto.value(function (d) {
												return datax.get(d.id)? Math.random() * 100:1//datax.get(d.id).transactions:1;//Math.random() * 100:1;
											});
											
											break;
											
					case "flagged":			carto.value(function (d) {
												return datax.get(d.id)? Math.random() * 100:1//datax.get(d.id).flagged:1;//Math.random() * 100:1;
											});
											
											break;
											
					case "amount":			carto.value(function (d) {
												return datax.get(d.id)? Math.random() * 100:1//datax.get(d.id).amount:1;//Math.random() * 100:1;
											});
											
											break;
											
					case "volume":			carto.value(function (d) {
												return datax.get(d.id)? Math.random() * 100:1;//datax.get(d.id).volume:1;
											});
											
											break;
											
					default:				carto.value(function (d) {
												return datax.get(d.id)? datax.get(d.id).area:1;//Math.random() * 100:1;
											});
											
											break;
				}

					 // generate the new features
					var carto_features = carto(topology, geometries).features;

					//update the map data
					countries.data(carto_features).transition()
							.duration(750)
							.ease("linear")
						.attr("d", carto.path);
			}
				     
		}  
    };
});