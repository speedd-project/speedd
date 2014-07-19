//package org.speedd;
//
//import java.io.File;
//import java.util.Properties;
//
//import kafka.api.FetchRequest;
//import kafka.javaapi.consumer.SimpleConsumer;
//import kafka.javaapi.message.ByteBufferMessageSet;
//import kafka.javaapi.producer.Producer;
//import kafka.message.MessageAndOffset;
//import kafka.producer.ProducerConfig;
//import kafka.server.KafkaConfig;
//import kafka.server.KafkaServer;
//import kafka.utils.MockTime;
//import kafka.utils.Time;
//import kafka.utils.Utils;
//
//public class KafkaLocalBroker {
//
//	// location of kafka logging file:
//	public static final String DEFAULT_LOG_DIR = "/tmp/embedded/kafka/";
//	public static final String LOCALHOST_BROKER = "0:localhost:9092";
//	public static final String TEST_TOPIC = "test-topic";
//
//	public KafkaConfig kafkaConfig;
//	// This is the actual Kafka server:
//	public KafkaServer kafkaServer;
//
//	/**
//	 * default constructor
//	 */
//	public KafkaLocalBroker() {
//		this(DEFAULT_LOG_DIR, 9092, 1);
//	}
//
//	public KafkaLocalBroker(Properties properties) {
//		Time mock = new MockTime();
//
//		kafkaConfig = new KafkaConfig(properties);
//		kafkaServer = new KafkaServer(kafkaConfig, mock);
//		
//		kafkaServer.startup();
//		System.out.println("embedded kafka is up");
//	}
//
//	public KafkaLocalBroker(String logDir, int port, int brokerId) {
//		this(createProperties(logDir, port, brokerId));
//	}
//
//	private static Properties createProperties(String logDir, int port,
//			int brokerId) {
//		Properties properties = new Properties();
//		properties.put("port", port + "");
//		properties.put("brokerid", brokerId + "");
//		properties.put("log.dir", logDir);
//		properties.put("enable.zookeeper", "false");
//		return properties;
//	}
//
//	public void stop() {
//		kafkaServer.shutdown();
//		System.out.println("embedded kafka stop");
//	}
//
//	/**
//	 * a main that tests the embedded kafka
//	 * 
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		// delete old Kafka topic files
//		File logDir = new File(DEFAULT_LOG_DIR + "/"
//				+ TEST_TOPIC + "-0");
//		if (logDir.exists()) {
//			deleteFolder(logDir);
//		}
//
//		// init kafka server and start it:
//		KafkaLocalBroker kafkaLocalBroker = new KafkaLocalBroker();
//
//		// create kafka producer without zookeeper on localhost:
//		Properties props = new Properties();
//		// Use broker.list to bypass zookeeper:
//		props.put("broker.list", LOCALHOST_BROKER);
//		props.put("serializer.class", "kafka.serializer.StringEncoder");
//		ProducerConfig config = new ProducerConfig(props);
//		Producer<String, String> producer = new Producer<String, String>(config);
//
//		// send one message to local kafka server:
//		for (int i = 0; i < 10; i++) {
//			ProducerData<String, String> data = new ProducerData<String, String>(
//					TEST_TOPIC, "test-message" + i);
//			producer.send(data);
//			System.out.println("sent: " + data.getData().get(0));
//		}
//		producer.close();
//
//		// consume one messages from Kafka:
//		SimpleConsumer consumer = new SimpleConsumer("localhost", 9092, 10000,
//				1024000, "");
//		long offset = 0L;
//		while (offset < 160) { // this is an exit criteria just for this test so
//								// we are not stuck in enless loop
//		// create a fetch request for topic “test”, partition 0, current offset,
//		// and fetch size of 1MB
//			FetchRequest fetchRequest = new FetchRequest(TEST_TOPIC, 0, offset,
//					1000000);
//
//			// get the message set from the consumer and print them out
//			ByteBufferMessageSet messages = consumer.fetch(fetchRequest);
//			for (MessageAndOffset msg : messages) {
//				System.out.println("consumed: "
//						+ Utils.toString(msg.message().payload(), "UTF-8"));
//				// advance the offset after consuming each message
//				offset = msg.offset();
//			}
//		}
//
//		// close the consumer
//		consumer.close();
//		// stop the kafka broker:
//		kafkaLocalBroker.stop();
//	}
//
//	/**
//	 * utility function to delete a directory
//	 * 
//	 * @param folder
//	 */
//	public static void deleteFolder(File folder) {
//		File[] files = folder.listFiles();
//		if (files != null) { // some JVMs return null for empty dirs
//			for (File f : files) {
//				if (f.isDirectory()) {
//					deleteFolder(f);
//				} else {
//					f.delete();
//				}
//			}
//		}
//		folder.delete();
//	}
//}