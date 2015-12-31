package org.speedd.kafka;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import kafka.serializer.Encoder;
import kafka.serializer.StringEncoder;
import kafka.utils.VerifiableProperties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.speedd.Fields;
import org.speedd.data.Event;


/**
 * @author kofman
 *
 */
public class JsonEventEncoder implements Encoder<Object>, Fields {
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
		jsonObj.put(FIELD_NAME, event.getEventName());
		jsonObj.put(FIELD_TIMESTAMP, event.getTimestamp());
		jsonObj.put(FIELD_ATTRIBUTES, encodeAttributes(event.getAttributes()));
		
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
				attrJson.put(attrName, jsonizeVal(attrVal));
			}
		}
		
		return attrJson;
	}

	/**
	 * Currently the main reason for this is that UUID should be converted to String in order
	 * to be serialized correctly
	 * 
	 * @param attrVal
	 * @return
	 */
	private Object jsonizeVal(Object attrVal) {
		if(attrVal instanceof UUID){
			return String.valueOf(attrVal);
		}
		
		if(attrVal instanceof Date){
			return String.valueOf(attrVal);
			 
		}
		return attrVal;
	}
}
