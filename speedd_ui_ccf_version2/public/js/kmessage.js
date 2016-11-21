var kafkaRuntimeLog = [];
var transactionsNo = 0;
var flaggedTransactionsNo = 0;
var transactionStats = [];

var mesTS = {
    "name": "TransactionStats",
    "timestamp": 1409901066030,
    "attributes": {
        "country": 972,
        "average_transaction_amount_eur": "120.5",
        "transaction_volume": 15616512
    }
};

var mesT = {
    "name": "Transaction",
    "timestamp": 1409901066030,
    "attributes": {
        "card_pan": "1234-1234",
        "terminal_id": "123456578",
        "cvv_validation": 10,
        "amount_eur": "120.5",
        "acquirer_country": 972,
        "is_cnp": 1
    }
};

var mesIA = {
    "name": "IncreasingAmounts",
    "timestamp": 1409901066030,
    "attributes": {
        "Certainty": "1",
        "card_pan": "1235-1234",
        "terminal_id": 12345567,
        "TrendCount": 1.2
    }
};

var mesFATM = {
    "name": "FraudAtATM",
    "timestamp": 1409901066030,
    "attributes": {
        "Certainty": "0.8",
        "terminal_id": 12345678
    }
};

function parseKafkaMessage(message)
{
//	console.log(message.Name);
	
	switch(message.name)
	{
	    case "TransactionStats":
	        transactionStats.push(message.attributes)
	        updateTransactionStats();
	        updateTreemapData(message.attributes);
			break;
	    case "FraudAtATM":
	        // update UI
	        updateFlaggedTransactions(message);
            break;
	    case "IncreasingAmounts":
	        // update UI
	        updateFlaggedTransactions(message);
	        break;
	    case "Transaction":
	        transactionsNo++;
	        // update UI
	        d3.select("#textTop1").text(transactionsNo);
	        break;
	    default:
	        console.log("just testing...");
	        break;
	}
		

	kafkaRuntimeLog.push(message);
}

function updateTransactionStats() {
    var aveTransactionAmount = 0;
    var aveTransactionVolume = 0;
    var sumAmount = 0;
    var sumVolume = 0;

    // compute ave trans amount and volume
    for (var i = 0; i < transactionStats.length; i++) {
        //        console.log(i);
        sumAmount += transactionStats[i].average_transaction_amount_eur.toFloat();
        sumVolume += transactionStats[i].transaction_volume;
    }
    aveTransactionAmount = sumAmount / transactionStats.length;
    aveTransactionVolume = sumVolume / transactionStats.length;

    // update UI
    d3.select("#textTop3").text(aveTransactionAmount);
    d3.select("#textTop4").text(aveTransactionVolume);
}

function updateFlaggedTransactions(message) {
    flaggedTransactionsNo++;
    d3.select("#textTop2").text(flaggedTransactionsNo);
    d3.select("#divFlaggedTransContainer").append("p")
        .append("text").attr("id", "textFlaggedTransactions")
        .text(message.name + " " + message.attributes.terminal_id)
        .on("mouseover", function () { d3.select(this).style("cursor", "pointer"); })
        .on("click", function () {

            // clear selection info
            d3.select("#textSelectionContainer").selectAll("p").remove();
            // print flagged transaction info
            d3.select("#textSelectionContainer").append("p").text("Name: " + message.name);
			d3.select("#textSelectionContainer").append("p").text("Terminal ID: " + message.attributes.terminal_id);
			d3.select("#textSelectionContainer").append("p").text("Certainty: " + message.attributes.Certainty);
			d3.select("#textSelectionContainer").append("p").text("Card Pan: " + message.attributes.card_pan);


            // make all text black
            removeTextHighlights();
            // highlight selected flagged transaction 
            d3.select(this).style("color", "steelblue");
        });

}

function removeTextHighlights()
{
    d3.selectAll("#textFlaggedTransactions").style("color", "black");
    d3.selectAll("#pattern").style("color", "black");
}