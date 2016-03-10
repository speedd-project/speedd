package org.speedd;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

import org.speedd.data.Event;
import org.speedd.kafka.JsonEventDecoder;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class EventJsonScheme implements Scheme, org.speedd.Fields {
	private static final long serialVersionUID = 1L;
	
	JsonEventDecoder jsonEventDecoder = new JsonEventDecoder();
	
	@Override
	public List<Object> deserialize(byte[] ser) {
		try {
			String jsonAdminCommand = new String(ser, "UTF-8");
			
			Event event = jsonEventDecoder.fromBytes(jsonAdminCommand.getBytes(Charset.forName("UTF-8"))); 
			return new Values(event.getEventName(), event.getTimestamp(), event.getAttributes());
		}
		catch (UnsupportedEncodingException e){
			throw new ParsingError(e);
		}
	}

	@Override
	public Fields getOutputFields() {
		return new Fields(FIELD_NAME, FIELD_TIMESTAMP, FIELD_ATTRIBUTES);
	}

}
