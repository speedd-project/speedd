package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.server.KafkaServer;
import kafka.utils.TestUtils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.EventFileReader.EventMessageRecord;
import org.speedd.EventFileReader.Statistics;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.test.TestUtil;
import org.speedd.traffic.TrafficAggregatedReadingCsv2Event;

import com.netflix.curator.test.TestingServer;

public class TimedEventFileReaderTest {
	private int brokerId = 0;
	private String topic = "test";
	Logger logger = LoggerFactory
			.getLogger(TimedEventFileReaderTest.class.getName());

	private static class TestConsumer implements Runnable {
		private KafkaStream<byte[], byte[]> stream;
		private int threadNumber;
		private Logger logger = LoggerFactory.getLogger(getClass().getName());
		private AtomicInteger count = new AtomicInteger();

		public TestConsumer(KafkaStream<byte[], byte[]> aStream, int aThreadNumber) {
			threadNumber = aThreadNumber;
			stream = aStream;
		}

		public void run() {
			ConsumerIterator<byte[], byte[]> it = stream.iterator();
			
			while (it.hasNext()) {
				this.logger.info("Thread " + threadNumber + ": "
						+ new String(it.next().message()));
				this.logger.info("Count: " + count.incrementAndGet());
			}
		}

		public void verify() {
			assertEquals(10, count.intValue());
		}
	}
	
	private static class TimedEventListener implements EventFileReader.EventListener {
		private long expectedDelays[];
		
		private long actualDelays[];
		
		private int i = 0;
		
		public void setExpectedDelays(long values[]){
			expectedDelays = Arrays.copyOf(values, values.length);
			actualDelays = new long[values.length];
		}
		
		@Override
		public void onEvent(EventMessageRecord eventMessageRecord) {
			actualDelays[i++] = eventMessageRecord.sendDelayMillis;
		}
		
		public void verifyDelays(){
			for (int j=0; j<expectedDelays.length; ++j) {
				assertEquals("Message send delays do not match for index: " + j, expectedDelays[j], actualDelays[j]);
			}
		}
	}

	@Test
	public void testSendingTimedEvents() throws Exception {
		int zookeeperPort = TestUtils.choosePort();
		
		String zkConnect = "localhost:" + zookeeperPort; 
		
		TestingServer zkServer = TestUtil.startEmbeddedZkServer(zookeeperPort);
		Thread.sleep(5000);
		
		zkConnect = zkServer.getConnectString();
		
		// setup Broker
		int kafkaBrokerPort = TestUtils.choosePort();

		KafkaServer kafkaServer = TestUtil.setupKafkaServer(brokerId, kafkaBrokerPort, zkServer, new String[]{"test"});

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig("localhost:" + kafkaBrokerPort);
		producerProperties.put("bootstrap.servers", "localhost:"
				+ kafkaBrokerPort);
		producerProperties.put("key.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		producerProperties.put("value.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");

		TimedEventFileReader eventFileReader = new TimedEventFileReader(this.getClass().getClassLoader().getResource("test-events.csv").getPath(), topic, producerProperties, new TrafficAggregatedReadingCsv2Event(SpeeddEventFactory.getInstance()));
		
		TimedEventListener eventListener = new TimedEventListener();
		
		eventListener.setExpectedDelays(new long[] {0,2000,0,1000,2000,5000,2000,1000,0,0});
		
		eventFileReader.addListener(eventListener);

		eventFileReader.streamEvents();
		
		Statistics stats = eventFileReader.getStatistics();
		
		assertNotNull("Statistics must not be null", stats);
		assertEquals(10, stats.getNumOfAttempts());
		assertEquals(10, stats.getNumOfSent());
		assertEquals(0, stats.getNumOfFailed());
		assertTrue(stats.isFinished());
		assertTrue("Elapsed time must be a positive number", stats.getElapsedTimeMilliseconds() > 0);	
		
		Properties consumerProperties = TestUtils.createConsumerProperties(zkConnect, "group1", "consumer1", -1);
		ConsumerConfig consumerConfig = new ConsumerConfig(consumerProperties);

		ConsumerConnector consumer = Consumer
				.createJavaConsumerConnector(consumerConfig);
		
		Map<String, Integer> topicMap = new HashMap<String, Integer>();
		topicMap.put(topic, 1);
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicMap);

		List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
		assertEquals("Supposed to be a single stream", 1, streams.size());

		KafkaStream<byte[], byte[]> stream = streams.get(0);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		TestConsumer c = new TestConsumer(stream, 1);
		
		executor.submit(c);

		executor.awaitTermination(5, TimeUnit.SECONDS);

		executor.shutdown();

		c.verify();

		consumer.shutdown();
		kafkaServer.shutdown();
		zkServer.stop();
		zkServer.close();
		
		eventListener.verifyDelays();
	}
}
