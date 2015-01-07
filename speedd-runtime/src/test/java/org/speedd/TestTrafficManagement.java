package org.speedd;

import org.junit.Test;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.traffic.TrafficAggregatedReadingCsv2Event;

public class TestTrafficManagement extends BaseSpeeddIntegrationTest {
	@Test
	public void testCNRS() throws Exception {
		TimedEventFileReader eventReader = new TimedEventFileReader(TestTrafficManagement.class.getClassLoader().getResource("traffic-data-short.csv").getPath(), "speedd-in-events", createProducerConfig(), new TrafficAggregatedReadingCsv2Event(SpeeddEventFactory.getInstance()));
		//EventFileReader eventReader = new EventFileReader(TestTrafficManagement.class.getClassLoader().getResource("inputCNRS.csv").getPath(), "speedd-in-events", createProducerConfig(), 1000);

		streamEventsAndVerifyResults("speedd-traffic.properties", "traffic", eventReader, new String[]{"PredictedCongestion", "2minsAverageDensityAndSpeedPerLocation"}, new String[]{"UpdateMeteringRateAction"});
		
	}
}
