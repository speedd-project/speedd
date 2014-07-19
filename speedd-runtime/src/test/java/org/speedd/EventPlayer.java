package org.speedd;

import java.util.Properties;

import kafka.producer.ProducerConfig;

import org.junit.Test;

public class EventPlayer {
	public static final String EVENT_FILE = "test-events.csv";
	public static final String TOPIC = "speedd-in-events";
	
	@Test
	public void playEventsFromFile() throws Exception {
		Properties properties = new Properties();
		properties.load(SpeeddTopology.class.getClassLoader().getResourceAsStream("producer.properties"));

		System.out.println("Properties loaded:" + properties.toString() );

		ProducerConfig kafkaProducerConfig = new ProducerConfig(properties);
		
		EventFileReader eventFileReader = new EventFileReader(EVENT_FILE, TOPIC, kafkaProducerConfig);
		
		eventFileReader.streamEvents(1000);
		
		System.out.println("Event playback complete.");
	}
}
