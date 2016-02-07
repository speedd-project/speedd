package org.speedd;

import org.junit.Test;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.fraud.CCFTopology;
import org.speedd.fraud.FraudAggregatedReadingCsv2Event;

public class TestCreditCardFraudManagement extends BaseSpeeddIntegrationTest {
	private static final String TOPOLOGY_NAME = "speedd-ccf-test";

	@Test
	public void testCCF() throws Exception {
		TimedEventFileReader eventReader = new TimedEventFileReader(
				TestCreditCardFraudManagement.class.getClassLoader()
						.getResource("FeedzaiIntegrationData.csv").getPath(),
				"speedd-fraud-in-events", createProducerConfig(),
				new FraudAggregatedReadingCsv2Event(
						SpeeddEventFactory.getInstance()));

		streamEventsAndVerifyResults("speedd-fraud.properties", "CC Fraud",
				eventReader, new String[] { "Transaction", "SuddenCardUseNearExpirationDate" });
	}
	
	@Override
	protected String getTopicName(String topicKey) {
		if(SpeeddRunner.CONFIG_KEY_IN_EVENTS_TOPIC.equals(topicKey))
			return "speedd-fraud-in-events";
		
		if(SpeeddRunner.CONFIG_KEY_OUT_EVENTS_TOPIC.equals(topicKey))
			return "speedd-fraud-out-events";
		
		if(SpeeddRunner.CONFIG_KEY_ACTIONS_TOPIC.equals(topicKey))
			return "speedd-fraud-actions";
		
		if(SpeeddRunner.CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC.equals(topicKey))
			return "speedd-fraud-actions-confirmed";
		
		if(SpeeddRunner.CONFIG_KEY_ADMIN_TOPIC.equals(topicKey))
			return "speedd-fraud-admin";
		
		throw new RuntimeException("Invalid topic key: " + topicKey);
	}
	
	@Override
	protected ISpeeddTopology getTopology() {
		return new CCFTopology();
	}

	@Override
	protected String getTopologyName() {
		return TOPOLOGY_NAME;
	}
}
