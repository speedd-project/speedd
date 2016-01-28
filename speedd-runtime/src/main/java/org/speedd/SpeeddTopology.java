package org.speedd;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.cep.ProtonOutputConsumerBolt;
import org.speedd.data.Event;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;
import storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import storm.kafka.bolt.selector.DefaultTopicSelector;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.Scheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.base.BaseRichSpout;

import com.ibm.hrl.proton.ProtonTopologyBuilder;
import com.ibm.hrl.proton.metadata.parser.ParsingException;

public class SpeeddTopology {
	private BrokerHosts brokerHosts;

	private SpeeddConfig speeddConfig;

	public static final String CONFIG_KEY_IN_EVENTS_TOPIC = "topic.in.events";
	public static final String CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC = "topic.actions.confirmed";
	public static final String CONFIG_KEY_ADMIN_TOPIC = "topic.admin";
	public static final String CONFIG_KEY_OUT_EVENTS_TOPIC = "topic.out.events";
	public static final String CONFIG_KEY_ACTIONS_TOPIC = "topic.actions";


	public static final String ADMIN_COMMAND_READER = "admin-command-reader";

	public static final String IN_EVENT_READER = "in-event-reader";

	private static final String OUT_EVENT_WRITER = "out-event-writer";

	private static final String CEP_EVENT_CONSUMER = "cep-event-consumer";

	private static final String DECISION_MAKER = "dm";
	
	private static final String ENRICHER = "enricher";

	private static final String DECISION_WRITER = "decision-writer";

	private static final String OPTION_PROPERTY = "D";

	private static final String OPTION_FILE = "f";

	private static final String OPTION_MODE = "m";

	private static final Object OPTION_MODE_REMOTE = "remote";
	
	private static final Object OPTION_MODE_LOCAL = "local";

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
				speeddConfig.inEventScheme, speeddConfig.topicInEvents, IN_EVENT_READER);

		BaseRichSpout adminSpout = createKafkaReaderSpout(
				AdminCommandScheme.class.getName(), speeddConfig.topicAdmin,
				ADMIN_COMMAND_READER);
		
		builder.setSpout(IN_EVENT_READER, trafficReaderSpout);

		KafkaBolt<String, Event> eventWriterBolt = new KafkaBolt<String, Event>().withTopicSelector(
				new DefaultTopicSelector(speeddConfig.topicOutEvents))
				.withTupleToKafkaMapper(
						new FieldNameBasedTupleToKafkaMapper());
		
		BaseRichBolt enricherBolt = createEnrichmentBolt(speeddConfig.enricherClass, speeddConfig.enricherPath);
		// @FIXME distribute output events according to the use-case specific
		// grouping strategy
		builder.setBolt(ENRICHER, enricherBolt)
				.shuffleGrouping(IN_EVENT_READER);

		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();

		try {
			
			protonTopologyBuilder.buildProtonTopology(builder, ENRICHER,
					protonOutputConsumerBolt, CEP_EVENT_CONSUMER,
					speeddConfig.epnPath);
		} catch (ParsingException e) {
			throw new RuntimeException("Building Proton topology failed, reason: ", e);
		}

		builder.setBolt(OUT_EVENT_WRITER, eventWriterBolt).shuffleGrouping(
				CEP_EVENT_CONSUMER);

		builder.setSpout(ADMIN_COMMAND_READER, adminSpout)
				.setMaxTaskParallelism(1);

		IBasicBolt dmBolt = createDecisionMakerBolt(speeddConfig.dmClass);
		// @FIXME distribute output events according to the use-case specific
		// grouping strategy
		builder.setBolt(DECISION_MAKER, dmBolt)
				.shuffleGrouping(CEP_EVENT_CONSUMER)
				.allGrouping(ADMIN_COMMAND_READER);

		builder.setBolt(DECISION_WRITER,
				new KafkaBolt<String, Event>().withTopicSelector(
						new DefaultTopicSelector(speeddConfig.topicActions))
						.withTupleToKafkaMapper(
								new FieldNameBasedTupleToKafkaMapper()))
				.shuffleGrouping(DECISION_MAKER);

		return builder.createTopology();
	}

	public IBasicBolt createDecisionMakerBolt(String dmBoltClassName) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends IBasicBolt> clazz = (Class<IBasicBolt>) SpeeddTopology.class
					.getClassLoader().loadClass(dmBoltClassName);
			return clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Creating a decision maker bolt failed", e);
		}

	}
	
	public BaseRichBolt createEnrichmentBolt(String enrichmentBoltClassName, String enrichmentTablePath) {		
		
		try {
			@SuppressWarnings("unchecked")						
			Class<? extends BaseRichBolt> clazz = (Class<BaseRichBolt>) SpeeddTopology.class
					.getClassLoader().loadClass(enrichmentBoltClassName);			
			
			return clazz.getDeclaredConstructor(String.class).newInstance(enrichmentTablePath);
			
		} catch (Exception e) {
			throw new RuntimeException("Creating an enrichment bolt failed: "+e.getMessage(), e);
		}

	}

	/*
	 * Arguments:
	 * 		-f <config-file> - (optional*) configuration file path (must be available on the supervisor node)
	 * 	    -m remote|local - (optional) mode (remote or local), default=local
	 *      -Dproperty=value - (optional*) configuration properties; override values in the config file (if specified)
	 *      * - either config file or property values must be specified
	 */
	public static void main(String[] args) {
		Properties properties = new Properties();
		boolean localMode = true;
		
		try {
			Options options = new Options();
			
			options.addOption(Option.builder(OPTION_FILE).hasArg().build());
			options.addOption(Option.builder(OPTION_MODE).hasArg().build());
			options.addOption(Option.builder(OPTION_PROPERTY).hasArgs().valueSeparator('=').build());
	
			CommandLineParser clParser = new DefaultParser();
			CommandLine cmd = clParser.parse(options, args);

			if(cmd.hasOption(OPTION_MODE) && cmd.getOptionValue(OPTION_MODE).equals(OPTION_MODE_REMOTE)){
				localMode = false;
			}

			
			if(cmd.hasOption(OPTION_FILE)){
				String confPath = cmd.getOptionValue(OPTION_FILE);
			
				logger.info("Loading configuration from file: " + confPath);
				
				properties.load(new FileInputStream(confPath));
			}
			
			if(cmd.hasOption(OPTION_PROPERTY)){
				String[] propertyArgs = cmd.getOptionValues(OPTION_PROPERTY);
				
				for(int i=0,n=propertyArgs.length-1; i < n;){
					String propertyName = propertyArgs[i++];
					String propertyValue = propertyArgs[i++];
					
					logger.info(String.format("Set %s=%s", propertyName, propertyValue));
					
					properties.put(propertyName, propertyValue);
				}
			}
		
		} catch(ParseException e){
			System.err.println("Invalid arguments: " + e.getMessage());
			logger.error("Invalid arguments: " + e.getMessage());
			System.exit(1);
		} catch (IOException e1){
			System.err.println("Cannot load configuration: " + e1.getMessage());
			logger.error("Cannot load configuration", e1);
			System.exit(1);
		}

		SpeeddConfig configuration = new SpeeddConfig();
		configuration.zkConnect = (String) properties
				.getProperty("zookeeper.connect");
		configuration.epnPath = (String) properties
				.getProperty("proton.epnPath");
		configuration.enricherPath = (String) properties
				.getProperty("speedd.enricherPath");
		configuration.enricherClass = (String) properties
				.getProperty("speedd.enricherClass");
		configuration.inEventScheme = (String) properties
				.getProperty("speedd.inEventScheme");

		configuration.dmClass = (String) properties
				.getProperty("speedd.dmClass");
		
		configuration.topicInEvents = (String) properties.getProperty(CONFIG_KEY_IN_EVENTS_TOPIC);
		configuration.topicOutEvents = (String) properties.getProperty(CONFIG_KEY_OUT_EVENTS_TOPIC);
		configuration.topicActions = (String) properties.getProperty(CONFIG_KEY_ACTIONS_TOPIC);
		configuration.topicAdmin = (String) properties.getProperty(CONFIG_KEY_ADMIN_TOPIC);
		configuration.topicActionsConfirmed = (String) properties.getProperty(CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC);
		
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
			if(properties.containsKey(Config.NIMBUS_HOST)){
				conf.put(Config.NIMBUS_HOST, properties.getProperty(Config.NIMBUS_HOST));
			}
			
			if(properties.containsKey(Config.NIMBUS_THRIFT_PORT)){
				conf.put(Config.NIMBUS_THRIFT_PORT, Integer.parseInt(properties.getProperty(Config.NIMBUS_THRIFT_PORT)));
			}
			
			runRemotely(conf, topology);
		}
	}

	private static void runRemotely(Config conf, StormTopology speeddTopology) {
		try {
			logger.info("Running remotely");
			StormSubmitter.submitTopology("speedd-runtime",
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
