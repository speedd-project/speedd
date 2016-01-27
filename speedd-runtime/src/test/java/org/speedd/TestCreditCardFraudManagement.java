package org.speedd;

import org.junit.Test;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.fraud.FraudAggregatedReadingCsv2Event;

public class TestCreditCardFraudManagement extends BaseSpeeddIntegrationTest {
	@Test
	public void testCCF() throws Exception {
		TimedEventFileReader eventReader = new TimedEventFileReader(
				TestCreditCardFraudManagement.class.getClassLoader()
						.getResource("FeedzaiIntegrationData.csv").getPath(),
				"speedd-fraud-in-events", createProducerConfig(),
				new FraudAggregatedReadingCsv2Event(
						SpeeddEventFactory.getInstance()));

		streamEventsAndVerifyResults("speedd-fraud.properties", "CC Fraud",
				eventReader, new String[] { "FraudAtATM" });
	}
	
	@Override
	protected String getTopicName(String topicKey) {
		if(SpeeddTopology.CONFIG_KEY_IN_EVENTS_TOPIC.equals(topicKey))
			return "speedd-fraud-in-events";
		
		if(SpeeddTopology.CONFIG_KEY_OUT_EVENTS_TOPIC.equals(topicKey))
			return "speedd-fraud-out-events";
		
		if(SpeeddTopology.CONFIG_KEY_ACTIONS_TOPIC.equals(topicKey))
			return "speedd-fraud-actions";
		
		if(SpeeddTopology.CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC.equals(topicKey))
			return "speedd-fraud-actions-confirmed";
		
		if(SpeeddTopology.CONFIG_KEY_ADMIN_TOPIC.equals(topicKey))
			return "speedd-fraud-admin";
		
		throw new RuntimeException("Invalid topic key: " + topicKey);
	}

}
