package org.speedd;

import java.io.IOException;
import java.nio.charset.Charset;

import kafka.producer.ProducerConfig;

import org.speedd.data.Event;

public class TimedEventFileReader extends EventFileReader {
	private EventParser eventParser;

	private long prevTimestamp;

	public TimedEventFileReader(String filePath, String topic,
			ProducerConfig kafkaProducerConfig, EventParser eventParser) {
		super(filePath, topic, kafkaProducerConfig);
		this.eventParser = eventParser;
	}

	@Override
	public void open() throws EventReaderException {
		super.open();
		prevTimestamp = 0;
	}

	@Override
	protected EventMessageRecord nextEventMessageRecord() throws IOException {
		String line = reader.readLine();

		if (line != null) {
			Event event = eventParser.fromBytes(line.getBytes(Charset
					.forName("UTF-8")));
			long timestamp = event.getTimestamp();

			long delayMillis = prevTimestamp > 0 ? timestamp - prevTimestamp : 0;

			if (delayMillis >= 0) {
				prevTimestamp = timestamp;
			} else {
				delayMillis = 0;
				// leave prevTimestamp as its last value - either this
				// is the 1st event, or the current event has earlier
				// timestamp than the previous one
			}

			return new EventMessageRecord(line, delayMillis);
		} else {
			return null;
		}
	}

}
