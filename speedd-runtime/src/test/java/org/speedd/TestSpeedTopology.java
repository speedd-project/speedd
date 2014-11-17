package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
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
import org.junit.Ignore;
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

import com.netflix.curator.test.TestingServer;

public class TestSpeedTopology {
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

	private static Logger logger = LoggerFactory
			.getLogger(TestSpeedTopology.class);

	@Before
	public void setup() {
		logger.info("Setting up a new test");
		zookeeperPort = TestUtils.choosePort();

		brokerPort = TestUtils.choosePort();

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

//		 FIXME - either uncomment or remove depending whether needed to manually kill the daemon threads left from previous storm run
//		 ThreadGroup rootThreadGroup = getRootThreadGroup();
//		 
//		 Thread[] daemons = getAllDaemonThreads();
//		 
//		 for (Thread thread : daemons) {
//			String name = thread.getName();
//			String clazz = thread.getClass().getCanonicalName();
//			logger.info("Daemon named " + name + " of class " + clazz);
//			if(clazz.startsWith("org.apache.zookeeper.ClientCnxn")){
//				thread.stop();
//			}
//		 }
	}

//	ThreadGroup getRootThreadGroup( ) {
//	     ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
//	     ThreadGroup ptg;
//	     while ( (ptg = tg.getParent( )) != null )
//	         tg = ptg;
//	     return tg;
//	 }
//
//	Thread[] getAllThreads( ) {
//	    final ThreadGroup root = getRootThreadGroup( );
//	    final ThreadMXBean thbean = ManagementFactory.getThreadMXBean( );
//	    int nAlloc = thbean.getThreadCount( );
//	    int n = 0;
//	    Thread[] threads;
//	    do {
//	        nAlloc *= 2;
//	        threads = new Thread[ nAlloc ];
//	        n = root.enumerate( threads, true );
//	    } while ( n == nAlloc );
//	    return java.util.Arrays.copyOf( threads, n );
//	}
//	
//	Thread[] getAllDaemonThreads( ) {
//	    final Thread[] allThreads = getAllThreads( );
//	    final Thread[] daemons = new Thread[allThreads.length];
//	    int nDaemon = 0;
//	    for ( Thread thread : allThreads )
//	        if ( thread.isDaemon( ) )
//	            daemons[nDaemon++] = thread; 
//	    return java.util.Arrays.copyOf( daemons, nDaemon );
//	}
	
	private void shutdownConsumers() {
		logger.info("Shutting down kafka consumers");
		if (outEventConsumer != null)
			outEventConsumer.close();

		if (actionConsumer != null)
			actionConsumer.close();
	}

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

		outEventConsumer = new SimpleConsumer("localhost", brokerPort, 60000,
				1024, "outEventConsumer");

		logger.info(String
				.format("OutEventsConsumer details: host=%s, brokerPort=%s, clientName=%s",
						outEventConsumer.host(), outEventConsumer.port(),
						outEventConsumer.clientId()));

		actionsKafkaConfig = new KafkaConfig(brokerHosts,
				SpeeddTopology.TOPIC_ACTIONS);

		actionConsumer = new SimpleConsumer("localhost", brokerPort, 60000,
				1024, "actionConsumer");

		logger.info(String
				.format("ActionsConsumer details: host=%s, brokerPort=%s, clientName=%s",
						actionConsumer.host(), actionConsumer.port(),
						actionConsumer.clientId()));

	}

	@Test
	@Ignore
	public void executeForSingleTrafficEvent() throws Exception {
		// Start a local storm, submit speedd topology
		// send a single event to the speedd-in-events topic
		// verify that both speedd-out-events and speedd-actions topics have
		// events arrived, check contents

		startSpeeddTopology("speedd-traffic.properties", "single-traffic-topology");

		// wait till topology is up
		Thread.sleep(5000);

		String trafficReadingCsv = "2014-04-13,08:00:00,0024a4dc0000343e,right,12.16,7,\\N,60.0,0,0,0,0,0,4,0,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,2,3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + brokerPort, "kafka.producer.DefaultPartitioner");

		ProducerConfig pConfig = new ProducerConfig(producerProperties);

		Producer<String, String> producer = new Producer<String, String>(
				pConfig);

		KeyedMessage<String, String> message = new KeyedMessage<String, String>(
				"speedd-in-events", trafficReadingCsv);

		producer.send(message);

		// wait till propagation of the message through SPEEDD topology
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		Map<String, Object> expectedAttrs = new HashMap<String, Object>();
		expectedAttrs.put("location", "0024a4dc0000343e");
		Event expectedEvent = SpeeddEventFactory.getInstance().createEvent(
				"TrafficCongestion", 1397365200000l, expectedAttrs);

		verifyEvent(outEventConsumer, outEventsKafkaConfig, expectedEvent);
	}

//	@Test
//	@Ignore
//	public void verifyNodeExistsIssueSolved() throws Exception {
//		shutdown();
//
//		for (int i = 0; i < 5; ++i) {
//			logger.info("PASS #" + i);
//			setup();
//			testProtonIntegrationOnSimpleEvent();
//			shutdown();
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//
//			}
//		}
//
//	}

	@Test
	@Ignore //FIXME - enable after resolving multiple EPN run issue
	public void testProtonIntegrationOnSimpleEvent() throws Exception {
		// Start a local storm, submit speedd topology
		// send a single event to the speedd-in-events topic
		// verify that both speedd-out-events and speedd-actions topics have
		// events arrived, check contents

		startSpeeddTopology("speedd-simple.properties", "simple-test");

		// wait till topology is up
		Thread.sleep(5000);

		char[] buf = new char[1000];

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				TestSpeedTopology.class.getClassLoader().getResourceAsStream(
						"simple-event.json")));

		int length = reader.read(buf);

		reader.close();

		String inEventStr = new String(buf, 0, length);

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + brokerPort, "kafka.producer.DefaultPartitioner");

		ProducerConfig pConfig = new ProducerConfig(producerProperties);

		Producer<String, String> producer = new Producer<String, String>(
				pConfig);

		KeyedMessage<String, String> message = new KeyedMessage<String, String>(
				"speedd-in-events", inEventStr);

		producer.send(message);

		// wait till propagation of the message through SPEEDD topology
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}

		Map<String, Object> expectedAttrs = new HashMap<String, Object>();
		expectedAttrs.put("A1", 80.0);
		Event expectedEvent = SpeeddEventFactory.getInstance().createEvent(
				"OutputEvent", 0, expectedAttrs);

		verifyEvent(outEventConsumer, outEventsKafkaConfig, expectedEvent);
		
		
		expectedAttrs = new HashMap<String, Object>();
		expectedAttrs.put("decision", "do something");
		Event expectedAction = SpeeddEventFactory.getInstance().createEvent("Action", 0, expectedAttrs);
		verifyEvent(actionConsumer, actionsKafkaConfig, expectedAction);
	}

	@Test
	public void testCNRS() throws Exception {
		// Start a local storm, submit speedd topology
		// send a single event to the speedd-in-events topic
		// verify that both speedd-out-events and speedd-actions topics have
		// events arrived, check contents

		startSpeeddTopology("speedd-traffic.properties", "traffic");

		// wait till topology is up
		Thread.sleep(5000);

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + brokerPort, "kafka.producer.DefaultPartitioner");

		ProducerConfig pConfig = new ProducerConfig(producerProperties);

		EventFileReader eventReader = new EventFileReader(TestSpeedTopology.class.getClassLoader().getResource("inputCNRS.csv").getPath(), "speedd-in-events", pConfig, 1000);
		eventReader.streamEvents();
		
		// wait till propagation of the message through SPEEDD topology
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}

		verifyEvents(outEventConsumer, outEventsKafkaConfig, new String[]{"PredictedCongestion"});
	}

	/**
	 * Verify that all the events from the @{expected} have been received
	 * @param consumer
	 * @param kafkaConfig
	 * @param expected
	 */
	private void verifyEvents(SimpleConsumer consumer, KafkaConfig kafkaConfig, String[] expected) {
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

	private void startSpeeddTopology(String configPath, String name) {
		Properties properties = new Properties();
		try {
			properties.load(TestSpeedTopology.class.getClassLoader()
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
}
