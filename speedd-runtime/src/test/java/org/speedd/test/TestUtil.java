package org.speedd.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.admin.CreateTopicCommand;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.Time;
import kafka.utils.ZKStringSerializer$;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.curator.test.TestingServer;
import com.netflix.curator.test.TestingZooKeeperMain;

public class TestUtil {
	private final static Logger log = LoggerFactory.getLogger(TestUtil.class);
	
	public static File createTempDir(String prefix) throws IOException {
		File dir = File.createTempFile(prefix, null);
		
		if(!dir.delete()){
			throw new IOException("Cannot delete temp dir: " + dir.getAbsolutePath());
		}
		
		if(!dir.mkdirs()){
			throw new IOException("Cannot create temp dir: " + dir.getAbsolutePath());
		}
		
		dir.deleteOnExit();
		
		return dir;
	}
	
	public static TestingServer startEmbeddedZkServer(int port) throws Exception {
		File tempDir = createTempDir("testzk-");
		TestingServer zkServer = new TestingServer(port, tempDir);

		return zkServer;
	}
	
	public static TestingZooKeeperMain startEmbeddedZookeeper(int port) {
		final TestingZooKeeperMain zooKeeperServerMain = new TestingZooKeeperMain();
		
		QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
		
		Properties zkProp = new Properties();
		
		File dataDir = null;
		
		try {
			dataDir = createTempDir("zookeeper");
		} catch (IOException e1) {
			log.error("Cannot create data directory for zookeeper: " + e1.getMessage(), e1);
		}
		
		log.info("Zookeeper data directory will be: " + dataDir.getAbsolutePath());
		
		zkProp.put("dataDir", dataDir.getAbsolutePath());
		zkProp.put("clientPort", String.valueOf(port));
		zkProp.put("maxClientCnxns", "0");
		
		
		try {
			quorumConfiguration.parseProperties(zkProp);
		} catch (Exception e) {
			log.error("Parsing zookeeper properties failed", e);
		}
		
		final ServerConfig configuration = new ServerConfig();
		configuration.readFrom(quorumConfiguration);

		new Thread() {
		    public void run() {
		        try {
		            zooKeeperServerMain.runFromConfig(configuration);
		        } catch (IOException e) {
		            log.error("Start zookeeper failed", e);
		        }
		    }
		}.start();
		
		return zooKeeperServerMain;
	}
	
	/**
	 * utility function to delete a directory
	 * 
	 * @param folder
	 */
	public static boolean deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		return folder.delete();
	}

	public static long getLastOffset(SimpleConsumer consumer, String topic,
			int partition, long whichTime, String clientName) {
		TopicAndPartition topicAndPartition = new TopicAndPartition(topic,
				partition);
		Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
		requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(
				whichTime, 1));
		kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(
				requestInfo, kafka.api.OffsetRequest.CurrentVersion(),
				clientName);
		OffsetResponse response = consumer.getOffsetsBefore(request);

		if (response.hasError()) {
			log.error(String.format("Error fetching offset data for the Broker - error code: ", response.errorCode(topic, partition)));
			return 0;
		}
		
		long[] offsets = response.offsets(topic, partition);
		
		return offsets[0];
	}

	public static KafkaServer setupKafkaServer(int brokerId, int brokerPort, TestingServer testingZkServer, String[] topicsToCreate) throws Exception {
		String zkConnect = testingZkServer.getConnectString();
		
		ZkClient zkClient = new ZkClient(zkConnect, 30000,
				30000, ZKStringSerializer$.MODULE$);

		Properties props = TestUtils.createBrokerConfig(brokerId, brokerPort);

		props.setProperty("zookeeper.connect", zkConnect);

		KafkaConfig config = new KafkaConfig(props);
		Time mock = new MockTime();
		
		KafkaServer kafkaServer = TestUtils.createServer(config, mock);

		log.info("Creating topics");
		String lastTopicCreated = null;
		for (String topic : topicsToCreate) {
			log.info("Create topic: " + topic);
			CreateTopicCommand.createTopic(zkClient,
					topic, 1, 1, "");
			lastTopicCreated = topic;
		}

		List<KafkaServer> servers = new ArrayList<KafkaServer>();
		servers.add(kafkaServer);
		
		if(lastTopicCreated != null){
			TestUtils.waitUntilMetadataIsPropagated(
					scala.collection.JavaConversions.asScalaBuffer(servers),
					lastTopicCreated, 0, 5000);
		}

		log.info("Kafka server started and initialized");
		
		return kafkaServer;
	}

}
