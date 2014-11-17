package org.speedd.fraud;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import org.speedd.ParsingError;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

public class FraudAggregatedReadingScheme implements Scheme, org.speedd.Fields {
	private static final long serialVersionUID = 1L;
	
	private static final FraudAggregatedReadingCsv2Event parser = new FraudAggregatedReadingCsv2Event(SpeeddEventFactory.getInstance());

	@Override
	public List<Object> deserialize(byte[] ser) {
		try {
			String csv = new String(ser, "UTF-8");
			Event event = parser.fromBytes(csv.getBytes(Charset.forName("UTF-8")));
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
