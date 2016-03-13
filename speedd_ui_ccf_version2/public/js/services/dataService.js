app.factory('dataService', function ($rootScope,socket,$http) { // this service broadcasts all data items received from server --- controllers need to listen for 'broadcastRamps'
	var data = {};
	  
	data.selection;
	data.rawEventList=[];
	data.map_data;
	data.map_data2;
	data.countrySelection;
    
    data.analystLoggedIn="";
    data.analysts = [];
	
	data.userEvent;
	data.inTerms = "area";
	
	socket.on('event-list', function (socketData) {
			var events = JSON.parse(socketData.eventList);
			data.rawEventList=events;
			data.broadcastRawEventList();
	});
	
	
	socket.on('speedd-out-events', function (socketData) {
			var event = JSON.parse(socketData);
			data.rawEventList.push(event);
			data.parseEvent(event);
	});
    
    socket.on("analyst", function (socketData){
        data.analysts = socketData;
        console.log(data.analysts);
    });

	data.parseEvent = function(event){

        if (event.Name == "SuddenCardUseNearExpirationDate" || event.name == "SuddenCardUseNearExpirationDate")
		{
			// decode country
			var country = data.map_data.get(data.getCountryCode(event.attributes.card_country));
			// increment flagged
			country.financial.flagged[country.financial.flagged.length-1]++;
			// notify listeners of change
			data.SuddenCardUseNearExpirationDate();
		}
        else if (event.Name == "TransactionsInFarAwayPlaces" || event.name == "TransactionsInFarAwayPlaces")
		{
			// decode country
			var country = data.map_data.get(data.getCountryCode(event.attributes.card_country));
			// increment flagged
			country.financial.flagged[country.financial.flagged.length-1]++;
			// notify listeners of change
			data.TransactionsInFarAwayPlaces();
            data.broadcastMapCountriesData();
		}
		else if (event.Name == "Transaction" || event.name == "Transaction")
		{
			data.broadcastTransaction();
		}
		else if (event.Name == "TransactionStats" || event.name == "TransactionStats")
		{
			// decode country
			var country = data.map_data.get(data.getCountryCode(event.attributes.country));
			// store amount and volume
			country.financial.amount.push(parseInt(event.attributes.average_transaction_amount_eur));
			country.financial.volume.push(parseInt(event.attributes.transaction_volume));
			// notify listeners of change
			data.broadcastTransactionStats();
            // broadcast mapData again to update map countries info
            data.broadcastMapCountriesData();
		}
        else;
	};
	
	
	// function to convert country identifier from callingCode to cca2
	data.getCountryCode = function (callingCode){
		var cca;
//		console.log(callingCode);
		var c = data.map_data2.get(callingCode);
		
		cca = c.cca2;
//		console.log(cca);
		
		return cca;
	}
	
	// function to convert country name to cca2
	data.getCountryCodeFromName = function (name){
		var cca;

		var c = data.map_data_name.get(name);
		cca = c.cca2;
		
		return cca;
	}
	

	
	data.broadcastAllEvents = function (eventList){
		for (var i = 0; i < eventList.length ; i++)
		{
			data.parseEvent(eventList[i]);
		}
	}
	
	$http.get('data/treemapcountries.json')
		.success(function(d, status, headers, config) {
			d.children.forEach(function (d) {
                
                d.colour = "";
                
				d.financial = {};
				
				d.financial.amount = [data.randomInt(0,1000),data.randomInt(0,1000),data.randomInt(0,1000)];
				d.financial.flagged = [data.randomInt(0,1000),data.randomInt(0,1000),data.randomInt(0,1000)];
				d.financial.volume = [data.randomInt(0,10000),data.randomInt(0,10000),data.randomInt(0,10000)];
//					console.log(map_data.set(d.cca2, d))
//					countryProperties[d.cca2] = d;
			});
		  
		  data.map_data = d3.map(d.children, function(d){return d.cca2;});
		  data.map_data2 = d3.map(d.children, function(d){return d.callingCode;});
		  data.map_data_name = d3.map(d.children, function(d){return d.name.common;});
//		  console.log(data.map_data_name.get("Tajikistan").cca2);
		  data.broadcastMapCountriesData();
		  
		})
		.error(function(data, status, headers, config) {
		  // log error
		});
	
	
  data.changeSelection = function(obj){	//changes ramp selected based on rampList click (RampListController)
	data.selection = obj;
	data.broadcastSelectionChanged();
//	console.log(obj);
  };
  
  data.changeCountrySelection = function(obj){	//changes ramp selected based on rampList click (RampListController)
	data.countrySelection = obj;
	data.broadcastCountrySelectionChanged();
	console.log(data.countrySelection);
  };
  
  
  data.changeStatsClick = function(obj){
	data.inTerms = obj;
	data.broadcastStatsClick();
  }
  
  data.updateAnalyst = function(obj){
	data.analystLoggedIn = obj;
    // emit the analyst name
    socket.emit("analyst", data.analystLoggedIn)
  }

  data.broadcastSelectionChanged = function(){
	$rootScope.$broadcast('broadcastSelectionChanged');
  };
  
  data.broadcastCountrySelectionChanged = function(){
	$rootScope.$broadcast('broadcastCountrySelectionChanged');
  };
  
   data.broadcastStatsClick = function(){
	$rootScope.$broadcast('broadcastStatsClick');
  };
  
    data.broadcastMapCountriesData = function(){
	$rootScope.$broadcast('broadcastMapCountriesData');
  };
  
  data.broadcastRawEventList = function(){
	$rootScope.$broadcast('broadcastRawEventList');
  };

  data.broadcastFraudAtATM = function(){
	$rootScope.$broadcast('broadcastFraudAtATM');
  };
    
  data.broadcastIncreasingAmounts = function(){
	$rootScope.$broadcast('broadcastIncreasingAmounts');
  };
  
  data.broadcastTransaction = function(){
	$rootScope.$broadcast('broadcastTransaction');
  };
  data.broadcastTransactionStats = function(){
	$rootScope.$broadcast('broadcastTransactionStats');
  };
  data.SuddenCardUseNearExpirationDate = function(){
	$rootScope.$broadcast('SuddenCardUseNearExpirationDate');
  };
  data.TransactionsInFarAwayPlaces = function(){
	$rootScope.$broadcast('TransactionsInFarAwayPlaces');
  };


    data.randomInt = function (min, max) // function that generates a random int between min and max
	{
		return Math.floor(Math.random() * (max - min + 1) + min);
	}
    
  return data;
});