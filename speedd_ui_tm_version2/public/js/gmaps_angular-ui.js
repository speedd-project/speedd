app.controller('mapController', function($scope, uiGmapGoogleMapApi) {
  
	$scope.isCollapsed = false;
	
	
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
  // BUTTONS ======================
  var mapOptions =
    {
        center: new google.maps.LatLng(45.1841656, 5.7155425),
        zoom: 13,
        // disable map controls
        disableDefaultUI: true,
        streetViewControl: false,
        scrollwheel: true,
        navigationControl: true,
        mapTypeControl: true,
        scaleControl: true,
        draggable: true,
        //
        mapTypeControlOptions: {
            mapTypeIds: [google.maps.MapTypeId.ROADMAP, 'map_style']
        },
		styles: styles
    };
  
	$scope.map = {center: {latitude: 45.1841656, longitude: 5.7155425 }, zoom: 14, bounds:{} };
    $scope.options = mapOptions;
	
	$scope.circles = [];
	$scope.sensorPos = [];
	$scope.markers = [];
	$scope.circles = [{
                id: 1,
                center: {
                    latitude: 45.205385,
                    longitude: 5.78356
                },
                radius: 50,
                stroke: {
                    color: '#08B21F',
                    weight: 2,
                    opacity: 1
                },
                fill: {
                    color: '#08B21F',
                    opacity: 0.5
                },
                geodesic: true, // optional: defaults to false
                draggable: true, // optional: defaults to false
                clickable: true, // optional: defaults to true
                editable: true, // optional: defaults to false
                visible: true, // optional: defaults to true
                control: {}
            }];
	$scope.marker = {
      id: 0,
      coords: {
        latitude: 40.1451,
        longitude: -99.6680
      },
      options: { draggable: true },
      events: {
        dragend: function (marker, eventName, args) {
          $log.log('marker dragend');
          var lat = marker.getPosition().lat();
          var lon = marker.getPosition().lng();
          $log.log(lat);
          $log.log(lon);

          $scope.marker.options = {
            draggable: true,
            labelContent: "lat: " + $scope.marker.coords.latitude + ' ' + 'lon: ' + $scope.marker.coords.longitude,
            labelAnchor: "100 0",
            labelClass: "marker-labels"
          };
        }
      }
    };
	function drawCirclesAlert(lat,lng,problemId,name,certainty){
			// determine color of circle (from name) and fill opacity (from certainty)
		var color;
		var circleOpacity = 0;
		var opacityScale = d3.scale.linear()
				   .domain([0, 1])
				   .range([0, 0.8]);


		if (name == "Congestion") {
			color = "red";
		}
		else if (name == "PredictedCongestion") {
			color = "steelblue";
		}
		else {
			color = "C4C4C4";
		}
		circleOpacity = (opacityScale(certainty)).toFixed(2);

		// draw circle
		var circle = {
				id: problemId,
                center: {
                    latitude: lat,
                    longitude: lng
                },
                radius: 200,
                stroke: {
                    color: 'black',
                    weight: 1,
                    opacity: 0.8
                },
                fill: {
                    color: color,
                    opacity: circleOpacity
                },
                geodesic: true, // optional: defaults to false
                draggable: false, // optional: defaults to false
                clickable: true, // optional: defaults to true
                editable: false, // optional: defaults to false
                visible: true, // optional: defaults to true
                control: {}
            };
			function showCongestionInfo(){}
		return circle;
	}
	/*
	$scope.m = [];
	d3.csv("data/sensorpos.csv", function (d,i) {
	

	var sensor = { location: "", gps:{lat:0, lng:0}, density: 0, vehicles:0 , lane: "", marker:0};        

	sensor.location = d.location;
	sensor.gps.lat = d.lat;
	sensor.gps.lng = d.lng;
	sensor.lane = d.lane;
	
			
	var m = new MarkerWithLabel({
                position: new google.maps.LatLng(sensor.location.lat, sensor.location.lng),
                map: map,
                visible: true,
                icon: imgRamp,
                title: sensor.id.toString(),
                labelAnchor: new google.maps.Point(3, -13),
                labelContent: sensor.id.toString(),
                labelClass: "markerlabel" // the CSS class for the label
            });
	sensor.marker=m;
	$scope.markers.push(m);
	var m = {id: i, coords:{latitude: sensor.gps.lat, longitude: sensor.gps.lng}, title: 'marker'+i, options: { draggable: true }};

	sensor.marker=m;
	$scope.markers.push(m);
	$scope.m.push(sensor);
//	console.log(sensor);
	$scope.sensorPos.push(sensor);

    }, function (error, rows) {
        console.log(JSON.stringify($scope.m));
    });*/
	$scope.ramps = [{"location":"0024a4dc00003356","gps":{"lat":"45.205385","lng":"5.78356"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":0,"coords":{"latitude":"45.205385","longitude":"5.78356"},"title":"marker0","options":{"draggable":true}}},{"location":"0024a4dc00003354","gps":{"lat":"45.2019","lng":"5.781605"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":1,"coords":{"latitude":"45.2019","longitude":"5.781605"},"title":"marker1","options":{"draggable":true}}},{"location":"0024a4dc0000343b","gps":{"lat":"45.189538","lng":"5.781835"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":2,"coords":{"latitude":"45.189538","longitude":"5.781835"},"title":"marker2","options":{"draggable":true}}},{"location":"0024a4dc0000343b","gps":{"lat":"45.188033","lng":"5.781614"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":3,"coords":{"latitude":"45.188033","longitude":"5.781614"},"title":"marker3","options":{"draggable":true}}},{"location":"0024a4dc00003445","gps":{"lat":"45.183515","lng":"5.779847"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":4,"coords":{"latitude":"45.183515","longitude":"5.779847"},"title":"marker4","options":{"draggable":true}}},{"location":"0024a4dc00003445","gps":{"lat":"45.18249","lng":"5.778956"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":5,"coords":{"latitude":"45.18249","longitude":"5.778956"},"title":"marker5","options":{"draggable":true}}},{"location":"0024a4dc00001b67","gps":{"lat":"45.18104","lng":"5.777474"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":6,"coords":{"latitude":"45.18104","longitude":"5.777474"},"title":"marker6","options":{"draggable":true}}},{"location":"0024a4dc00000ddd","gps":{"lat":"45.167782","lng":"5.758982"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":7,"coords":{"latitude":"45.167782","longitude":"5.758982"},"title":"marker7","options":{"draggable":true}}},{"location":"0024a4dc00003355","gps":{"lat":"45.163866","lng":"5.755275"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":8,"coords":{"latitude":"45.163866","longitude":"5.755275"},"title":"marker8","options":{"draggable":true}}},{"location":"0024a4dc000021d1","gps":{"lat":"45.157567","lng":"5.748415"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":9,"coords":{"latitude":"45.157567","longitude":"5.748415"},"title":"marker9","options":{"draggable":true}}},{"location":"0024a4dc0000343f","gps":{"lat":"45.154437","lng":"5.744254"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":10,"coords":{"latitude":"45.154437","longitude":"5.744254"},"title":"marker10","options":{"draggable":true}}},{"location":"0024a4dc00001b5c","gps":{"lat":"45.151638","lng":"5.737041"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":11,"coords":{"latitude":"45.151638","longitude":"5.737041"},"title":"marker11","options":{"draggable":true}}},{"location":"0024a4dc000025eb","gps":{"lat":"45.150762","lng":"5.730067"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":12,"coords":{"latitude":"45.150762","longitude":"5.730067"},"title":"marker12","options":{"draggable":true}}},{"location":"0024a4dc000025ea","gps":{"lat":"45.15171","lng":"5.721857"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":13,"coords":{"latitude":"45.15171","longitude":"5.721857"},"title":"marker13","options":{"draggable":true}}},{"location":"0024a4dc000013c6","gps":{"lat":"45.153985","lng":"5.715696"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":14,"coords":{"latitude":"45.153985","longitude":"5.715696"},"title":"marker14","options":{"draggable":true}}},{"location":"0024a4dc00003444","gps":{"lat":"45.157333","lng":"5.712402"},"density":0,"vehicles":0,"lane":"offramp","marker":{"id":15,"coords":{"latitude":"45.157333","longitude":"5.712402"},"title":"marker15","options":{"draggable":true}}},{"location":"0024a4dc000025ec","gps":{"lat":"45.158797","lng":"5.707607"},"density":0,"vehicles":0,"lane":"onramp","marker":{"id":16,"coords":{"latitude":"45.158797","longitude":"5.707607"},"title":"marker16","options":{"draggable":true}}}];
	
	/*for (var i = 0 ; i<ramps.length ; i++)
		$scope.markers.push(ramps[i].marker);*/
	
//	    $scope.markers = [{"id":0,"coords":{"latitude":"45.205385","longitude":"5.78356"},"title":"marker0","options":{"draggable":true}},{"id":1,"coords":{"latitude":"45.2019","longitude":"5.781605"},"title":"marker1","options":{"draggable":true}},{"id":2,"coords":{"latitude":"45.189538","longitude":"5.781835"},"title":"marker2","options":{"draggable":true}},{"id":3,"coords":{"latitude":"45.188033","longitude":"5.781614"},"title":"marker3","options":{"draggable":true}},{"id":4,"coords":{"latitude":"45.183515","longitude":"5.779847"},"title":"marker4","options":{"draggable":true}},{"id":5,"coords":{"latitude":"45.18249","longitude":"5.778956"},"title":"marker5","options":{"draggable":true}},{"id":6,"coords":{"latitude":"45.18104","longitude":"5.777474"},"title":"marker6","options":{"draggable":true}},{"id":7,"coords":{"latitude":"45.167782","longitude":"5.758982"},"title":"marker7","options":{"draggable":true}},{"id":8,"coords":{"latitude":"45.163866","longitude":"5.755275"},"title":"marker8","options":{"draggable":true}},{"id":9,"coords":{"latitude":"45.157567","longitude":"5.748415"},"title":"marker9","options":{"draggable":true}},{"id":10,"coords":{"latitude":"45.154437","longitude":"5.744254"},"title":"marker10","options":{"draggable":true}},{"id":11,"coords":{"latitude":"45.151638","longitude":"5.737041"},"title":"marker11","options":{"draggable":true}},{"id":12,"coords":{"latitude":"45.150762","longitude":"5.730067"},"title":"marker12","options":{"draggable":true}},{"id":13,"coords":{"latitude":"45.15171","longitude":"5.721857"},"title":"marker13","options":{"draggable":true}},{"id":14,"coords":{"latitude":"45.153985","longitude":"5.715696"},"title":"marker14","options":{"draggable":true}},{"id":15,"coords":{"latitude":"45.157333","longitude":"5.712402"},"title":"marker15","options":{"draggable":true}},{"id":16,"coords":{"latitude":"45.158797","longitude":"5.707607"},"title":"marker16","options":{"draggable":true}}];

	
// Get the bounds from the map once it's loaded
    $scope.$watch(function() {
      return $scope.map.bounds;
    }, function(nv, ov) {
      // Only need to regenerate once
      if (!ov.southwest && nv.southwest) {
//	  console.log($scope.map.bounds);
        var markers = [];
        for (var i = 0; i < $scope.ramps.length; i++) {
          markers.push($scope.ramps[i].marker)
		  markers[i].latitude = $scope.ramps[i].marker.coords.latitude;
		  markers[i].longitude = $scope.ramps[i].marker.coords.longitude;
		  markers[i].icon = imgRamp;
		  markers[i].options = {draggable: false};
		  /* doesn't work says that label is not string
		  var label = i.toString();
		  markers[i].options = {label:{labelAnchor: new google.maps.Point(3, -13),
                labelContent: label,
                labelClass: "markerlabel" // the CSS class for the label
				}};*/
        }
        $scope.markers = markers;
//		console.log($scope.markers);
		
		$scope.circles.push(drawCirclesAlert(45.1841656, 5.7155425,"asdas","PredictedCongestion",0.5));
      }
    }, true);
	
	
});

app.config(function(uiGmapGoogleMapApiProvider) {
    uiGmapGoogleMapApiProvider.configure({
        //    key: 'your api key',
        v: '3.17',
        libraries: 'weather,geometry,visualization'
    });
})

