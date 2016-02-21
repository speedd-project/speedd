package org.speedd.fraud;

import java.util.List;

import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class FraudAggregatedReadingScheme implements Scheme, org.speedd.Fields {
	private static final long serialVersionUID = 1L;
	
	private static final FraudAggregatedReadingCsv2Event parser = new FraudAggregatedReadingCsv2Event(SpeeddEventFactory.getInstance());

	@Override
	public List<Object> deserialize(byte[] ser) {
			Event event = parser.fromBytes(ser);
			return new Values(event.getEventName(), event.getTimestamp(), event.getAttributes());
	}

	@Override
	public Fields getOutputFields() {
		return new Fields(FIELD_PROTON_EVENT_NAME, FIELD_TIMESTAMP, FIELD_ATTRIBUTES);
	}

}
