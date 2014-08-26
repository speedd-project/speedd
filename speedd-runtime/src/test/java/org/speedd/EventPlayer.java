package org.speedd;

import java.io.File;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.utils.TestUtils;

import org.fest.util.Files;
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
	
	public static void main(String[] args) throws Exception {
		File eventJsonFile = null;
		
		if(args.length > 0){
			eventJsonFile = new File(args[0]);
		}
		else {
			eventJsonFile = new File(TestSpeedTopology.class.getClassLoader().getResource("simple-event.json").toURI()); 
		}
		
		char[] buf = new char[1000];

		String inEventStr = Files.contentOf(eventJsonFile, "UTF-8");

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + 9092, "kafka.producer.DefaultPartitioner");
		
		ProducerConfig pConfig = new ProducerConfig(producerProperties);

		Producer<String, String> producer = new Producer<String, String>(
				pConfig);

		KeyedMessage<String, String> message = new KeyedMessage<String, String>(
				"speedd-in-events", inEventStr);

		producer.send(message);

	}
}
