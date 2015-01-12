var sensorPos = [];

var imgControl =
{
    url: 'img/control_icon2_small.png',
    // This marker is 15 pixels wide by 15 pixels tall.
    size: new google.maps.Size(15, 15),
    // The origin for this image is 0,0.
    origin: new google.maps.Point(0,0),
    // The anchor for this image is the base of the flagpole at 8,8 (centre).
    anchor: new google.maps.Point(8, 8)
};
var imgCamera =
{
    url: 'img/camera_icon_small.png',
    // This marker is 15 pixels wide by 15 pixels tall.
    size: new google.maps.Size(15, 15),
    // The origin for this image is 0,0.
    origin: new google.maps.Point(0,0),
    // The anchor for this image is the base of the flagpole at 8,8 (centre).
    anchor: new google.maps.Point(8, 8)
};
var imgRamp =
{
    url: 'img/traffic_light_icon.png',
    // This marker is 15 pixels wide by 15 pixels tall.
    size: new google.maps.Size(21, 21),
    // The origin for this image is 0,0.
    origin: new google.maps.Point(0, 0),
    // The anchor for this image is the base of the flagpole at 8,8 (centre).
    anchor: new google.maps.Point(11, 11)
};

var map, pano;
var markerControl = [];
var markerCamera = [];

var markerJson = [];

// lat,lng points where cameras and controllable traffic signs are located
var posControl = { lat: [], lng: [] };// = [[45.18047856164824, 5.716860121726995], [45.18045587414887, 5.716071552276617], [45.18012312311911, 5.704457587242132], [45.158781402700136, 5.704677528381353]];
var posCamera = { lat: [], lng: [] };// = [[45.180467217899675, 5.7166777315139825], [45.180482342897264, 5.716275400161749], [45.18013446693625, 5.7038192214965875], [45.1586073987022, 5.702917999267584], [45.15945471751181, 5.696416324615484]];

var areasOfInterest = [];
var infowindow = new google.maps.InfoWindow({
    content: '<div id="divVideo" style="width: 500px; height: 300px"></div>'
});
var infowindowCongestion = new google.maps.InfoWindow({
    content: '<div id="divCongestion" style="width: 300px; height: 150px"></div>'
});

function initialize() {
    // Create an array of styles.
    var styles = [
      {
          stylers: [
            { hue: "#585858" },
            { saturation: -100 }
          ]
      }, {
          featureType: "poi",
          elementType: "all",
          stylers: [
            { visibility: "off" }
          ]
      }, {
          featureType: "road",
          elementType: "geometry",
          stylers: [
            { lightness: 100 },
            { visibility: "simplified" }
          ]
      }, {
          featureType: "road",
          elementType: "labels",
          stylers: [
            { visibility: "on" }
          ]
      }
    ];

    var styledMap = new google.maps.StyledMapType(styles,
        { name: "Road" });

    var mapOptions =
    {
        center: new google.maps.LatLng(45.1841656, 5.7155425),
        zoom: 13,
        disableDefaultUI: false,
        streetViewControl: false,
        mapTypeControlOptions: {
            mapTypeIds: [google.maps.MapTypeId.ROADMAP, 'map_style']
        }
    };
    map = new google.maps.Map(d3.select("#map-canvas").node(),
        mapOptions);

    //Associate the styled map with the MapTypeId and set it to display.
    map.mapTypes.set('map_style', styledMap);
    map.setMapTypeId('map_style');


    // panorama settings ------------> to be REPLACED by live camera feed

    var title = d3.select("#divCamHead").append("text").attr("id", "titleCam").text("Video Feed").style("font-weight", "bold").style("font-size", "20px").style("color", "black");

    


    // add selection button to map ------------------- button doesnt stay on the map, it refreshes and gets on top of the img if div appended to #map-canvas
    // if appended to body, doesn't scale properly (overlapping)
/*
    var divButton = d3.select("body").append("div")
                    .attr("id", "divMapButton")
                    .style("width", "50px")
                    .style("height", "50px")
                    .style("position", "absolute")
                    .style("top", "7px")
                    .style("right", "29%");
    var svgButton = divButton.append("svg")        
                    .attr("width", 40)
                    .attr("height", 40)
                    .style("position", "absolute")
                    .style("top", "0px")
                    .style("right", "0px");

    var imgButton = svgButton.append("image")
                            .attr("xlink:href", "img/selection.png")
                            .attr("width", 20)
                            .attr("height", 20)
                            .attr("x", 10)
                            .attr("y", 0)
                    .on("mouseover", function () { imgButton.style("cursor", "pointer") })
                    .on("click", addAreaOfInterest)
                    .on("contextmenu", removeAreaOfInterest);
*/

    //   map.controls[google.maps.ControlPosition.TOP_RIGHT].push(divButton);
    /////////////////////////////////////////////////////////////////////////////////
    


    // adding markers and drawing graphs
    addMarkers();

    // initialise dashboard
    // Timeout needed to load data before drawing
    setTimeout(getRamps, 200);
//    drawSuggestions();
    drawControl();
    drawLog();
    // set up event to update graphs on window resize
    d3.select(window).on('resize', updateGraphs);

    // add overlay to draw on map using d3js
    //    addOverlay();

    // draw on map using gmaps api
//    drawCirclesAlert();

    /*    google.maps.event.addListener(map, 'idle', function () {
        scaleOverlay(); console.log(map.getZoom());
    })*/

    // initialise window drag behaviour
    //    initDrag();
}
google.maps.event.addDomListener(window, 'load', initialize);


function moveMapToLocation(lat,lng)
{
    var location = new google.maps.LatLng(lat, lng);

    map.setCenter(location);
    map.setZoom(15);

}


function drawCirclesAlert(lat,lng,name,certainty,problem_id)
{// draw on map using gmaps api
    
    // determine color of circle (from name) and fill opacity (from certainty)
    var color;
    var circleOpacity = 0;
    var opacityScale = d3.scale.linear()
               .domain([0, 1])
               .range([0, 0.8]);


    if (name == "Congestion") {
        color = "orangered";
    }
    else if (name == "PredictedCongestion") {
        color = "steelblue";
    }
    else {
        color = "C4C4C4";
    }
    circleOpacity = (opacityScale(certainty)).toFixed(2);

    // draw circle
    var circle = new google.maps.Circle({
        center: new google.maps.LatLng(lat, lng),
        radius: 200,
		clickable: true,
        strokeColor: "black",
        strokeOpacity: 0.8,
        strokeWeight: 1,
        fillColor: color,//"red",
        fillOpacity: circleOpacity,
		problemID: problem_id,
        map: map
    });
	// add event to circle
	google.maps.event.addListener(circle, 'click', displayCongestionInfo);
	
    return circle;
}

function displayCongestionInfo()
{
//	infowindowCongestion.open(map, this.getCenter());

    var circle = this;
    setTimeout(function () {
        var pos = circle.getCenter();

		// sets position of infowindow at circle centre
		infowindowCongestion.setPosition(pos);
		infowindowCongestion.open(map);
		
		// modifies the infowindow content
		for (var i = 0; i < activeMapCircles.length; i++)
		{
			if (activeMapCircles[i].problemID == circle.problemID)
			{
				d3.select("#divCongestion")
					.append("p").text("problem name: "+activeMapCircles[i].name)
					.append("p").text("timestamp: "+activeMapCircles[i].timestamp)
					.append("p").text("certainty: "+activeMapCircles[i].certainty)
					.append("p").text("problem id: "+activeMapCircles[i].problemID)
					.append("p").text("av. density: "+activeMapCircles[i].density);
			}
		}
		
		
    }, 50);
}

/// doesn't work properly
function scaleOverlay()
{
    d3.select("#mapcircle").selectAll("circle").attr("r", function (d, i) {
        return (0.00000104 * Math.pow(i * 3 * map.getZoom(), 6) - 0.00007584 * Math.pow(i * 3 * map.getZoom(), 5) +
                      0.0022416 * Math.pow(i * 3 * map.getZoom(), 4) - 0.034028 * Math.pow(i * 3 * map.getZoom(), 3) + 0.28032 * Math.pow(i * 3 * map.getZoom(), 2) - 1.19916 *( i * 3 *map.getZoom())+ 2.1256);
    })//i * 3 + 1.025*map.getZoom();})
}

function addOverlay()
{
    //////////////////////////////////////////////////////////////////////
    // Load the station data. When the data comes back, create an overlay.
    var dt = [{ lat: 45.1835570735929, lng: 5.731261074542999 }, { lat: 45.1835570735929, lng: 5.731261074542999 }, { lat: 45.1835570735929, lng: 5.731261074542999 }, { lat: 45.1835570735929, lng: 5.731261074542999 }];
    var overlay = new google.maps.OverlayView();

    // Add the container when the overlay is added to the map.
    overlay.onAdd = function () {
        var layer = d3.select(this.getPanes().overlayLayer).append("div")
            .attr("class", "mapshape").attr("id","mapcircle");

        // Draw each marker as a separate SVG element.
        // We could use a single SVG, but what size would it have?
        overlay.draw = function () {

            var projection = this.getProjection(),
                padding = 50;

            var marker = layer.selectAll("svg")
                .data(dt)
                .each(transform) // update existing markers
              .enter().append("svg")
                .each(transform)
                .attr("class", "marker");

            // Add a circle.
            marker.append("circle")
                .attr("r", function (d, i) { return i*10})
                .attr("cx", padding)
                .attr("cy", padding);

            // Add a label.
 /*           marker.append("text")
                .attr("x", padding + 7)
                .attr("y", padding)
                .attr("dy", ".31em")
            .text("Circle!!!");//function(d) { return d.key; });
            */
            function transform(d) {
                latLngPoint = new google.maps.LatLng(d.lat, d.lng);
                point = projection.fromLatLngToDivPixel(latLngPoint);
                return d3.select(this)
                    .style("left", (point.x - padding) + "px")
                    .style("top", (point.y - padding) + "px");
            }
        };
    };

    // Bind our overlay to the map…
    overlay.setMap(map);

}



function storeSensors()
{
    // parse sensor position data --- STORES all sensor locations 
    d3.csv("data/sensorpos.csv", function (d) {

        var sensor = { location: "", gps: { lat: 0, lng: 0 }, density: 0, vehicles: 0, lane: "", marker: 0 };

        sensor.location = d.location;
        sensor.gps.lat = d.lat;
        sensor.gps.lng = d.lng;
        sensor.lane = d.lane;


        sensorPos.push(sensor);

    }, function (error, rows) {
        //        console.log(ramps);
    });
}
///////////////////////////////////////////////// DEPRECATED
// function using gmapsmarkers
function addMarkers()   // creates markers on the map for cameras and controllable traffic signs
{
    // parse data containing lat,lng of controllable signs
//    d3.csv("data/controlpos.csv", function (d,i) {
//        posControl.lat.push(d.lat);
//        posControl.lng.push(d.lng);
//        var m = new google.maps.Marker({
//            position: new google.maps.LatLng(d.lat, d.lng),
//            map: map,
//            visible: true,
//            icon: imgControl,
//            title: 'Control',
//            // custom info
//            controlId: markerControl.length,
//            currentSpeed: '100',
//            closedLanes: [],
//            noLanes: i%4+1,
//            currentGeneralPurpose: 'not set'
//        });
//        // add event to show control options at this location
////        google.maps.event.addListener(m, 'click', seeControl);
	
	
//        // markers are saved into an array for ease of access
//        markerControl.push(m);
//    }, function (error, rows) {
////        console.log(rows);
//    });
///////////////////////
   
    // parse sensor position data --- STORES all sensor locations 
    d3.csv("data/sensorpos.csv", function (d) {

	var sensor = { location: "", gps:{lat:0, lng:0}, density: 0, vehicles:0 , lane: "", marker:0};        

	sensor.location = d.location;
	sensor.gps.lat = d.lat;
	sensor.gps.lng = d.lng;
	sensor.lane = d.lane;
 

    // ADDS ramp metering icons
/*	if (sensor.lane == "onramp" || sensor.lane == "offramp") {
	    var m = new google.maps.Marker({
	        position: new google.maps.LatLng(d.lat, d.lng),
	        map: map,
	        visible: true,
	        icon: imgRamp,
	        title: 'Ramp'
	    });
        // add event to see cam at that point
//	    google.maps.event.addListener(m, 'click', seeCam);
	    sensor.marker = m;
	}*/
	
	sensorPos.push(sensor);

    }, function (error, rows) {
//        console.log(ramps);
    });
/////////////////
//    // parse data containing lat,lng of traffic cams
//    d3.csv("data/camerapos.csv", function (d) {
//        posCamera.lat.push(d.lat);
//        posCamera.lng.push(d.lng);

//        var m = new google.maps.Marker({
//            position: new google.maps.LatLng(d.lat, d.lng),
//            map: map,
//            visible: true,
//            icon: imgCamera,
//            title: 'Camera'
//        });

//        // add event to show camera view at this location
//        google.maps.event.addListener(m, 'click',seeCam);

//        // markers are saved into an array for ease of access
//        markerCamera.push(m);
//    }, function (error, rows) {
////        console.log(rows);
//    });
}

function seeCam()   // function to view cam at the selected marker location
{
    infowindow.open(map, this);

    var marker = this;

    setTimeout(function () {
        var pos = marker.getPosition();
		
        // maps API fails without this
        pano = new google.maps.StreetViewPanorama(document.getElementById("divVideo"),
        {
            disableDefaultUI: true,
        });

        pano.setPosition(pos);
        pano.setVisible(true);
    }, 50);
    
}



function removeAreaOfInterest()
{

    for (var i = 0; i < areasOfInterest.length; i++)
    {
        var area = areasOfInterest[i]
        area.setMap(null);
    }

    areasOfInterest = [];
}

function addAreaOfInterest()
{
    var coords = [
                      new google.maps.LatLng(45.18464, 5.72132),
                      new google.maps.LatLng(45.180, 5.72132),                     
                      new google.maps.LatLng(45.180, 5.73),	                  
	                  new google.maps.LatLng(45.18464, 5.73)
    ];

    // Define a rectangle and set its editable property to true.
    var area = new google.maps.Polygon({
        paths: coords,
        strokeColor: '#000000',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#000000',
        fillOpacity: 0.25,
        editable: true,
        draggable: true,
        geodesic: true
    });


    area.setMap(map);

    google.maps.event.addListener(area, 'dragend', showBehaviourInArea);

    areasOfInterest.push(area);
}

function showBehaviourInArea(event)
{

//    computeValues();

}
function updateGraphs() {
    redrawRampGraph();
//    redrawDrivers();
    redrawRampMetering();

    // fixes challenge button to middle of the control window
//    d3.select("#challengeButton").style("left", function () { return (parseInt(d3.select("#divControlStatus").style("width")) / 2 - 50) + "px" });// 30px
//    redrawSuggestions();
}