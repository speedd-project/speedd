app.controller('CircularMapController', ['$scope','$interval','$window','dataService','$modal','$log', function($scope, $interval,$window,dataService,$modal,$log){
    
	//////////////////////// ADD BROADCAST USER EVENT to show on list
	$scope.barScale = d3.scale.linear()
        .domain([0,100])
        .range([0,38]);
 
        
    $scope.segColourScale = d3.scale.linear()
        .domain([0,50,100])
        .range(["green","yellow","red"]);
        
    $scope.segClass = d3.scale.ordinal()
        .domain(["normal","medium","congestion"])
        .range(["st4","st5","st2"]);
    
    $scope.barClass = d3.scale.ordinal()
        .domain(["rnorm","rprob","onorm","oprob","snorm","sprob","rother","oother","sother"])
        .range(["st23","st29","st21","st28","st19","st27","st26","st25","st24"]);
    
    $scope.nodeClass = d3.scale.ordinal()
        .domain(["normal","medium","congestion"])
        .range(["st35","st5","st2"]);


	$scope.isCollapsed = false;
	
	$scope.eventList = [];
	$scope.listSelection=[];
	   
       
    // plays all pre-existing events   
    $scope.$on("broadcastRawEventList", function(){
		var eventList = dataService.rawEventList;
		console.log(eventList);
		setTimeout(function (){
		for (var j = 0 ; j < eventList.length ; j++)
		{
			$scope.parseEvent(eventList[j]);
		}
		}, 2000);
	});   
 
  
    $scope.parseEvent = function(event){
        if (event.name == "Congestion"){
			$scope.displayCongestion(event,true);
            
            console.log(event);
        }
        else if(event.name == "PredictedCongestion"){
             $scope.displayPredictedCongestion(event);
             
             console.log(event);
        }
		else if(event.name == "ClearCongestion"){
            $scope.clearPredictedCongestion(event);
			$scope.clearCongestion(event,true);
            
            console.log(event);
        }
        else if (event.name == "UpdateMeteringRateAction"){
			// update current density and rate for that ramp
	        var node = dataService.locationToNode(event.attributes.location);
            
            $scope.currentRate(node, event.attributes.newMeteringRate);
            $scope.currentOccupancy(node, event.attributes.density);
            
            console.log(event);
		}
        else {
            var node = dataService.locationToNode(event.attributes.location);
        
            $scope.currentSpeed(node, event.attributes.average_speed);
            $scope.currentOccupancy(node, event.attributes.average_density);
            
            console.log(event);
        }
    }     
	
	$scope.$on("broadcastMapEvent", function(){
		var event = dataService.currentMapEvent;
		
        $scope.parseEvent(event);
	});
	$scope.$on("broadcastRampEvent", function(){
		var event = dataService.currentRampEvent;
		
        $scope.parseEvent(event);
	});
	
	$scope.$on("broadcastMainRoadEvent", function(){
		var event = dataService.currentMainRoadEvent;
		
        $scope.parseEvent(event);
	});

    console.log("WOOOOORKS")
     
     
    $scope.colourRoadSegments = function (node, percentage){
        
        var state;
        
        if(percentage<45)
            state = "normal";
        else if(percentage<75)
            state = "medium";
        else    
            state = "congestion";
        
        // get segments that are defined by node
        var segments = dataService.segmentsWithNode(node);
        // make segments red
        segments.forEach(function(segment){
            d3.select(circularMap).select('#'+segment).attr("class", $scope.segClass(state));
        });
    }

    $scope.displayCongestion = function (event,complex){
        var node = dataService.locationToNode(event.attributes.location);
        
        // make node red
        d3.select(circularMap).select('#'+node).attr("class", $scope.nodeClass("congestion"));
        
        // get segments that are defined by node
        var segments = dataService.segmentsWithNode(node);
        // make segments red
        segments.forEach(function(segment){
            d3.select(circularMap).select('#'+segment).attr("class", $scope.segClass("congestion"));
            
            if(complex == true){
               var nodesOfSegment = dataService.nodesOfSegment(segment);
            
                nodesOfSegment.forEach(function(n){
                    if (n != node){
                        if (d3.select(circularMap).select('#'+n).attr("class") != $scope.nodeClass("congestion"))
                        {
                            d3.select(circularMap).select('#'+n).attr("class", $scope.nodeClass("medium"));
                        
                            var seg = dataService.segmentsWithNode(n);
                            seg.forEach(function(s){
                                if(s != segments[0] && s != segments[1])
                                        d3.select(circularMap).select('#'+s).attr("class", $scope.segClass("medium"));
                            });
                        }
                            
                    }
                }); 
            }
            
            
        });
        
        // update current density values
        $scope.currentOccupancy(node, event.attributes.average_density);
               
        // attract attention to current bars
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
        
        d3.select(circularMap).select('#cr'+barNo).attr("class", $scope.barClass("rprob"));
        d3.select(circularMap).select('#co'+barNo).attr("class", $scope.barClass("oprob"));
        d3.select(circularMap).select('#cs'+barNo).attr("class", $scope.barClass("sprob"));
        
    }
    
    $scope.displayPredictedCongestion = function (event){
        var node = dataService.locationToNode(event.attributes.location);
        
        // attract attention to current bars
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
                
        d3.select(circularMap).select('#cr'+barNo).attr("class", $scope.barClass("rprob"));
        d3.select(circularMap).select('#co'+barNo).attr("class", $scope.barClass("oprob"));
        d3.select(circularMap).select('#cs'+barNo).attr("class", $scope.barClass("sprob"));
        
        
        // have to write this code because bar po4 is rotated 180 deg
        if(barNo != "4"){
            d3.select(circularMap).select('#po'+barNo).attr("height", $scope.barScale(100));
            d3.select(circularMap).select('#ps'+barNo).attr("height", $scope.barScale(15));  
        }
     /*   
        else {
            if( parseFloat(d3.select(circularMap).select('#po'+barNo).attr("height")) > $scope.barScale(100))
            {
                var difference = parseFloat(d3.select(circularMap).select('#po'+barNo).attr("height")) - parseFloat($scope.barScale(100));
                var currentY = parseFloat(d3.select(circularMap).select('#po'+barNo).attr("y"));
                
                d3.select(circularMap).select('#po'+barNo).attr("height", $scope.barScale(100));
                d3.select(circularMap).select('#po'+barNo).attr("y", currentY-difference);
            }
            else
            {
                var difference = parseFloat($scope.barScale(100)) - parseFloat(d3.select(circularMap).select('#po'+barNo).attr("height"));
                var currentY = parseFloat(d3.select(circularMap).select('#po'+barNo).attr("y"));
                
                d3.select(circularMap).select('#po'+barNo).attr("height", $scope.barScale(100));
                d3.select(circularMap).select('#po'+barNo).attr("y", currentY+difference);
            }
            d3.select(circularMap).select('#ps'+barNo).attr("height", $scope.barScale(15));  
        }
       */ 
        
    }
    
    $scope.clearPredictedCongestion = function (event){
        var node = dataService.locationToNode(event.attributes.location);
        
        // remove attention from current bars
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
        
        d3.select(circularMap).select('#cr'+barNo).attr("class", $scope.barClass("rnorm"));
        d3.select(circularMap).select('#co'+barNo).attr("class", $scope.barClass("onorm"));
        d3.select(circularMap).select('#cs'+barNo).attr("class", $scope.barClass("snorm"));
        
        // have to write this code because bar po4 is rotated 180 deg
        if(barNo != "4"){
            d3.select(circularMap).select('#po'+barNo).attr("height", $scope.barScale(100));
            d3.select(circularMap).select('#ps'+barNo).attr("height", $scope.barScale(70));  
        }
   /*     
        else {
            if( parseFloat(d3.select(circularMap).select('#po'+barNo).attr("height")) > $scope.barScale(15))
            {
                var difference = parseFloat(d3.select(circularMap).select('#po'+barNo).attr("height")) - $scope.barScale(100);
                var currentY = parseFloat(d3.select(circularMap).select('#po'+barNo).attr("y"));
                
                d3.select(circularMap).select('#po'+barNo).attr("height", $scope.barScale(15));
                d3.select(circularMap).select('#po'+barNo).attr("y", currentY-difference);
            }
            else
            {
                var difference = parseFloat($scope.barScale(100)) - parseFloat(d3.select(circularMap).select('#po'+barNo).attr("height"));
                var currentY = parseFloat(d3.select(circularMap).select('#po'+barNo).attr("y"));
                
                d3.select(circularMap).select('#po'+barNo).attr("height", $scope.barScale(15));
                d3.select(circularMap).select('#po'+barNo).attr("y", currentY+difference);
            }
            d3.select(circularMap).select('#ps'+barNo).attr("height", $scope.barScale(70));  
        }
    */   
    }
    
    $scope.clearCongestion = function (event, complex){
        var node = dataService.locationToNode(event.attributes.location);
        
        // make node normal
        d3.select(circularMap).select('#'+node).attr("class", $scope.nodeClass("normal"));
        
         // get segments that are defined by node
        var segments = dataService.segmentsWithNode(node);
        // make segments normal
        segments.forEach(function(segment){
            d3.select(circularMap).select('#'+segment).attr("class", $scope.segClass("normal"));
            
            if(complex == true){
               var nodesOfSegment = dataService.nodesOfSegment(segment);
            
                nodesOfSegment.forEach(function(n){
                    if (n != node){
                        d3.select(circularMap).select('#'+n).attr("class", $scope.nodeClass("normal"));
                        
                        var seg = dataService.segmentsWithNode(n);
                        seg.forEach(function(s){
                            if(s != segments[0] && s != segments[1])
                                    d3.select(circularMap).select('#'+s).attr("class", $scope.segClass("normal"));
                        });
                    }
                }); 
            }
        });
        
        // update current density values
        if (event.attributes.average_density != null)
            $scope.currentOccupancy(node, event.attributes.average_density);
        else
            $scope.currentOccupancy(node, 15);
        
        // remove attention to current bars
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
        
        d3.select(circularMap).select('#cr'+barNo).attr("class", $scope.barClass("rnorm"));
        d3.select(circularMap).select('#co'+barNo).attr("class", $scope.barClass("onorm"));
        d3.select(circularMap).select('#cs'+barNo).attr("class", $scope.barClass("snorm"));
              
    }

    $scope.clearAll = function (){
        // make nodes normal
        dataService.nodes.forEach(function(n){
            var node = n.id;
            d3.select(circularMap).select('#'+node).attr("class", $scope.nodeClass("normal"));
            
             // get segments that are defined by node
            var segments = dataService.segmentsWithNode(node);
            // make segments normal
            segments.forEach(function(segment){
                d3.select(circularMap).select('#'+segment).attr("class", $scope.segClass("normal"));       
            });
            
            
            // remove attention to current bars
            var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
            // historical
            d3.select(circularMap).select('#hr'+barNo).attr("class", $scope.barClass("rother"));
            d3.select(circularMap).select('#ho'+barNo).attr("class", $scope.barClass("oother"));
            d3.select(circularMap).select('#hs'+barNo).attr("class", $scope.barClass("sother"));
            // predicted
            d3.select(circularMap).select('#pr'+barNo).attr("class", $scope.barClass("rother"));
            d3.select(circularMap).select('#po'+barNo).attr("class", $scope.barClass("oother"));
            d3.select(circularMap).select('#ps'+barNo).attr("class", $scope.barClass("sother"));
            // current
            d3.select(circularMap).select('#cr'+barNo).attr("class", $scope.barClass("rnorm"));
            d3.select(circularMap).select('#co'+barNo).attr("class", $scope.barClass("onorm"));
            d3.select(circularMap).select('#cs'+barNo).attr("class", $scope.barClass("snorm"));
        })      
    }
    
    $scope.currentOccupancy = function (node,percentage){     
        // modify current occupancy bar
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
        
        // change bar height
        d3.select(circularMap).select('#co'+barNo).attr("height", $scope.barScale(percentage))
        
        // colour road segments
//        $scope.colourRoadSegments(node, percentage);   
    }
    
    $scope.currentSpeed = function (node, percentage){     
        // modify current occupancy bar
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
       
        // change bar height
        d3.select(circularMap).select('#cs'+barNo).attr("height", function(){
            if (percentage<=100) 
                return $scope.barScale(percentage); 
            else 
                return $scope.barScale(100);
        });    
    }
    
    $scope.currentRate = function (node, percentage){     
        // modify current occupancy bar
        var barNo = node.slice(-2);
            if(barNo[0] == "e")
                barNo = node.slice(-1);
       
        // change bar height
        d3.select(circularMap).select('#cr'+barNo).attr("height", $scope.barScale(percentage))    
    }
    
    $scope.appendCam = function (){
        // appends cam icon to circular map
//        var node = dataService.nodes[dataService.randomInt(0,dataService.nodes.length)];
        var node = dataService.nodes[9] // "node8"
        
        d3.select(circularMap).select("svg").append("svg:image").attr("id","cam")
        .attr('x', node.camX)
        .attr('y', node.camY)
        .attr('width', 25)
        .attr('height', 30)
        .attr("xlink:href","vidCam.png");
    }

    setTimeout(function(){
        $scope.clearAll();
        // appends cam icon to circular map
        $scope.appendCam();
    },1000)
}]);