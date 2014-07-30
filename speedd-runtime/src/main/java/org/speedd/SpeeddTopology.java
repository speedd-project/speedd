package org.speedd;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.cep.ProtonOutputConsumerBolt;
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
import backtype.storm.spout.Scheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichSpout;

import com.ibm.hrl.proton.ProtonTopologyBuilder;

public class SpeeddTopology {
	private BrokerHosts brokerHosts;
	
	private SpeeddConfig speeddConfig;

	public static final String TOPIC_IN_EVENTS = "speedd-in-events";
	public static final String TOPIC_OUT_EVENTS = "speedd-out-events";
	public static final String TOPIC_ACTIONS = "speedd-actions";

	public static final String IN_EVENT_READER = "in-event-reader";

	private static final String EVENT_PROCESSOR = "event-processor";

	private static final String OUT_EVENT_WRITER = "out-event-writer";
	
	private static final String CEP_EVENT_CONSUMER = "cep-event-consumer";

	private static final String DECISION_MAKER = "dm";

	private static final String DECISION_WRITER = "decision-writer";
	
	public static final String CONFIG_KEY_OUT_EVENTS_TOPIC = "topic.out.events";
	
	public static final String CONFIG_KEY_ACTIONS_TOPIC = "topic.actions";
	
	private static Logger logger = LoggerFactory.getLogger(SpeeddTopology.class); 
	
	public SpeeddTopology(SpeeddConfig configuration) {
		this.speeddConfig = configuration;
		brokerHosts = new ZkHosts(speeddConfig.zkConnect);
	}

	private BaseRichSpout createInputEventReaderSpout(String schemeClassName) {
		SpoutConfig kafkaConfig = new SpoutConfig(brokerHosts, TOPIC_IN_EVENTS,
				"", IN_EVENT_READER);
		try {
			//schemeClassName = TrafficAggregatedReadingScheme.class.getCanonicalName();
			Class<? extends Scheme> clazz = (Class<Scheme>)SpeeddTopology.class.getClassLoader().loadClass(schemeClassName);
			//Class<? extends Scheme> clazz = (Class<Scheme>)Class.forName(schemeClassName, true, SpeeddTopology.class.getClassLoader());
			kafkaConfig.scheme = new SchemeAsMultiScheme(clazz.newInstance());
		} catch (Exception e) {
			throw new RuntimeException("Creating a scheme instance failed", e);
		}

		return new KafkaSpout(kafkaConfig);
	}

	//FIXME carefully choose grouping strategy
	public StormTopology buildTopology() {
		TopologyBuilder builder = new TopologyBuilder();

		BaseRichSpout trafficReaderSpout;

		trafficReaderSpout = createInputEventReaderSpout(speeddConfig.inEventScheme);
		
		builder.setSpout(IN_EVENT_READER, trafficReaderSpout);

		KafkaBolt<String, Event> eventWriterBolt = new KafkaBolt<String, Event>(CONFIG_KEY_OUT_EVENTS_TOPIC);
		
		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();
		
		protonTopologyBuilder.buildProtonTopology(builder, trafficReaderSpout, protonOutputConsumerBolt, CEP_EVENT_CONSUMER, speeddConfig.epnPath);

		builder.setBolt(OUT_EVENT_WRITER, eventWriterBolt).shuffleGrouping(CEP_EVENT_CONSUMER);
//		
//		builder.setBolt(EVENT_PROCESSOR, new CEPPlaceholderBolt())
//				.shuffleGrouping(IN_EVENT_READER);

//		builder.setBolt(OUT_EVENT_WRITER, new KafkaBolt<String, Event>(CONFIG_KEY_OUT_EVENTS_TOPIC))
//				.shuffleGrouping(EVENT_PROCESSOR);

//FIXME add DM to the topology		
//		builder.setBolt(DECISION_MAKER, new DMPlaceholderBolt()).shuffleGrouping(EVENT_PROCESSOR);
//		
//		//FIXME Write decision to a separate topic (curr. same as out events) - need to modify KafkaBolt for that		
//		builder.setBolt(DECISION_WRITER, new KafkaBolt<String, Event>(CONFIG_KEY_ACTIONS_TOPIC)).shuffleGrouping(DECISION_MAKER);
//
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

		
		SpeeddConfig configuration = new SpeeddConfig();
		configuration.zkConnect = (String) properties.getProperty("zookeeper.connect");
		configuration.epnPath = (String) properties.getProperty("proton.epnPath");
		configuration.inEventScheme = (String) properties.getProperty("sppeedd.inEventScheme");

		SpeeddTopology speeddTopology = new SpeeddTopology(configuration);

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
				speeddTopology.buildTopology());

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
