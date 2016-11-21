app.directive('mapsDirective', function ($parse) { // inspired from http://prag.ma/code/d3-cartogram/#popest/2010

	return{
        restrict: 'EA',
		replace: false,
		link: function (scope, element, attrs) {
           
           var mapcont = d3.select(element[0]);
		   console.log(element[0])
		   
		    var exp = $parse(attrs.mapData);
			var datax = exp(scope);
			
			var exp2 = $parse(attrs.inTerms);
			var inTermsOf = exp2(scope);
            
            var exp3 = $parse(attrs.countries);
			var countries = exp3(scope);
		   
		   // listens for WINDOW RESIZE
			scope.$watch(function(){
//				resize();
//                map.resize();     // if uncommented resizes map to window but creates gliches because of zoom
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
            scope.$watchCollection(exp3, function(newVal, oldVal){	// don't know what happens to scope if more controllers are added to the same directive (DOM elem)
				countries=newVal;
			});
		   
		   
		   // get container size
			var margin = { top: -10, right: 10, bottom: -10, left: 10 }
			, width = parseInt(element[0].clientWidth) - margin.left - margin.right
			, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;

			var color = d3.scale.category10();
			


				
			// resizes the svg 
			function resize() {
				var margin = { top: -10, right: 10, bottom: -10, left: 10 }
				, width = parseInt(element[0].clientWidth) - margin.left - margin.right
				, height = parseInt(element[0].clientHeight) - margin.top - margin.bottom;
			
				svg.attr("width", width)
				   .attr("height", height);
			}	
				

            var path;
			
			var map = new Datamap({
				element: element[0],
				projection: 'mercator',
				responsive: true,
 /*               setProjection: function(element) {
                    var projection = d3.geo.mercator()
                        .scale(150)
                        .translate([600, 480]);
                
                    path = d3.geo.path()
                        .projection(projection);

                    return {path: path, projection: projection};
                },*/
				done: function(datamap) {
					datamap.svg.selectAll('.datamaps-subunit').on('click', function(geography) {
                        console.log(geography.properties.name);
                        
                        if (geography.properties.name != "United States of America"){
                            console.log(datax.get(geography.properties.name));
 //                       console.log("CENTRE"+path.centroid(geography)[1]);    // gets centroid of the country shape
                            console.log(datax.get(geography.properties.name).latlng);
                            return scope.onCountryClick(datax.get(geography.properties.name).cca2); 
                        }
                        else {
                            console.log(datax.get("United States"));
                            console.log(datax.get("United States").latlng);
                            return scope.onCountryClick(datax.get("United States").cca2); 
                        }
						
					});
				}
                
			});
            
            
            
            function drawArc(countries){
               
               var latlngA = datax.get(countries[0]).latlng;
               var latlngB = datax.get(countries[1]).latlng;
               
               map.arc([{
                        origin: {
                            latitude: latlngA[0],
                            longitude: latlngA[1]
                        },
                        destination: {
                            latitude: latlngB[0],
                            longitude: latlngB[1]
                        }
                    }], 
                    {
                        greatArc: true,
                        animationSpeed: 2000
                    }
                );
            }
            
            function removeArcs(){
               map.arc([]);
            }
            
  //          map.labels({'customLabelText': newLabels});
                            // #carto
            var mapElems = d3.selectAll(".datamap").call(d3.behavior.zoom()
				.on("zoom", redraw))
				.selectAll("g"); // makes sure both countries and labels are moved
		  
            // redraws map on zoom and move
			function redraw() {
				mapElems.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
			}

			console.log(map.svg[0])

			function update() {

				switch(inTermsOf) {
					
					case "transactions" :   map.resize();
                                            console.log(countries)
											drawArc(countries);
                                            
                                            carto.value(function (d) {
												return datax.get(d.id)? Math.random() * 100:1//datax.get(d.id).transactions:1;//Math.random() * 100:1;
											});
											break;
											
					case "flagged":			carto.value(function (d) {
												return datax.get(d.id)? datax.get(d.id).financial.flagged[datax.get(d.id).financial.flagged.length-1]:1;//datax.get(d.id).flagged:1;//Math.random() * 100:1;
											});
											
											break;
											
					case "amount":			carto.value(function (d) {
												return datax.get(d.id)? datax.get(d.id).financial.amount[datax.get(d.id).financial.amount.length-1]:1;//datax.get(d.id).amount:1;//Math.random() * 100:1;
											});
											
											break;
											
					case "volume":			carto.value(function (d) {
												return datax.get(d.id)? datax.get(d.id).financial.volume[datax.get(d.id).financial.volume.length-1]:1;//datax.get(d.id).volume:1;
											});
											
											break;
											
					default:				// code that removes choropleth patterns and arcs
                                            
                                            removeArcs(); // removes all arcs
                                            
											break;
				}

				
			}
				     
		}  
    };
});