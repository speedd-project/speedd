package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import kafka.admin.CreateTopicCommand;
import kafka.api.OffsetRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.javaapi.producer.Producer;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.Time;
import kafka.utils.ZKStringSerializer$;

import org.I0Itec.zkclient.ZkClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.kafka.JsonEventDecoder;
import org.speedd.test.TestUtil;

import storm.kafka.Broker;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaConfig;
import storm.kafka.KafkaUtils;
import storm.kafka.Partition;
import storm.kafka.StaticHosts;
import storm.kafka.bolt.KafkaBolt;
import storm.kafka.trident.GlobalPartitionInformation;
import backtype.storm.Config;
import backtype.storm.LocalCluster;

public class TestSpeedTopology {
	private KafkaConfig outEventsKafkaConfig;
	private KafkaConfig actionsKafkaConfig;
	private SimpleConsumer outEventConsumer;
	private SimpleConsumer actionConsumer;
	private int brokerId = 0;
	private int brokerPort;
	private int zookeeperPort;
	private KafkaServer kafkaServer;

	private static Logger logger = LoggerFactory.getLogger(TestSpeedTopology.class);
	
	@Before
	public void setup() {
		zookeeperPort = TestUtils.choosePort();
		
		brokerPort = TestUtils.choosePort();
		
		setupKafkaServer();
		
		setupKafkaConsumers();
	}

	@After
	public void shutdown() throws Exception {
		shutdownConsumers();
		kafkaServer.shutdown();
	}

	private void shutdownConsumers() {
		outEventConsumer.close();
		actionConsumer.close();
	}
	
	private void setupKafkaServer() {
		TestUtil.startEmbeddedZookeeper(zookeeperPort);

		//wait till zookeper running
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}

		ZkClient zkClient = new ZkClient("localhost:" + zookeeperPort, 30000, 30000,
				ZKStringSerializer$.MODULE$);

		Properties props = TestUtils.createBrokerConfig(brokerId, brokerPort);
		props.setProperty("zookeeper.connect", "localhost:" + zookeeperPort);

		kafka.server.KafkaConfig config = new kafka.server.KafkaConfig(props);
		Time mock = new MockTime();
		kafkaServer = TestUtils.createServer(config, mock);

		// create topics
		CreateTopicCommand.createTopic(zkClient,
				SpeeddTopology.TOPIC_OUT_EVENTS, 1, 1, "");
		CreateTopicCommand.createTopic(zkClient,
				SpeeddTopology.TOPIC_IN_EVENTS, 1, 1, "");
		CreateTopicCommand.createTopic(zkClient, SpeeddTopology.TOPIC_ACTIONS,
				1, 1, "");

		List<KafkaServer> servers = new ArrayList<KafkaServer>();
		servers.add(kafkaServer);
		TestUtils.waitUntilMetadataIsPropagated(
				scala.collection.JavaConversions.asScalaBuffer(servers),
				SpeeddTopology.TOPIC_ACTIONS, 0, 5000);

		logger.info("Kafka server started and initialized");
	}

	private void setupKafkaConsumers() {
		logger.info("Setting up kafka consumers");
		
		GlobalPartitionInformation globalPartitionInformation = new GlobalPartitionInformation();
		globalPartitionInformation.addPartition(0,
				Broker.fromString("localhost:" + brokerPort));
		BrokerHosts brokerHosts = new StaticHosts(globalPartitionInformation);

		outEventsKafkaConfig = new KafkaConfig(brokerHosts,
				SpeeddTopology.TOPIC_OUT_EVENTS);

		outEventConsumer = new SimpleConsumer("localhost", brokerPort, 60000, 1024,
				"outEventConsumer");
		
		logger.info(String.format(
				"OutEventsConsumer details: host=%s, brokerPort=%s, clientName=%s",
				outEventConsumer.host(), outEventConsumer.port(),
				outEventConsumer.clientId()));


		actionsKafkaConfig = new KafkaConfig(brokerHosts,
				SpeeddTopology.TOPIC_ACTIONS);

		actionConsumer = new SimpleConsumer("localhost", brokerPort, 60000, 1024,
				"actionConsumer");

		logger.info(String.format(
				"ActionsConsumer details: host=%s, brokerPort=%s, clientName=%s",
				actionConsumer.host(), actionConsumer.port(),
				actionConsumer.clientId()));
	
	}

	@Test
	public void executeForSingleTrafficEvent() throws Exception {
		// Start a local cluster, submit speedd topology
		// send a single event to the speedd-in-events topic
		// verify that both speedd-out-events and speedd-actions topics have
		// events arrived, check contents

		startSpeeddTopology();

		// wait till topology is up
		Thread.sleep(5000);

		String trafficReadingCsv = "2014-04-13,08:00:00,0024a4dc0000343e,right,12.16,7,\\N,60.0,0,0,0,0,0,4,0,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,2,3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + brokerPort, "kafka.producer.DefaultPartitioner");

		ProducerConfig pConfig = new ProducerConfig(producerProperties);
		
		Producer<String, String> producer = new Producer<String, String>(pConfig);

		KeyedMessage<String, String> message = new KeyedMessage<String, String>(
				"speedd-in-events", trafficReadingCsv);

		producer.send(message);

		//wait till propagation of the message through SPEEDD topology
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		Map<String, Object> expectedAttrs = new HashMap<String, Object>();
		expectedAttrs.put("location", "0024a4dc0000343e");
		Event expectedEvent = SpeeddEventFactory.getInstance().createEvent("TrafficCongestion", 1397365200000l, expectedAttrs);
		
		verifyEvent(expectedEvent);
	}

	private void verifyEvent(Event expectedEvent) {
		logger.info("Verifying event");
		
		long lastMessageOffset = TestUtil.getLastOffset(outEventConsumer,
				"speedd-out-events", 0, OffsetRequest.LatestTime() - 1,
				"speeddTest");
		
		logger.info("Last message offset: " + lastMessageOffset);

		ByteBufferMessageSet messageAndOffsets = KafkaUtils.fetchMessages(
				outEventsKafkaConfig,
				outEventConsumer,
				new Partition(Broker.fromString("localhost:" + zookeeperPort), 0), lastMessageOffset);

		assertTrue("There must be at least one message", messageAndOffsets.iterator().hasNext());
		
		MessageAndOffset messageAndOffset = messageAndOffsets.iterator().next();

		Message kafkaMessage = messageAndOffset.message();
		
		ByteBuffer payload = kafkaMessage.payload();

		byte[] bytes = new byte[payload.limit()];
		
		payload.get(bytes);
		
	    try {
	    	String eventStr = new String(bytes, "UTF-8");
	    	
	    	logger.info("Event message: " + eventStr);
	    	
		} catch (UnsupportedEncodingException e) {
		}
	    
		Event event = new JsonEventDecoder().fromBytes(bytes);

		assertNotNull("Event must not be null", event);

		assertEquals(expectedEvent.getEventName(), event.getEventName());
		assertEquals(expectedEvent.getTimestamp(), event.getTimestamp());
		
		Map<String, Object> attrs = event.getAttributes();
		Map<String, Object> expectedAttrs = expectedEvent.getAttributes();
		
		for (Entry<String, Object> entry : expectedAttrs.entrySet()) {
			String attrName = entry.getKey();
			Object attrValue = entry.getValue();
			assertEquals(expectedAttrs.get(attrName), attrs.get(attrName));
		}
		
	}

	private void startSpeeddTopology() {
		Properties properties = new Properties();
		try {
			properties.load(SpeeddTopology.class.getClassLoader()
					.getResourceAsStream("speedd.properties"));
			logger.info("Properties loaded:" + properties.toString());
		} catch (Exception e) {
			logger.error("Failed to load configuration properties", e);
			fail("Failed to start SPEEDD topology");
		}

		properties.setProperty("metadata.broker.list", "localhost:" + brokerPort);
		properties.setProperty("zookeeper.connect", "localhost:" + zookeeperPort);
		
		String zkConnect = (String) properties.get("zookeeper.connect");

		SpeeddTopology speeddTopology = new SpeeddTopology(zkConnect);

		Config conf = new Config();
		conf.setDebug(true);

		conf.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, properties);

		for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
			Entry<String, String> entry = (Entry<String, String>) iter.next();
			if (entry.getKey().startsWith("topic.")) {
				conf.put(entry.getKey(), entry.getValue());
			}
		}

		conf.setMaxTaskParallelism(1);

		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("speedd", conf,
				speeddTopology.buildCEPTopology());

		logger.info("Submitted topology - should start listening on incoming events");
	}
}
