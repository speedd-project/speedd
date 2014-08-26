package org.speedd.util;

import java.util.List;

import org.speedd.data.Event;
import org.speedd.kafka.JsonEventDecoder;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class SimpleEventScheme implements Scheme, org.speedd.Fields {

	private static final long serialVersionUID = 1L;
	
	private JsonEventDecoder jsonEventDecoder;
	
	public SimpleEventScheme() {
		jsonEventDecoder = new JsonEventDecoder();
	}
	
	@Override
	public List<Object> deserialize(byte[] eventAsBytes) {
		Event event = jsonEventDecoder.fromBytes(eventAsBytes);
		
		return new Values(event.getEventName(), event.getTimestamp(), event.getAttributes());
	}

	@Override
	public Fields getOutputFields() {
		return new Fields(FIELD_NAME, FIELD_TIMESTAMP, FIELD_ATTRIBUTES);
	}

}
