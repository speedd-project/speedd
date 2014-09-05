package org.speedd.traffic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.traffic.Constants;
import org.speedd.traffic.TrafficAggregatedReadingCsv2Event;

public class TrafficAggregatedReadingCsv2EventTest {
	private static final TrafficAggregatedReadingCsv2Event parser = new TrafficAggregatedReadingCsv2Event(SpeeddEventFactory.getInstance());

	@Test
	public void testCsv2Event() throws Exception {
		String eventCsv = "2014-04-13,08:00:00,0024a4dc0000343e,right,12.16,7,\\N,60.0,0,0,0,0,0,4,0,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,2,3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
		
		Event event = parser.csv2event(eventCsv);
		
		assertNotNull(event);
		
		assertEquals(Constants.TRAFFIC_SENSOR_READING_AGGREGATED, event.getEventName());
		
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(new Date(event.getTimestamp()));
		
		assertEquals(13,calendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(3, calendar.get(Calendar.MONTH));
		assertEquals(2014, calendar.get(Calendar.YEAR));
		assertEquals(8, calendar.get(Calendar.HOUR));
		assertEquals(0, calendar.get(Calendar.MINUTE));
		assertEquals(0, calendar.get(Calendar.SECOND));

		Map<String, Object> attrs = event.getAttributes();
		assertEquals("0024a4dc0000343e", attrs.get("location"));
		assertEquals("right", attrs.get("lane"));
		assertEquals(12.16, attrs.get("occupancy"));
		assertEquals(Long.valueOf(7), attrs.get("vehicles"));
		assertNull(attrs.get("median_speed"));
		assertEquals(60.0, attrs.get("average_speed"));
		
		Long[] speedHistogram = (Long[])attrs.get("speed_histogram");
		assertNotNull(speedHistogram);
		assertEquals(20, speedHistogram.length);
		assertEquals(Long.valueOf(4), speedHistogram[5]);
		
		Long[] lengthHistogram = (Long[])attrs.get("length_histogram");
		assertNotNull(lengthHistogram);
		assertEquals(100, lengthHistogram.length);
		assertEquals(Long.valueOf(1), lengthHistogram[6]);
		
	}

}
