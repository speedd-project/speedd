package org.speedd;

import org.junit.Ignore;
import org.junit.Test;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.traffic.TrafficAggregatedReadingCsv2Event;
import org.speedd.traffic.TrafficAimsunReadingCsv2Event;
import org.speedd.traffic.TrafficManagementTopology;

public class TestTrafficManagement extends BaseSpeeddIntegrationTest {
	private static final String TOPOLOGY_NAME = "speedd-traffic-test";

	@Test
//	@Ignore
	public void testCNRS() throws Exception {
		TimedEventFileReader eventReader = new TimedEventFileReader(TestTrafficManagement.class.getClassLoader().getResource("simulator_data_incident_short.csv").getPath(), "speedd-traffic-in-events", createProducerConfig(), new TrafficAimsunReadingCsv2Event(SpeeddEventFactory.getInstance()));
		//EventFileReader eventReader = new EventFileReader(TestTrafficManagement.class.getClassLoader().getResource("inputCNRS.csv").getPath(), "speedd-in-events", createProducerConfig(), 1000);

		streamEventsAndVerifyResults("speedd-traffic.properties", "traffic", eventReader, new String[]{/*"Congestion", */"AverageDensityAndSpeedPerLocation"}, new String[]{});
		
	}

	@Override
	protected String getTopicName(String topicKey) {
		if(SpeeddRunner.CONFIG_KEY_IN_EVENTS_TOPIC.equals(topicKey))
			return "speedd-traffic-in-events";
		
		if(SpeeddRunner.CONFIG_KEY_OUT_EVENTS_TOPIC.equals(topicKey))
			return "speedd-traffic-out-events";
		
		if(SpeeddRunner.CONFIG_KEY_ACTIONS_TOPIC.equals(topicKey))
			return "speedd-traffic-actions";
		
		if(SpeeddRunner.CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC.equals(topicKey))
			return "speedd-traffic-actions-confirmed";
		
		if(SpeeddRunner.CONFIG_KEY_ADMIN_TOPIC.equals(topicKey))
			return "speedd-traffic-admin";
		
		throw new RuntimeException("Invalid topic key: " + topicKey);
	}

	@Override
	protected ISpeeddTopology getTopology() {
		return new TrafficManagementTopology();
	}
	
	@Override
	protected String getTopologyName() {
		return TOPOLOGY_NAME;
	}
}
