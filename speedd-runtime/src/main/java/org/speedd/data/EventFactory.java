package org.speedd.data;

import java.util.Map;

public interface EventFactory {
	public Event createEvent(String name, long timestamp, Map<String, Object> attributes);
}
