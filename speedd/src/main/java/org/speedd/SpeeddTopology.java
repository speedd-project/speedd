package org.speedd;

import java.util.Properties;

import org.speedd.cep.CEPPlaceholderBolt;
import org.speedd.data.Event;
import org.speedd.dm.DMPlaceholderBolt;
import org.speedd.traffic.TrafficAggregatedReadingScheme;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

public class SpeeddTopology {
	private BrokerHosts brokerHosts;

	public static final String TOPIC_IN_EVENTS = "speedd-in-events";
	public static final String TOPIC_OUT_EVENTS = "speedd-out-events";
	public static final String TOPIC_ACTIONS = "speedd-actions";

	public static final String IN_EVENT_READER = "in-event-reader";

	private static final String EVENT_PROCESSOR = "event-processor";

	private static final String OUT_EVENT_WRITER = "out-event-writer";

	private static final String DECISION_MAKER = "dm";

	private static final String DECISION_WRITER = "decision-writer";
	
	public static final String CONFIG_KEY_OUT_EVENTS_TOPIC = "topic.out.events";
	
	public static final String CONFIG_KEY_ACTIONS_TOPIC = "topic.actions";
	
	public SpeeddTopology(String kafkaZookeeper) {
		brokerHosts = new ZkHosts(kafkaZookeeper);
	}

	private IRichSpout createTrafficReaderSpout() {
		SpoutConfig kafkaConfig = new SpoutConfig(brokerHosts, TOPIC_IN_EVENTS,
				"", IN_EVENT_READER);
		kafkaConfig.scheme = new SchemeAsMultiScheme(
				new TrafficAggregatedReadingScheme());

		return new KafkaSpout(kafkaConfig);
	}

	//FIXME carefully choose grouping strategy
	public StormTopology buildCEPTopology() {
		TopologyBuilder builder = new TopologyBuilder();

		builder.setSpout(IN_EVENT_READER, createTrafficReaderSpout());

		builder.setBolt(EVENT_PROCESSOR, new CEPPlaceholderBolt())
				.shuffleGrouping(IN_EVENT_READER);

		builder.setBolt(OUT_EVENT_WRITER, new KafkaBolt<String, Event>(CONFIG_KEY_OUT_EVENTS_TOPIC))
				.shuffleGrouping(EVENT_PROCESSOR);
		
		builder.setBolt(DECISION_MAKER, new DMPlaceholderBolt()).shuffleGrouping(EVENT_PROCESSOR);
		
		//FIXME Write decision to a separate topic (curr. same as out events) - need to modify KafkaBolt for that		
		builder.setBolt(DECISION_WRITER, new KafkaBolt<String, Event>(CONFIG_KEY_ACTIONS_TOPIC)).shuffleGrouping(DECISION_MAKER);

		return builder.createTopology();
	}

	// FIXME decide on how DM is integrated. 2 ways - a separate topology
	// reading from kafka OR part of same topology reading directly from
	// out-events stream
	public StormTopology buildDMTopology() {
		TopologyBuilder builder = new TopologyBuilder();

		return builder.createTopology();
	}

	public static void main(String[] args) {
		Properties properties = new Properties();
		try {
			properties.load(SpeeddTopology.class.getClassLoader()
					.getResourceAsStream("speedd.properties"));
			System.out.println("Properties loaded:" + properties.toString());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		String zkConnect = (String) properties.get("zookeeper.connect");

		SpeeddTopology speeddTopology = new SpeeddTopology(zkConnect);

		Config conf = new Config();
		conf.setDebug(true);

		conf.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, properties);

//		conf.put("topic", TOPIC_OUT_EVENTS);

		conf.setMaxTaskParallelism(1);

		// FIXME if storm cluster details provided (arguments) - connect to the
		// external cluster
		// FIXME admin capabilities? - shutdown the cluster? needed when working
		// with external cluster?
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("speedd", conf,
				speeddTopology.buildCEPTopology());

		System.out.println("Listening on the topic");
		try {
			while (true) {
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
			System.out.println("Interrupted - exiting.");
		} finally {
			System.out.println("Shutting down the storm cluster");
			cluster.shutdown();
		}

	}
}
