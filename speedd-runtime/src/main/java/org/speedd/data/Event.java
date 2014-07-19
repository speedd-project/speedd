package org.speedd.data;

import java.util.Map;

public interface Event {
	public String getEventName();
	public long getTimestamp();
	public Map<String, Object> getAttributes();
}
