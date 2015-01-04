package org.speedd.dm;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * A placeholder for DM lightweight in-storm decision making component.
 * 
 * @author kofman
 *
 */
public class NopBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	Logger logger = LoggerFactory.getLogger(NopBolt.class);

	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
	}

	@Override
	public void execute(Tuple input) {
		logger.debug("Processing tuple " + input.toString());
		
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	}

}
