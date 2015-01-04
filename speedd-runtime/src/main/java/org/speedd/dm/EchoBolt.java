package org.speedd.dm;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * A placeholder for DM lightweight in-storm decision making component.
 * 
 * @author kofman
 *
 */
public class EchoBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	private OutputCollector collector;
	
	Logger logger = LoggerFactory.getLogger(EchoBolt.class);

	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		logger.debug("Processing tuple " + input.toString());
		
		collector.emit(new Values("1", input.getValueByField("message")));
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}

}
