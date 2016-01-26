package org.speedd.dm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * A placeholder for DM lightweight in-storm decision making component.
 * 
 * @author kofman
 *
 */
public class EchoBolt extends BaseBasicBolt {
	private static final long serialVersionUID = 1L;
	
	Logger logger = LoggerFactory.getLogger(EchoBolt.class);

	@Override
	public void execute(Tuple input, BasicOutputCollector collector) {
		logger.debug("Processing tuple " + input.toString());
		
		collector.emit(new Values("1", input.getValueByField("message")));
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}

}
