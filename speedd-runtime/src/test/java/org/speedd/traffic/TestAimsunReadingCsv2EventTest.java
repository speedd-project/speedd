package org.speedd.traffic;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

public class TestAimsunReadingCsv2EventTest {
	private static final TrafficAimsunReadingCsv2Event parser = new TrafficAimsunReadingCsv2Event(SpeeddEventFactory.getInstance());
	
	@Test
	public void testCsv2Event() throws Exception{
		String eventCsv = "\r\n 2015-12-16 06:00:15,1375,60.607787719,2,1,11.8837352842,7.82520854977,4.05852673446,7.328";
		System.out.println(eventCsv);
		Event event = parser.fromBytes(eventCsv.getBytes(Charset.forName("UTF-8")));
		
		assertNotNull(event);
		
		assertEquals(Constants.TRAFFIC_SENSOR_READING_AVERAGE, event.getEventName());
		
		Calendar calendar = new GregorianCalendar();
		System.out.println("event timestamp: "+event.getTimestamp());
		System.out.println("event date: "+new Date(event.getTimestamp()));
		calendar.setTime(new Date(event.getTimestamp()));
		
		assertEquals(16,calendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(11, calendar.get(Calendar.MONTH));
		assertEquals(2015, calendar.get(Calendar.YEAR));
		assertEquals(6, calendar.get(Calendar.HOUR));
		assertEquals(0, calendar.get(Calendar.MINUTE));
		assertEquals(15, calendar.get(Calendar.SECOND));
		
		Map<String,Object> attributes = event.getAttributes();
		assertEquals(attributes.get(TrafficAimsunReadingCsv2Event.ATTR_DETECTOR_ID),"1375");
		assertEquals(attributes.get(TrafficAimsunReadingCsv2Event.ATTR_AVG_SPEED),Double.valueOf("60.607787719"));
		assertEquals(attributes.get(TrafficAimsunReadingCsv2Event.ATTR_OCCUPANCY),Double.valueOf("7.328")/100);
		assertEquals(attributes.get(TrafficAimsunReadingCsv2Event.ATTR_VEHICLES),3);
		assertEquals(attributes.get(TrafficAimsunReadingCsv2Event.ATTR_DENSITY),Double.valueOf("11.8837352842"));
					
		
	
		/*Map<String, Object> attrs = event.getAttributes();
		assertEquals(12.16, attrs.get("occupancy"));
		assertEquals(Long.valueOf(7), attrs.get("vehicles"));
		assertEquals(60.0, attrs.get("average_speed"));*/
		
	}

}
