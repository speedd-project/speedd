package org.speedd.util;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Properties;

import kafka.producer.ProducerConfig;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.speedd.EventParser;
import org.speedd.TimedEventFileReader;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

public class EventPlayer {
	public static final String EVENT_FILE = "test-events.csv";
	public static final String DEFAULT_TOPIC = "speedd-in-events";

	public static final String DEFAULT_CONFIG_PATH = "producer.properties";

	private ProducerConfig kafkaProducerConfig;

	private String topic;

	private EventParser eventParser;

	public void playEventsFromFile(String path) throws Exception {
		TimedEventFileReader eventFileReader = new TimedEventFileReader(path,
				topic, kafkaProducerConfig, eventParser);

		eventFileReader.streamEvents();

		System.out.println("Event playback complete.");
	}

	public EventPlayer(String configPath, String topic, EventParser eventParser)
			throws IOException {
		Properties properties = new Properties();

		properties.load(new FileReader(configPath));

		System.out.println("Properties loaded:" + properties.toString());

		kafkaProducerConfig = new ProducerConfig(properties);

		this.topic = topic;

		this.eventParser = eventParser;
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("c", "configuration", true, "configuration file");
		options.addOption("t", "topic", true, "topic");
		options.addOption("p", "parser", true, "event parser class name (FQN)");

		CommandLineParser clParser = new BasicParser();

		String eventFile = null;

		String configPath = null;

		String topic = null;

		String eventParserClassName = null;

		try {
			CommandLine cmd = clParser.parse(options, args);

			configPath = cmd.hasOption('c') ? cmd.getOptionValue('c')
					: DEFAULT_CONFIG_PATH;

			topic = cmd.hasOption('t') ? cmd.getOptionValue('t')
					: DEFAULT_TOPIC;

			if (cmd.hasOption('p')) {
				eventParserClassName = cmd.getOptionValue('p');
			} else {
				throw new ParseException("Event parser class name missing");
			}

			@SuppressWarnings("unchecked")
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

		Constructor constructor = Class.forName(eventParserClassName).getDeclaredConstructor(EventFactory.class);
		EventPlayer player = new EventPlayer(configPath, topic,
				(EventParser) constructor.newInstance(SpeeddEventFactory.getInstance()));

		player.playEventsFromFile(eventFile);

		return;
	}

}
