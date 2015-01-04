package org.speedd;

import org.junit.Test;

public class TestTrafficManagement extends BaseSpeeddIntegrationTest {
	@Test
	public void testCNRS() throws Exception {
		EventFileReader eventReader = new EventFileReader(TestTrafficManagement.class.getClassLoader().getResource("inputCNRS.csv").getPath(), "speedd-in-events", createProducerConfig(), 1000);

		streamEventsAndVerifyResults("speedd-traffic.properties", "traffic", eventReader, new String[]{"PredictedCongestion"});
	}
}
