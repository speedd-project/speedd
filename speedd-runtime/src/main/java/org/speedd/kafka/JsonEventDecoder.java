package org.speedd.kafka;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import kafka.serializer.Decoder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.speedd.ParsingError;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

public class JsonEventDecoder implements Decoder<Event> {
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	
	@Override
	public Event fromBytes(byte[] eventAsBytes) {
		JSONParser parser = new JSONParser();
		
		try {
			JSONObject json = (JSONObject)parser.parse(new String(eventAsBytes));
			
			String eventName = (String)json.get("eventName");
			long timestamp = (Long)json.get("timestamp");
			
			Map<String, Object> attributes = decodeAttributes((JSONObject)json.get("attributes"));
			
			return eventFactory.createEvent(eventName, timestamp, attributes);
		} catch (ParseException e) {
			throw new ParsingError("Error parsing event JSON representation", e);
		}
		
	}
	
	private Map<String, Object> decodeAttributes(JSONObject attrJson){
		Map<String, Object> attrMap = new HashMap<String, Object>(attrJson.size());
		
		for (Object obj : attrJson.entrySet()) {
			Entry<String, Object> entry = (Entry<String, Object>)obj;
			
			String attrName = entry.getKey();
			
			Object attrVal = entry.getValue();
			
			if(attrVal instanceof JSONArray){
				JSONArray jsonArr = (JSONArray)attrVal;
				Object val = jsonArr.get(0);
				
				Long[] arr = (Long[])Array.newInstance(val.getClass(), jsonArr.size());
				for (int i=0,n=jsonArr.size(); i<n; ++i) {
					Array.set(arr, i, jsonArr.get(i));
				}
				
				attrMap.put(attrName, arr);
			}
			else {
				attrMap.put(attrName, attrVal);
			}
		}
		return attrMap;
	}

}
