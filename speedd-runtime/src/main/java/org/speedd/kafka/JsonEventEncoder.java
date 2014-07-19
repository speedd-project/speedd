package org.speedd.kafka;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;

import kafka.serializer.Encoder;
import kafka.serializer.StringEncoder;
import kafka.utils.VerifiableProperties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.speedd.data.Event;


public class JsonEventEncoder implements Encoder<Object> {
	private StringEncoder stringEncoder;
	
	public JsonEventEncoder(VerifiableProperties properties) {
		stringEncoder = new StringEncoder(properties);
	}

	@Override
	public byte[] toBytes(Object object) {
		if(object instanceof String){
			return stringEncoder.toBytes((String)object);
		}
		
		if(object instanceof Event){
			return eventToBytes((Event)object);
		}

		throw new RuntimeException("Unsupported data type for encoding: " + object.getClass().getName());
	}
	
	public byte[] eventToBytes(Event event) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("eventName", event.getEventName());
		jsonObj.put("timestamp", event.getTimestamp());
		jsonObj.put("attributes", encodeAttributes(event.getAttributes()));
		
		return jsonObj.toJSONString().getBytes();
	}
	
	private JSONObject encodeAttributes(Map<String, Object> attrMap){
		JSONObject attrJson = new JSONObject();
		
		for (Entry<String, Object> entry : attrMap.entrySet()) {
			String attrName = entry.getKey();
			
			Object attrVal = entry.getValue();
			
			if(attrVal instanceof Object[]){
				JSONArray arr = new JSONArray();
				for(int i=0,n=Array.getLength(attrVal); i<n; ++i){
					arr.add(Array.get(attrVal, i));
				}
				attrJson.put(attrName, arr);
			}
			else {
				attrJson.put(attrName, attrVal);
			}
		}
		
		return attrJson;
	}
}
