package org.speedd.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;
import org.speedd.kafka.JsonEventDecoder;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class SimpleEventScheme implements Scheme, org.speedd.Fields {

	private static final long serialVersionUID = 1L;
	
	private JsonEventDecoder jsonEventDecoder;
	
	private static final Logger log = LoggerFactory.getLogger(SimpleEventScheme.class);
	
	public SimpleEventScheme() {
		jsonEventDecoder = new JsonEventDecoder();
	}
	
	@Override
	public List<Object> deserialize(byte[] eventAsBytes) {
		try {
			Event event = jsonEventDecoder.fromBytes(eventAsBytes);
			
			return new Values(event.getEventName(), event.getTimestamp(), event.getAttributes());
		} catch (Exception e){
			log.error("Cannot deserialize event: " + new String(eventAsBytes), e);
			return null;
		}
	}

	@Override
	public Fields getOutputFields() {
		return new Fields(FIELD_NAME, FIELD_TIMESTAMP, FIELD_ATTRIBUTES);
	}

}
