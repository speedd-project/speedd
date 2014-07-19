package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import kafka.admin.CreateTopicCommand;
import kafka.admin.DeleteTopicCommand;
import kafka.admin.ListTopicCommand;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.TestZKUtils;
import kafka.utils.Time;
import kafka.utils.ZKStringSerializer$;
import kafka.zk.EmbeddedZookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.test.TestUtil;

public class EventFileReaderTest {
	private int brokerId = 0;
	private String topic = "test";
	Logger logger = LoggerFactory
			.getLogger(EventFileReaderTest.class.getName());

	private static class TestConsumer implements Runnable {
		private KafkaStream stream;
		private int threadNumber;
		Logger logger = LoggerFactory.getLogger(getClass().getName());
		private AtomicBoolean passed = new AtomicBoolean(false);

		public TestConsumer(KafkaStream aStream, int aThreadNumber) {
			threadNumber = aThreadNumber;
			stream = aStream;
		}

		public void run() {
			ConsumerIterator<byte[], byte[]> it = stream.iterator();
			while (it.hasNext()) {
				System.out.println("Thread " + threadNumber + ": "
						+ new String(it.next().message()));
				passed.set(true);
			}
			logger.info("Shutting down consumer thread: " + threadNumber);
		}

		public boolean isPassed() {
			return passed.get();
		}
	}

	@Test
	public void producerTest() throws Exception {

		// setup Zookeeper
		String zkConnect = TestZKUtils.zookeeperConnect();

		TestUtil.startEmbeddedZookeeper(2181);
		Thread.sleep(5000);
		
//		EmbeddedZookeeper zkServer = new EmbeddedZookeeper(zkConnect);
		
//		ZkClient zkClient = new ZkClient(zkServer.connectString(), 30000,
//				30000, ZKStringSerializer$.MODULE$);

		ZkClient zkClient = new ZkClient("localhost:2181", 30000,
		30000, ZKStringSerializer$.MODULE$);

		// setup Broker
		int port = TestUtils.choosePort();
		Properties props = TestUtils.createBrokerConfig(brokerId, port);

		props.setProperty("zookeeper.connect", "localhost:2181");
		
		KafkaConfig config = new KafkaConfig(props);
		
		Time mock = new MockTime();
		KafkaServer kafkaServer = TestUtils.createServer(config, mock);

		DeleteTopicCommand.main(new String[]{"--topic", topic, "--zookeeper", "localhost:2181"});
		
		// create topic
		CreateTopicCommand.createTopic(zkClient, topic, 1, 1, "");

		List<KafkaServer> servers = new ArrayList<KafkaServer>();
		servers.add(kafkaServer);
		TestUtils.waitUntilMetadataIsPropagated(
				scala.collection.JavaConversions.asScalaBuffer(servers), topic,
				0, 5000);

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + port, "kafka.producer.DefaultPartitioner");

		ProducerConfig pConfig = new ProducerConfig(producerProperties);
		Producer producer = new Producer(pConfig);

		// send message
		KeyedMessage<Integer, String> data = new KeyedMessage(topic,
				"test-message");

		List<KeyedMessage> messages = new ArrayList<KeyedMessage>();
		messages.add(data);

		producer.send(messages);

		// cleanup
		producer.close();

		EventFileReader eventFileReader = new EventFileReader(
				"test-events.csv", topic, pConfig);

		eventFileReader.streamEvents(1000);

//		Properties consumerProperties = TestUtils.createConsumerProperties(zkServer.connectString(), "group1", "consumer1", -1);

		Properties consumerProperties = TestUtils.createConsumerProperties("localhost:2181", "group1", "consumer1", -1);
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

		Thread.sleep(3000);

		executor.shutdown();

		assertTrue("Must pass", c.isPassed());

		consumer.shutdown();

		kafkaServer.shutdown();
		zkClient.close();
//		zkServer.shutdown();

	}
}
