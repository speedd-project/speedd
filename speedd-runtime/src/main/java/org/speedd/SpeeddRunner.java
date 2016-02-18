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

import scala.xml.persistent.SetStorage;
import storm.kafka.bolt.KafkaBolt;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;

public class SpeeddRunner {
	public static final String CONFIG_KEY_IN_EVENTS_TOPIC = "topic.in.events";
	public static final String CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC = "topic.actions.confirmed";
	public static final String CONFIG_KEY_ADMIN_TOPIC = "topic.admin";
	public static final String CONFIG_KEY_OUT_EVENTS_TOPIC = "topic.out.events";
	public static final String CONFIG_KEY_ACTIONS_TOPIC = "topic.actions";

	private static final String OPTION_PROPERTY = "D";

	private static final String OPTION_FILE = "f";

	private static final String OPTION_MODE = "m";

	private static final Object OPTION_MODE_REMOTE = "remote";
	
	private static final String CONFIG_KEY_TOPOLOGY_CLASS = "speedd.topology.class";
	
	private static final String SPEEDD_RUNTIME_TOPOLOGY_NAME = "speedd-runtime";
	
	private static Logger logger = LoggerFactory.getLogger(SpeeddRunner.class);

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
		
		if(properties.containsKey("speedd.cepParallelismHint")){
			configuration.cepParallelismHint = Integer.parseInt(properties.getProperty("speedd.cepParallelismHint"));	
		}
		
		if(properties.containsKey("speedd.inEventReaderParallelismHint")){
			configuration.inEventReaderParallelismHint = Integer.parseInt(properties.getProperty("speedd.inEventReaderParallelismHint"));
		}
		
		if(properties.containsKey("speedd.inEventReaderTaskNum")){
			configuration.inEventReaderTaskNum = Integer.parseInt(properties.getProperty("speedd.inEventReaderTaskNum"));
		}

		if(properties.containsKey("speedd.outEventWriterParallelismHint")){
			configuration.outEventWriterParallelismHint = Integer.parseInt(properties.getProperty("speedd.outEventWriterParallelismHint"));
		}

		if(properties.containsKey("speedd.outEventWriterTaskNum")){
			configuration.outEventWriterTaskNum = Integer.parseInt(properties.getProperty("speedd.outEventWriterTaskNum"));
		}
		
		configuration.topicInEvents = (String) properties.getProperty(CONFIG_KEY_IN_EVENTS_TOPIC);
		configuration.topicOutEvents = (String) properties.getProperty(CONFIG_KEY_OUT_EVENTS_TOPIC);
		configuration.topicActions = (String) properties.getProperty(CONFIG_KEY_ACTIONS_TOPIC);
		configuration.topicAdmin = (String) properties.getProperty(CONFIG_KEY_ADMIN_TOPIC);
		configuration.topicActionsConfirmed = (String) properties.getProperty(CONFIG_KEY_ACTIONS_CONFIRMED_TOPIC);
		
		ISpeeddTopology speeddTopology = null;
		
		try {
			speeddTopology = createTopology((String) properties.getProperty(CONFIG_KEY_TOPOLOGY_CLASS));
			speeddTopology.configure(configuration);
		} catch (SpeeddRunnerException e){
			logger.error("Cannot create a topology. Aborting.", e);
			System.exit(1);
		}

		Config conf = new Config();
		
		conf.setDebug(Boolean.parseBoolean(properties.getProperty(Config.TOPOLOGY_DEBUG)));

		conf.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, properties);

		for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
			Entry<String, String> entry = (Entry<String, String>) iter.next();
			if (entry.getKey().startsWith("topic.")) {
				conf.put(entry.getKey(), entry.getValue());
			}
		}

		
		setStormConfigPropertyInteger(conf, properties, Config.TOPOLOGY_MAX_TASK_PARALLELISM, null);
		setStormConfigPropertyInteger(conf, properties, Config.TOPOLOGY_ACKER_EXECUTORS, null);
		setStormConfigPropertyInteger(conf, properties, Config.TOPOLOGY_WORKERS, 1);
		
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

	private static void setStormConfigPropertyInteger(Config config, Properties properties, String name, Object defaultValue){
		if(properties.containsKey(name)){
			config.put(name, Integer.parseInt(properties.getProperty(name)));
		} else {
			config.put(name, defaultValue);
		}
	}
	
	private static ISpeeddTopology createTopology(String topologyClassName) throws SpeeddRunnerException {
		try {
			return (ISpeeddTopology)Class.forName(topologyClassName).newInstance();
		} catch (Exception e){
			String message = "Cannot instantiate the topology object of class " + topologyClassName;
			logger.error(message, e);
			throw new SpeeddRunnerException(message, e);
		}
	}

	private static void runRemotely(Config conf, StormTopology speeddTopology) {
		try {
			logger.info("Running remotely");
			StormSubmitter.submitTopology(SPEEDD_RUNTIME_TOPOLOGY_NAME,
					conf, speeddTopology);
		} catch (Exception e) {
			String message = "Submit topology failed: " + e.getMessage();
			System.err.println(message);
			logger.error(message, e);
		}
	}

	private static void runLocally(Config conf, StormTopology speeddTopology) {
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology(SPEEDD_RUNTIME_TOPOLOGY_NAME, conf, speeddTopology);

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
