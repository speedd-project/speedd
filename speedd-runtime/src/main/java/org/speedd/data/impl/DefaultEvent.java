package org.speedd.data.impl;

import java.util.HashMap;
import java.util.Map;

import org.speedd.data.Event;
import org.speedd.kafka.JsonEventEncoder;

public class DefaultEvent implements Event {
	private String eventName;
	private long timestamp;
	private Map<String, Object> attrMap;

	protected DefaultEvent(String eventName, long timestamp) {
		this(eventName, timestamp, new HashMap<String, Object>());
	}

	protected DefaultEvent(String eventName, long timestamp, HashMap<String, Object> attributes) {
		this.eventName = eventName;
		this.timestamp = timestamp;
		attrMap = attributes;
	}

	public String getEventName() {
		return eventName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Map<String, Object> getAttributes() {
		return attrMap;
	}
	
	@Override
	public String toString() {
		JsonEventEncoder encoder = new JsonEventEncoder(null);
		return new String(encoder.eventToBytes(this));
	}
}
