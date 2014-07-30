package org.speedd.messaging;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.speedd.Fields;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.kafka.JsonEventEncoder;

import static org.junit.Assert.*;

public class JsonEncoderTest implements Fields {
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	
	@Test
	public void encodeEvent() throws Exception {
		
		String eventName = "TestEvent";
		
		long timestamp = 1404110725281L;
		
		Long[] arr = new Long[10];
		for (int i=0; i<10; ++i) {
			arr[i] = Long.valueOf(i);
		}
		
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("strattr1", "strval1");
		attrs.put("longval1", new Long(5));
		attrs.put("dblval1", new Double(1.2));
		attrs.put("arrval1", arr);
		attrs.put("timestamp", timestamp);
		
		Event event = eventFactory.createEvent(eventName, timestamp, attrs);
		
		byte[] bytes = new JsonEventEncoder(null).toBytes(event);
		
		assertNotNull(bytes);
		
		String jsonStr = new String(bytes);
		
		System.out.println(jsonStr);
		
		JSONObject json = (JSONObject)new JSONParser().parse(jsonStr);
		assertNotNull(json);
		
		assertEquals(eventName, json.get(FIELD_NAME));
		assertEquals(timestamp, json.get(FIELD_TIMESTAMP));
		
		JSONObject attrJson = (JSONObject)json.get(FIELD_ATTRIBUTES);
		assertNotNull(attrJson);
		
		assertEquals("strval1", attrJson.get("strattr1"));
		assertEquals(5l, attrJson.get("longval1"));
		assertEquals(1.2, attrJson.get("dblval1"));
		assertEquals(timestamp, attrJson.get("timestamp"));
		
		JSONArray arrjson = (JSONArray)attrJson.get("arrval1");
		assertEquals(10, arrjson.size());
		for(long i=0; i<10; ++i){
			assertEquals(i, arrjson.get((int)i));
		}
		
	}
}
