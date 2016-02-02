package org.speedd;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class JoinBolt extends BaseBasicBolt implements Fields {
	private static final long serialVersionUID = 1L;
	
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	Logger logger = LoggerFactory.getLogger(JoinBolt.class);


	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		backtype.storm.tuple.Fields fields = new backtype.storm.tuple.Fields(FIELD_PROTON_EVENT_NAME, FIELD_TIMESTAMP, FIELD_ATTRIBUTES);
		declarer.declare(fields);

	}


	@Override
	public void execute(Tuple input, BasicOutputCollector collector) {
		List<Object> output = new ArrayList<Object>();
		output.addAll(input.getValues());
		
		collector.emit(output);
	}
}
