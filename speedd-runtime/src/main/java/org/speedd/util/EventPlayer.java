package org.speedd.util;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.EventFileReader;
import org.speedd.EventFileReader.Statistics;
import org.speedd.EventParser;
import org.speedd.TimedEventFileReader;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

public class EventPlayer {
	public static final String EVENT_FILE = "test-events.csv";
	public static final String DEFAULT_TOPIC = "speedd-in-events";

	public static final String DEFAULT_CONFIG_PATH = "producer.properties";

	private Properties kafkaProducerProperties;

	private String topic;

	private EventParser eventParser;
	
	private boolean stressModeOn;
	
	private boolean repModeOn;
	
	private boolean endless;
	
	private int reps;
	
	private static final Logger log = LoggerFactory.getLogger(EventPlayer.class);

	public void playEventsFromFile(String path) throws Exception {
		EventFileReader eventFileReader = stressModeOn? 
				new BufferedEventFileReader(path, topic, kafkaProducerProperties, 0, reps) : 
				new TimedEventFileReader(path, topic, kafkaProducerProperties, eventParser);

		eventFileReader.streamEvents();

		Statistics stats = eventFileReader.getStatistics();
		
		log.info(String.format("Event playback complete: total events = %d, sent = %d, failed = %d", stats.getNumOfAttempts(), stats.getNumOfSent(), stats.getNumOfFailed()));
		log.info(String.format("Elapsed time: %d ms", stats.getElapsedTimeMilliseconds()));

//		boolean done = false;
//		int repsToGo = repModeOn? reps : 0;
//		
//		while(!done){
//			eventFileReader.streamEvents();
//
//			Statistics stats = eventFileReader.getStatistics();
//			
//			log.info(String.format("Event playback complete: total events = %d, sent = %d, failed = %d", stats.getNumOfAttempts(), stats.getNumOfSent(), stats.getNumOfFailed()));
//			log.info(String.format("Elapsed time: %d ms", stats.getElapsedTimeMilliseconds()));
//
//			if(!repModeOn){
//				done = true;
//			} else if (!endless){
//				repsToGo--;
//				done = repsToGo == 0;
//			}
//		}
		
	}

	
	public EventPlayer(String configPath, String topic, EventParser eventParser, boolean isStressMode, boolean isRepMode, int reps)
			throws IOException {
		kafkaProducerProperties = new Properties();
		kafkaProducerProperties.load(new FileReader(configPath));

		System.out.println("Properties loaded:" + kafkaProducerProperties.toString());

		this.topic = topic;

		this.eventParser = eventParser;
		
		this.stressModeOn = isStressMode;
		
		this.repModeOn = isRepMode;
		
		if(this.repModeOn){
			this.reps = reps;
			this.endless = reps == 0;
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("c", "configuration", true, "configuration file");
		options.addOption("t", "topic", true, "topic");
		options.addOption("p", "parser", true, "event parser class name (FQN)");
		options.addOption("s", "stress", false, "run in stress test mode - no pauses between events");
		options.addOption("r", "repeat", true, "repeat (n times), 0 = endless loop");

		CommandLineParser clParser = new DefaultParser();

		String eventFile = null;

		String configPath = null;

		String topic = null;

		String eventParserClassName = null;
		
		boolean stressModeOn = false;
		
		int reps = 0;
		
		boolean repModeOn = false;

		try {
			CommandLine cmd = clParser.parse(options, args);

			configPath = cmd.hasOption('c') ? cmd.getOptionValue('c')
					: DEFAULT_CONFIG_PATH;

			topic = cmd.hasOption('t') ? cmd.getOptionValue('t')
					: DEFAULT_TOPIC;

			if (cmd.hasOption('s')){
				stressModeOn = true;
			}
			
			if (cmd.hasOption('r')){
				reps = Integer.parseInt(cmd.getOptionValue('r'));
				repModeOn = true;
			}

			if(!stressModeOn){
				if (cmd.hasOption('p')) {
					eventParserClassName = cmd.getOptionValue('p');
				} else {
					throw new ParseException("Event parser class name missing");
				}
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

		EventPlayer player;
		
		if(eventParserClassName != null){
			Constructor<? extends EventParser> constructor = (Constructor<? extends EventParser>) Class.forName(eventParserClassName).getDeclaredConstructor(EventFactory.class);
			player = new EventPlayer(configPath, topic,
				(EventParser) constructor.newInstance(SpeeddEventFactory.getInstance()), stressModeOn, repModeOn, reps);
		} else {
			player = new EventPlayer(configPath, topic,
					null, stressModeOn, repModeOn, reps);
		}
		player.playEventsFromFile(eventFile);
		
		return;
	}

}
