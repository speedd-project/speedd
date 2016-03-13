
var area;
function randomInt(min, max) // function that generates a random int between min and max
{
    return Math.floor(Math.random() * (max - min + 1) + min);
}


//// 	http://leebyron.com/streamgraph/

function drawAccountHistory(){
	
	d3.select("#accountHistory").select("svg").remove();
	
	var n = 9, // number of layers
		m = 200, // number of samples per layer
		stack = d3.layout.stack().offset("wiggle"),
		layers0 = stack(d3.range(n).map(function() { return bumpLayer(m); })),
		layers1 = stack(d3.range(n).map(function() { return bumpLayer(m); }));
	
	var w = document.getElementById("accountHistory").clientWidth || document.body.clientWidth;
	var h = document.getElementById("accountHistory").clientHeight || document.body.clientHeight;
	
	var width = w-40;
	var height = h-20;
	
	var x = d3.scale.linear()
		.domain([0, m - 1])
		.range([0, width]);
	
	var y = d3.scale.linear()
		.domain([0, d3.max(layers0.concat(layers1), function(layer) { return d3.max(layer, function(d) { return d.y0 + d.y; }); })])
		.range([height, 0]);
	
	var color = d3.scale.category20c();/*d3.scale.linear()
		.range(["#aad", "#556"]);*/
	
	area = d3.svg.area()
		.x(function(d) { return x(d.x); })
		.y0(function(d) { return y(d.y0); })
		.y1(function(d) { return y(d.y0 + d.y); });
	
	var svg = d3.select("#accountHistory").append("svg")
		.attr("width", width)
		.attr("height", height);
	
	svg.selectAll("path")
		.data(layers0)
	.enter().append("path")
		.attr("d", area)
		.style("fill", function() { return color(Math.random()); });
	
	function transition() {
	d3.selectAll("path")
		.data(function() {
			var d = layers1;
			layers1 = layers0;
			return layers0 = d;
		})
		.transition()
		.duration(2500)
		.attr("d", area);
	}
	
	// Inspired by Lee Byron's test data generator.
	function bumpLayer(n) {
	
		function bump(a) {
			var x = 1 / (.1 + Math.random()),
				y = 2 * Math.random() - .5,
				z = 10 / (.1 + Math.random());
			for (var i = 0; i < n; i++) {
                var w = (i / n - y) * z;
                a[i] += x * Math.exp(-w * w);
			}
		}
		
		var a = [], i;
		for (i = 0; i < n; ++i) a[i] = 0;
		for (i = 0; i < 5; ++i) bump(a);
		
		return a.map(function(d, i) { return {x: i, y: Math.max(0, d)}; });
	}
	
	// add a 'hover' line that we'll show as a user moves their mouse (or finger)
	// so we can use it to show detailed values of each line
	function drawCursor(){
		var mouseX = randomInt(0,width);
		
		d3.select("#accountHistory").select("svg").selectAll("line").remove();
		
		d3.select("#accountHistory").select("svg").append("g").append("line").attr("id","curs")
			.attr("x1", mouseX)  //<<== change your code here
			.attr("y1", 0)
			.attr("x2", mouseX)  //<<== and here
			.attr("y2", height)
			.style("stroke-width", 2)
			.style("stroke", "grey")
			.style("fill", "none");
	}
	
	drawCursor();
	
	var handleMouseOverGraph = function(mouseX) {	
		
		d3.select("#accountHistory").select("svg").select("#curs")
			.attr("x1", mouseX)  
			.attr("x2", mouseX);

	}
	
	d3.select("#accountHistory").on("mouseout", function() {
			var coordinates = [0, 0];
			coordinates = d3.mouse(this);
			var x = coordinates[0];
			var y = coordinates[1];
			handleMouseOverGraph(x);
	});
	
	d3.select("#accountHistory").on("mouseover", function(event) {
			var coordinates = [0, 0];
			coordinates = d3.mouse(this);
			var x = coordinates[0];
			var y = coordinates[1];
			handleMouseOverGraph(x);
	});
	
	d3.select("#accountHistory").on("click", function(event) {
		// code that reloads 3D treemap -- 16-10-15
		 filename = f[randomInt(0,6)];    
         reload(); // function in -- public/js/3d_treemap.js
	});

    
}

setTimeout(function(){
    drawAccountHistory();
}, 100);


function redrawAccountHistory() {
	var n = 9, // number of layers
		m = 200, // number of samples per layer
		stack = d3.layout.stack().offset("wiggle"),
		layers0 = stack(d3.range(n).map(function() { return bumpLayer(m); })),
		layers1 = stack(d3.range(n).map(function() { return bumpLayer(m); }));
		
	d3.select("#accountHistory").selectAll("path")
		.data(function() {
			var d = layers1;
			layers1 = layers0;
			return layers0 = d;
		})
		.transition()
		.duration(2500)
		.attr("d", area);
		
	function bumpLayer(n) {

        function bump(a) {
            var x = 1 / (.1 + Math.random()),
                y = 2 * Math.random() - .5,
                z = 10 / (.1 + Math.random());
                
            for (var i = 0; i < n; i++) 
            {
                var w = (i / n - y) * z;
                a[i] += x * Math.exp(-w * w);
            }
        }
        
        var a = [], i;
        for (i = 0; i < n; ++i) a[i] = 0;
        for (i = 0; i < 5; ++i) bump(a);
        
        return a.map(function(d, i) { return {x: i, y: Math.max(0, d)}; });
	}
}