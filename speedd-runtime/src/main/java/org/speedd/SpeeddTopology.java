package org.speedd;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.cep.ProtonOutputConsumerBolt;
import org.speedd.data.Event;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.Scheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichSpout;

import com.ibm.hrl.proton.ProtonTopologyBuilder;

public class SpeeddTopology {
	private BrokerHosts brokerHosts;

	private SpeeddConfig speeddConfig;

	public static final String TOPIC_IN_EVENTS = "speedd-in-events";
	public static final String TOPIC_OUT_EVENTS = "speedd-out-events";
	public static final String TOPIC_ACTIONS = "speedd-actions";
	public static final String TOPIC_ADMIN = "speedd-admin";

	public static final String ADMIN_COMMAND_READER = "admin-command-reader";

	public static final String IN_EVENT_READER = "in-event-reader";

	private static final String OUT_EVENT_WRITER = "out-event-writer";

	private static final String CEP_EVENT_CONSUMER = "cep-event-consumer";

	private static final String DECISION_MAKER = "dm";

	private static final String DECISION_WRITER = "decision-writer";

	public static final String CONFIG_KEY_OUT_EVENTS_TOPIC = "topic.out.events";

	public static final String CONFIG_KEY_ACTIONS_TOPIC = "topic.actions";

	private static Logger logger = LoggerFactory
			.getLogger(SpeeddTopology.class);

	public SpeeddTopology(SpeeddConfig configuration) {
		this.speeddConfig = configuration;
		brokerHosts = new ZkHosts(speeddConfig.zkConnect);

		logger.info("EPN Path: " + configuration.epnPath);
	}

	private BaseRichSpout createKafkaReaderSpout(String schemeClassName,
			String topic, String spoutId) {
		SpoutConfig kafkaConfig = new SpoutConfig(brokerHosts, topic, "",
				spoutId);
		try {
			Class<? extends Scheme> clazz = (Class<Scheme>) SpeeddTopology.class
					.getClassLoader().loadClass(schemeClassName);
			kafkaConfig.scheme = new SchemeAsMultiScheme(clazz.newInstance());
		} catch (Exception e) {
			throw new RuntimeException("Creating a scheme instance failed", e);
		}

		return new KafkaSpout(kafkaConfig);
	}

	// FIXME carefully choose grouping strategy
	public StormTopology buildTopology() {
		TopologyBuilder builder = new TopologyBuilder();

		BaseRichSpout trafficReaderSpout = createKafkaReaderSpout(
				speeddConfig.inEventScheme, TOPIC_IN_EVENTS, IN_EVENT_READER);

		BaseRichSpout adminSpout = createKafkaReaderSpout(
				AdminCommandScheme.class.getName(), TOPIC_ADMIN,
				ADMIN_COMMAND_READER);

		KafkaBolt<String, Event> eventWriterBolt = new KafkaBolt<String, Event>(
				CONFIG_KEY_OUT_EVENTS_TOPIC);

		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();

		protonTopologyBuilder.buildProtonTopology(builder, trafficReaderSpout,
				protonOutputConsumerBolt, CEP_EVENT_CONSUMER,
				speeddConfig.epnPath);

		builder.setBolt(OUT_EVENT_WRITER, eventWriterBolt).shuffleGrouping(
				CEP_EVENT_CONSUMER);

		builder.setSpout(ADMIN_COMMAND_READER, adminSpout)
				.setMaxTaskParallelism(1);

		IRichBolt dmBolt = createDecisionMakerBolt(speeddConfig.dmClass);
		// @FIXME distribute output events according to the use-case specific
		// grouping strategy
		builder.setBolt(DECISION_MAKER, dmBolt)
				.shuffleGrouping(CEP_EVENT_CONSUMER)
				.allGrouping(ADMIN_COMMAND_READER);

		builder.setBolt(DECISION_WRITER,
				new KafkaBolt<String, Event>(CONFIG_KEY_ACTIONS_TOPIC))
				.shuffleGrouping(DECISION_MAKER);

		return builder.createTopology();
	}

	public IRichBolt createDecisionMakerBolt(String dmBoltClassName) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends IRichBolt> clazz = (Class<IRichBolt>) SpeeddTopology.class
					.getClassLoader().loadClass(dmBoltClassName);
			return clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Creating a decision maker bolt failed", e);
		}

	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Missing config file path");
			System.exit(1);
		}

		String confPath = args[0];

		boolean localMode = true;

		if (args.length == 2 && args[1].equals("remote")) {
			localMode = false;
		}

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(confPath));
			logger.info("Configuration loaded: " + properties.toString());
		} catch (Exception e) {
			System.err.println("Cannot load configuration: " + e.getMessage());
			logger.error("Cannot load configuration", e);
			System.exit(1);
		}

		SpeeddConfig configuration = new SpeeddConfig();
		configuration.zkConnect = (String) properties
				.getProperty("zookeeper.connect");
		configuration.epnPath = (String) properties
				.getProperty("proton.epnPath");
		configuration.inEventScheme = (String) properties
				.getProperty("speedd.inEventScheme");

		configuration.dmClass = (String) properties
				.getProperty("speedd.dmClass");
		
		SpeeddTopology speeddTopology = new SpeeddTopology(configuration);

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

		StormTopology topology = speeddTopology.buildTopology();

		if (localMode) {
			runLocally(conf, topology);
		} else {
			runRemotely(conf, topology);
		}
	}

	private static void runRemotely(Config conf, StormTopology speeddTopology) {
		try {
			logger.info("Running remotely");
			StormSubmitter.submitTopologyWithProgressBar("speedd-runtime",
					conf, speeddTopology);
		} catch (Exception e1) {
			System.err.println("Submit topology faield: " + e1.getMessage());
			logger.error("Submit topology failed", e1);
		}
	}

	private static void runLocally(Config conf, StormTopology speeddTopology) {
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("speedd", conf, speeddTopology);

		try {
			logger.info("Running locally");
			while (true) {
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
		} finally {
			System.out.println("Shutting down the storm cluster");
			cluster.shutdown();
		}
	}
}
