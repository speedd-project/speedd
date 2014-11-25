package org.speedd.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.log4j.Logger;
import org.speedd.data.Event;

import backtype.storm.spout.Scheme;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Values;

public class FileReaderSpout extends BaseRichSpout {
	private File inEventsFile;

	private BufferedReader eventReader;

	private SpoutOutputCollector collector;

	private Scheme scheme;

	private boolean done;

	private static final Logger logger = Logger.getLogger(FileReaderSpout.class);

	public FileReaderSpout(File inEventsFile, Scheme eventScheme) {
		this.inEventsFile = inEventsFile;
		this.scheme = eventScheme;
	}

	@Override
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		
		this.collector = collector;
		
		done = false;

		try {
			eventReader = new BufferedReader(new FileReader(inEventsFile));
		} catch (FileNotFoundException e) {
			logger.error("Cannot create a reader for event file", e);
		}
	}

	@Override
	public void nextTuple() {
		try {
			if (!done) {
				Thread.sleep(100);

				Values tuple = nextEvent();

				if (tuple == null) {
					done = true;
					eventReader.close();
					logger.info("All events have been read - done.");
					return;
				}

				logger.info("Next event: " + tuple);

				collector.emit(tuple);
			}
		} catch (Exception e) {
			logger.error("Error reading next event: ", e);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(scheme.getOutputFields());
	}

	private Values nextEvent() throws Exception {
		String nextLine = eventReader.readLine();
		if(nextLine != null){
			return (Values)scheme.deserialize(nextLine.getBytes(
				Charset.forName("UTF-8")));
		} else {
			return null;
		}
	}

}