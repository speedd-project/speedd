package org.speedd.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.speedd.Fields;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.kafka.JsonEventEncoder;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

@SuppressWarnings("serial")
public class FileWriterBolt extends BaseRichBolt {
	EventFactory eventFactory;
	File outFile;
	FileWriter outFileWriter;
	JsonEventEncoder jsonEventEncoder;

	public FileWriterBolt(File outFile) {
		this.outFile = outFile;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		try {
			eventFactory = SpeeddEventFactory.getInstance();

			outFileWriter = new FileWriter(outFile);

			jsonEventEncoder = new JsonEventEncoder(null);

		} catch (IOException e) {
			System.err.println("Error preparing event consumer: "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(Tuple input) {
		String eventName = (String) input
				.getValueByField(Fields.FIELD_NAME);

		Map<String, Object> inAttrs = (Map<String, Object>) input
				.getValueByField(Fields.FIELD_ATTRIBUTES);

		long timestamp = 0;

		if (inAttrs.containsKey(Fields.FIELD_DETECTION_TIME)) {
			timestamp = (Long) inAttrs.get(Fields.FIELD_DETECTION_TIME);
		}

		Event outEvent = eventFactory.createEvent(eventName, timestamp,
				inAttrs);

		try {
			outFileWriter.write(new String(jsonEventEncoder
					.eventToBytes(outEvent), "UTF-8"));
			outFileWriter.write('\n');
			outFileWriter.flush();
		} catch (IOException e) {
			System.err.println("Error writing event as JSON to file: "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	}

	@Override
	public void cleanup() {
		try {
			outFileWriter.close();
		} catch (IOException e) {
			System.err.println("Error closing the out file");
		}
		super.cleanup();
	}
}