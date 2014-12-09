package org.speedd.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.kafka.JsonEventDecoder;

public class JsonDecoderTest {
	@Test
	public void decodeEvent(){
		String jsonStr = "{\"timestamp\":1404110725281,\"name\":\"TestEvent\",\"attributes\":{\"timestamp\":1404110725281,\"intval1\":5,\"strattr1\":\"strval1\",\"arrval1\":[0,1,2,3,4,5,6,7,8,9],\"floatval1\":1.2}}";

		JsonEventDecoder decoder = new JsonEventDecoder();
		
		Event event = decoder.fromBytes(jsonStr.getBytes());
		
		assertNotNull(event);
		
		assertEquals("TestEvent", event.getEventName());
		assertEquals(1404110725281L, event.getTimestamp());
		
		Map<String, Object> attributes = event.getAttributes();
		
		assertNotNull(attributes);
		
		assertEquals(1404110725281L, attributes.get("timestamp"));
		assertEquals(5L, attributes.get("intval1"));
		assertEquals("strval1", attributes.get("strattr1"));
		assertEquals(1.2, attributes.get("floatval1"));
		
		Long[] arr = (Long[])attributes.get("arrval1");
		assertNotNull(arr);
	}
}
