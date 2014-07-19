package org.speedd.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtil {
	private final static Logger log = LoggerFactory.getLogger(TestUtil.class);
	
	public static void startEmbeddedZookeeper(int port) {
		final ZooKeeperServerMain zooKeeperServerMain = new ZooKeeperServerMain();
		
		QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
		
		Properties zkProp = new Properties();
		
		zkProp.put("dataDir", "/tmp/embedded/zookeeper");
		zkProp.put("clientPort", String.valueOf(port));
		zkProp.put("maxClientCnxns", "0");
		
		File logDir = new File("/tmp/embedded/zookeeper");
		if(logDir.exists()){
			deleteFolder(logDir);
		}
		
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
		
	}
	
	/**
	 * utility function to delete a directory
	 * 
	 * @param folder
	 */
	public static void deleteFolder(File folder) {
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
		folder.delete();
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

}
