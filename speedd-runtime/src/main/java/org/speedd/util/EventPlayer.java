package org.speedd.util;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import kafka.producer.ProducerConfig;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.speedd.EventFileReader;

public class EventPlayer {
	public static final String EVENT_FILE = "test-events.csv";
	public static final String DEFAULT_TOPIC = "speedd-in-events";

	public static final String DEFAULT_CONFIG_PATH = "producer.properties";

	private String configPath;
	
	private ProducerConfig kafkaProducerConfig;
	
	private String topic;

	public void playEventsFromFile(String path) throws Exception {
		EventFileReader eventFileReader = new EventFileReader(path, DEFAULT_TOPIC,
				kafkaProducerConfig);

		eventFileReader.streamEvents(1000);

		System.out.println("Event playback complete.");
	}

	public EventPlayer(String configPath, String topic) throws IOException {
		Properties properties = new Properties();
		
		properties.load(new FileReader(configPath));

		System.out.println("Properties loaded:" + properties.toString());

		kafkaProducerConfig = new ProducerConfig(properties);
		
		this.topic = topic;
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("c", "configuration", true, "configuration file");
		options.addOption("t", "topic", true, "topic");

		CommandLineParser clParser = new BasicParser();

		String eventFile = null;
		
		String configPath = null;
		
		String topic = null;

		try {
			CommandLine cmd = clParser.parse(options, args);

			configPath = cmd.hasOption('c') ? cmd.getOptionValue('c')
					: DEFAULT_CONFIG_PATH;

			topic = cmd.hasOption('t')? cmd.getOptionValue('t') : DEFAULT_TOPIC;
			
			List<String> argList = cmd.getArgList();

			if (argList.isEmpty()) {
				throw new ParseException("Event file missing");
			}

			eventFile = argList.get(0);

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("playevents [options] <event file>",
					options);
			System.exit(1);
		}

		System.out.println("Loading producer configuration from " + configPath);
		System.out.println("Event file: " + eventFile);
		System.out.println("Sending events to topic: " + topic);
		
		EventPlayer player = new EventPlayer(configPath, topic);
		
		player.playEventsFromFile(eventFile);

		return;

		/*
		 * File eventJsonFile = new File(eventFile);
		 * 
		 * // if(args.length > 0){ // eventJsonFile = new File(args[0]); // } //
		 * else { // eventJsonFile = new
		 * File(TestSpeedTopology.class.getClassLoader
		 * ().getResource("simple-event.json").toURI()); // } // // char[] buf =
		 * new char[1000];
		 * 
		 * String inEventStr = Files.contentOf(eventJsonFile, "UTF-8");
		 * 
		 * // setup producer Properties producerProperties =
		 * TestUtils.getProducerConfig( "localhost:" + 9092,
		 * "kafka.producer.DefaultPartitioner");
		 * 
		 * ProducerConfig pConfig = new ProducerConfig(producerProperties);
		 * 
		 * Producer<String, String> producer = new Producer<String, String>(
		 * pConfig);
		 * 
		 * KeyedMessage<String, String> message = new KeyedMessage<String,
		 * String>( "speedd-in-events", inEventStr);
		 * 
		 * producer.send(message);
		 */
	}

}
