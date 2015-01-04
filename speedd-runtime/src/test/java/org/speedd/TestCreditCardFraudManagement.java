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
				"speedd-in-events", createProducerConfig(),
				new FraudAggregatedReadingCsv2Event(
						SpeeddEventFactory.getInstance()));

		streamEventsAndVerifyResults("speedd-fraud.properties", "CC Fraud",
				eventReader, new String[] { "FraudAtATM" });
	}

}
