package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.test.TestUtil;

import scala.sys.Prop;

import com.netflix.curator.test.TestingServer;

public class EventFileReaderTest {
	private int brokerId = 0;
	private String topic = "test";
	Logger logger = LoggerFactory
			.getLogger(EventFileReaderTest.class.getName());


	/*
	 * TestEventConsumer uses the new consumer client API which is not implemented
	 * in v0.8.2.1. As it becomes available (planned in v0.9) we'll switch to using the new API
	 */
	private static class TestEventConsumer implements Runnable {
		private KafkaConsumer<String, String> consumer;
		private String topic;
		private Logger log;
		private AtomicInteger count; 

		public TestEventConsumer(Properties consumerConfig, String topic) {
			log = LoggerFactory.getLogger(this.getClass());
			
			this.topic = topic;

			consumer = new KafkaConsumer<String, String>(consumerConfig);
			consumer.subscribe(topic);
			
			count = new AtomicInteger();
		}

		public void run() {
			log.info("TestEventConsumer - start");
			Map<String, ConsumerRecords<String, String>> records = consumer
					.poll(1000);
			for (ConsumerRecord<String, String> consumerRecord : records.get(
					topic).records()) {
				try {
					count.incrementAndGet();
					log.info("consumer record {"
							+ consumerRecord.key() + ", "
							+ consumerRecord.value() + "}");
				} catch (Exception e) {
					log.error("Cannot read key or value consumer record", e);
				}
			}

			consumer.close();
			log.info("TestEventConsumer - completed");
		}
		
		public int getCount() {
			return count.intValue();
		}
	}

	private static class TestConsumer implements Runnable {
		private KafkaStream<byte[], byte[]> stream;
		private int threadNumber;
		private Logger logger = LoggerFactory.getLogger(getClass().getName());
		private AtomicInteger count = new AtomicInteger();

		public TestConsumer(KafkaStream<byte[], byte[]> aStream,
				int aThreadNumber) {
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

		public int getCount() {
			return count.intValue();
		}
	}

	@Test
	public void producerTest() throws Exception {
		int zookeeperPort = TestUtils.choosePort();

		String zkConnect = "localhost:" + zookeeperPort;

		TestingServer zkServer = TestUtil.startEmbeddedZkServer(zookeeperPort);
		Thread.sleep(5000);

		zkConnect = zkServer.getConnectString();

		// setup Broker
		int kafkaBrokerPort = TestUtils.choosePort();

		KafkaServer kafkaServer = TestUtil.setupKafkaServer(brokerId,
				kafkaBrokerPort, zkServer, new String[] { "test" });

		// setup producer
		Properties producerProperties = TestUtils
				.getProducerConfig("localhost:" + kafkaBrokerPort);
		producerProperties.put("bootstrap.servers", "localhost:"
				+ kafkaBrokerPort);
		producerProperties.put("key.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		producerProperties.put("value.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");

		EventFileReader eventFileReader = new EventFileReader(this.getClass()
				.getClassLoader().getResource("test-events.csv").getPath(),
				topic, producerProperties);

		eventFileReader.streamEvents();

		Properties consumerProperties = TestUtils.createConsumerProperties(
		zkConnect, "group1", "consumer1", -1);
		ConsumerConfig consumerConfig = new
		ConsumerConfig(consumerProperties);
		
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
		
		assertEquals(10, c.getCount());
		
		consumer.shutdown();
		
		System.out.println("Consumer shut down");

//TODO uncomment for starting using the new KafkaConsumer API
//		Properties props = new Properties();
//		props.put("bootstrap.servers", "localhost:" + kafkaBrokerPort);
//		props.put("group.id", "test");
//		props.put("session.timeout.ms", "1000");
//		props.put("auto.commit.enable", "true");
//		props.put("auto.commit.interval.ms", "10000");
//		props.put("key.deserializer", StringDeserializer.class.getName());
//		props.put("value.deserializer", StringDeserializer.class.getName());
//		props.put("partition.assignment.strategy", "roundrobin");
//
//		TestEventConsumer tec = new TestEventConsumer(props, topic);
//
//		ExecutorService executor = Executors.newSingleThreadExecutor();
//
//		executor.submit(tec);
//
//		executor.awaitTermination(20, TimeUnit.SECONDS);

//		executor.shutdown();

		kafkaServer.shutdown();
		
		zkServer.stop();
		zkServer.close();
		
		System.out.println("Stopped");
	}
}
