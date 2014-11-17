package org.speedd;

import org.speedd.data.Event;

public interface EventParser {
	
	public Event fromBytes(byte[] bytes);

}
