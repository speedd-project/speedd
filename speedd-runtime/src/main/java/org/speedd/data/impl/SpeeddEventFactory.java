package org.speedd.data.impl;

import java.util.HashMap;
import java.util.Map;

import org.speedd.data.Event;
import org.speedd.data.EventFactory;

public class SpeeddEventFactory implements EventFactory {
	private static final SpeeddEventFactory instance = new SpeeddEventFactory();

	private SpeeddEventFactory() {

	}

	public static SpeeddEventFactory getInstance() {
		return instance;
	}

	public Event createEvent(String name, long timestamp, Map<String, Object> attributes) {
		return new DefaultEvent(name, timestamp, new HashMap<String, Object>(attributes));
	}

}
