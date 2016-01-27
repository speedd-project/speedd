package org.speedd.fraud;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.Fields;

import backtype.storm.spout.Scheme;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class FraudEnrichmentBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	private OutputCollector collector;
	
	
	Logger logger = LoggerFactory.getLogger(FraudEnrichmentBolt.class);
	
	public FraudEnrichmentBolt( String enrichmentPath) {
		
	}

	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		logger.debug("Processing tuple " + input.toString());
		
		collector.emit(input.getValues());
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		backtype.storm.tuple.Fields fields = new backtype.storm.tuple.Fields(Fields.FIELD_PROTON_EVENT_NAME, Fields.FIELD_TIMESTAMP, Fields.FIELD_ATTRIBUTES);
		declarer.declare(fields);

	}

}
