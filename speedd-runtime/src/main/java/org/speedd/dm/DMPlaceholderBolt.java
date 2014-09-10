package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

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
public class DMPlaceholderBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	private OutputCollector collector;
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	
	Logger logger = LoggerFactory.getLogger(DMPlaceholderBolt.class);

	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		logger.debug("Processing tuple " + input.toString());
		
		Event event = (Event)input.getValueByField("message");
		
		String eventName = event.getEventName();
		
		long timestamp = event.getTimestamp();
		
		Map<String, Object> attributes = event.getAttributes();
		
		Map<String, Object> outAttrs = new HashMap<String, Object>();
		
		outAttrs.put("decision", "do something");
		
		outAttrs.put("newMeterRate", computeTheNewRate(attributes));
		
		Event outEvent = eventFactory.createEvent("Action", timestamp, outAttrs);

		logger.debug("Emitting out event: " + outEvent.getEventName());
		
		//FIXME use meaningful value for the 'key' field. It'll be used by kafka for partitioning
		collector.emit(new Values("1", outEvent));
	}

	private double computeTheNewRate(Map<String, Object> attributes) {
		// TODO Auto-generated method stub
		return 2;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}

}
