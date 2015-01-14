package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import kafka.admin.CreateTopicCommand;
import kafka.api.OffsetRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.Time;
import kafka.utils.ZKStringSerializer$;

import org.I0Itec.zkclient.ZkClient;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;
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

import com.netflix.curator.test.TestingServer;

public abstract class BaseSpeeddIntegrationTest {
	private KafkaConfig outEventsKafkaConfig;
	private KafkaConfig actionsKafkaConfig;
	private SimpleConsumer outEventConsumer;
	private SimpleConsumer actionConsumer;
	private int brokerId = 0;
	private int brokerPort;
	private int zookeeperPort;
	private KafkaServer kafkaServer;
	private String kafkaLogDir = null;
	private LocalCluster storm = null;
	private TestingServer kafkaZookeeper;
	private ZkClient zkClient = null;

	private Logger logger;

	@Before
	public void setup() {
		logger = LoggerFactory
				.getLogger(this.getClass());

		logger.info("Setting up a new test");
		
		zookeeperPort = TestUtils.choosePort();

		brokerPort = TestUtils.choosePort();

		logger.info(String.format("zookeeperPort=%d, brokerPort=%d", zookeeperPort, brokerPort));

		try {
			setupKafkaServer();
		} catch (Exception e){
			logger.error("Failed to start kafka server", e);
			fail("Failed to start kafka server");
		}

		setupKafkaConsumers();
		
		storm = new LocalCluster();
		logger.info("Setup test completed.");
	}

	@After
	public void shutdown() throws Exception {
		logger.info("Shutting down the test");
		shutdownConsumers();
		
		if(storm != null){
			logger.info("Shutting down speedd storm");
			storm.killTopology("speedd");
			storm.shutdown();
		}
		
		if (kafkaServer != null)
			logger.info("Shutting down kafka server");
			kafkaServer.shutdown();
		// wait to kafka server to shut down
		 Thread.sleep(5000);

		 if(!TestUtil.deleteFolder(new File(kafkaLogDir))){
			 logger.warn("Could not delete kafka log dir: " + kafkaLogDir);
		 }

		 if(zkClient != null){
			 zkClient.close();
		 }
		 
		 kafkaZookeeper.close();
		 logger.info("Test shut down complete");
	}
	
	private void shutdownConsumers() {
		logger.info("Shutting down kafka consumers");
		if (outEventConsumer != null)
			outEventConsumer.close();

		if (actionConsumer != null)
			actionConsumer.close();
	}

	protected abstract String getTopicName(String topicKey);
	
	private void setupKafkaServer() throws Exception {
		kafkaZookeeper = TestUtil.startEmbeddedZkServer(zookeeperPort);

		// wait till zookeper running
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}

		zkClient = new ZkClient("localhost:" + zookeeperPort, 30000,
				30000, ZKStringSerializer$.MODULE$);

		Properties props = TestUtils.createBrokerConfig(brokerId, brokerPort);

		props.setProperty("zookeeper.connect", "localhost:" + zookeeperPort);

		kafkaLogDir = props.getProperty("log.dir");

		kafka.server.KafkaConfig config = new kafka.server.KafkaConfig(props);
		Time mock = new MockTime();
		kafkaServer = TestUtils.createServer(config, mock);

		// create topics
		CreateTopicCommand.createTopic(zkClient,
				getTopicName(SpeeddTopology.CONFIG_KEY_OUT_EVENTS_TOPIC), 1, 1, "");
		CreateTopicCommand.createTopic(zkClient,
				getTopicName(SpeeddTopology.CONFIG_KEY_IN_EVENTS_TOPIC), 1, 1, "");
		CreateTopicCommand.createTopic(zkClient, getTopicName(SpeeddTopology.CONFIG_KEY_ACTIONS_TOPIC),
				1, 1, "");
		CreateTopicCommand.createTopic(zkClient, getTopicName(SpeeddTopology.CONFIG_KEY_ADMIN_TOPIC),
				1, 1, "");

		List<KafkaServer> servers = new ArrayList<KafkaServer>();
		servers.add(kafkaServer);
		TestUtils.waitUntilMetadataIsPropagated(
				scala.collection.JavaConversions.asScalaBuffer(servers),
				getTopicName(SpeeddTopology.CONFIG_KEY_ACTIONS_TOPIC), 0, 5000);

		logger.info("Kafka server started and initialized");
	}

	protected int getBrokerPort(){
		return brokerPort;
	}
	
	protected SimpleConsumer getOutEventConsumer(){
		return outEventConsumer;
	}
	
	protected SimpleConsumer getActionConsumer(){
		return actionConsumer;
	}
	
	protected KafkaConfig getOutEventKafkaConfig(){
		return outEventsKafkaConfig;
	}
	
	protected KafkaConfig getActionKafkaConfig(){
		return actionsKafkaConfig;
	}
	
	private void setupKafkaConsumers() {
		logger.info("Setting up kafka consumers");

		GlobalPartitionInformation globalPartitionInformation = new GlobalPartitionInformation();
		globalPartitionInformation.addPartition(0,
				Broker.fromString("localhost:" + brokerPort));
		BrokerHosts brokerHosts = new StaticHosts(globalPartitionInformation);

		outEventsKafkaConfig = new KafkaConfig(brokerHosts,
				getTopicName(SpeeddTopology.CONFIG_KEY_OUT_EVENTS_TOPIC));

		outEventConsumer = new SimpleConsumer("localhost", brokerPort, 60000,
				1024, "outEventConsumer");

		logger.info(String
				.format("OutEventsConsumer details: host=%s, brokerPort=%s, clientName=%s",
						outEventConsumer.host(), outEventConsumer.port(),
						outEventConsumer.clientId()));

		actionsKafkaConfig = new KafkaConfig(brokerHosts,
				getTopicName(SpeeddTopology.CONFIG_KEY_ACTIONS_TOPIC));

		actionConsumer = new SimpleConsumer("localhost", brokerPort, 60000,
				1024, "actionConsumer");

		logger.info(String
				.format("ActionsConsumer details: host=%s, brokerPort=%s, clientName=%s",
						actionConsumer.host(), actionConsumer.port(),
						actionConsumer.clientId()));

	}

	/**
	 * Verify that all the events from the @{expected} have been received
	 * @param consumer
	 * @param kafkaConfig
	 * @param expected
	 */
	protected void verifyEvents(SimpleConsumer consumer, KafkaConfig kafkaConfig, String[] expected) {
		logger.info("Verifying event on topic " + kafkaConfig.topic);

		long lastMessageOffset = TestUtil.getLastOffset(consumer,
				kafkaConfig.topic, 0, OffsetRequest.LatestTime() - 1,
				"speeddTest");

		logger.info("Last message offset: " + lastMessageOffset);

		ByteBufferMessageSet messageAndOffsets = KafkaUtils.fetchMessages(
				kafkaConfig, consumer,
				new Partition(Broker.fromString(kafkaZookeeper.getConnectString()),
						0), lastMessageOffset);

		ArrayList<Event> received = new ArrayList<Event>();
		
		for (Iterator<MessageAndOffset> iter = messageAndOffsets.iterator(); iter.hasNext();) {
			MessageAndOffset messageAndOffset = iter.next();

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
			received.add(event);
		}
		
		assertTrue(String.format("There must be at least %d messages", expected.length), received.size() >= expected.length );

		for (String expectedEventName : expected) {
			boolean found = false;
			for (Event receivedEvent : received) {
				if(expectedEventName.equals(receivedEvent.getEventName())){
					found = true;
					break;
				}
			}
			
			assertTrue(String.format("Event %s must be received", expectedEventName), found);
		}
		
	}

	private void verifyEvent(SimpleConsumer consumer, KafkaConfig kafkaConfig, Event expectedEvent) {
		logger.info("Verifying event on topic " + kafkaConfig.topic);

		long lastMessageOffset = TestUtil.getLastOffset(consumer,
				kafkaConfig.topic, 0, OffsetRequest.LatestTime() - 1,
				"speeddTest");

		logger.info("Last message offset: " + lastMessageOffset);

		ByteBufferMessageSet messageAndOffsets = KafkaUtils.fetchMessages(
				kafkaConfig, consumer,
				new Partition(Broker.fromString(kafkaZookeeper.getConnectString()),
						0), lastMessageOffset);

		assertTrue("There must be at least one message", messageAndOffsets
				.iterator().hasNext());

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
		// assertEquals(expectedEvent.getTimestamp(), event.getTimestamp());

		Map<String, Object> expectedAttrs = expectedEvent.getAttributes();

		for (Entry<String, Object> entry : expectedAttrs.entrySet()) {
			String attrName = entry.getKey();
			Object attrValue = entry.getValue();
			assertEquals(expectedAttrs.get(attrName), attrValue);
		}

	}

	protected void startSpeeddTopology(String configPath, String name) {
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getClassLoader()
					.getResourceAsStream(configPath));
			logger.info("Properties loaded:" + properties.toString());
		} catch (Exception e) {
			logger.error("Failed to load configuration properties", e);
			fail("Failed to start SPEEDD topology");
		}

		properties.setProperty("metadata.broker.list", "localhost:"
				+ brokerPort);
		properties.setProperty("zookeeper.connect", "localhost:"
				+ zookeeperPort);

		SpeeddConfig speeddConfiguration = new SpeeddConfig();
		speeddConfiguration.zkConnect = properties
				.getProperty("zookeeper.connect");

		String epnPath = properties.getProperty("proton.epnPath");

		try {
			URI epnUri = this.getClass().getClassLoader().getResource(epnPath)
					.toURI();

			speeddConfiguration.epnPath = Paths.get(epnUri).toAbsolutePath()
					.toString();

		} catch (URISyntaxException e) {
			fail("Cannot read epnPath property as a path: " + e.getMessage());
		}

		speeddConfiguration.inEventScheme = (String) properties
				.getProperty("speedd.inEventScheme");
		
		speeddConfiguration.dmClass = (String) properties
				.getProperty("speedd.dmClass");
		
		speeddConfiguration.topicInEvents = getTopicName(SpeeddTopology.CONFIG_KEY_IN_EVENTS_TOPIC);
		speeddConfiguration.topicOutEvents = getTopicName(SpeeddTopology.CONFIG_KEY_OUT_EVENTS_TOPIC);
		speeddConfiguration.topicActions = getTopicName(SpeeddTopology.CONFIG_KEY_ACTIONS_TOPIC);
		speeddConfiguration.topicActionsConfirmed = getTopicName(SpeeddTopology.CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC);
		speeddConfiguration.topicAdmin = getTopicName(SpeeddTopology.CONFIG_KEY_ADMIN_TOPIC);

		SpeeddTopology speeddTopology = new SpeeddTopology(speeddConfiguration);

		Config stormConfig = new Config();
		stormConfig.setDebug(true);

		stormConfig.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, properties);

		for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
			Entry<String, String> entry = (Entry<String, String>) iter.next();
			if (entry.getKey().startsWith("topic.")) {
				stormConfig.put(entry.getKey(), entry.getValue());
			}
		}

		stormConfig.setMaxTaskParallelism(1);

		storm.submitTopology("speedd", stormConfig,
				speeddTopology.buildTopology());

		logger.info("Submitted topology - should start listening on incoming events");
	}

	public void streamEventsAndVerifyResults(String configPath, String topologyName,EventFileReader eventReader, String[] expectedEvents) throws Exception {
		streamEventsAndVerifyResults(configPath, topologyName, eventReader, expectedEvents, null);
	}

	protected ProducerConfig createProducerConfig(){
		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + getBrokerPort(), "kafka.producer.DefaultPartitioner");

		return new ProducerConfig(producerProperties);
	}
	
	public void streamEventsAndVerifyResults(String configPath, String topologyName, EventFileReader eventReader, String[] expectedEvents, String[] expectedActions) throws Exception {
		// Start a local storm, submit speedd topology
		// send a single event to the speedd-in-events topic
		// verify that both speedd-out-events and speedd-actions topics have
		// events arrived, check contents

		startSpeeddTopology(configPath, topologyName);

		// wait till topology is up
		Thread.sleep(5000);

		eventReader.streamEvents();
		
		// wait till propagation of the message through SPEEDD topology
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}

		if(expectedEvents != null){
			verifyEvents(getOutEventConsumer(), getOutEventKafkaConfig(), expectedEvents);
		}
		
		if(expectedActions != null){
			verifyEvents(getActionConsumer(), getActionKafkaConfig(), expectedActions);
		}
	}

}
