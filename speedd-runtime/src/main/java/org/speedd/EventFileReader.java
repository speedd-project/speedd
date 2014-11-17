package org.speedd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventFileReader {
	
	static class EventMessageRecord {
		public final String messageText;
		public final long sendDelayMillis;

		public EventMessageRecord(String msg, long delay) {
			messageText = msg;
			sendDelayMillis = delay;
		}
	}

	public static interface EventListener {
		void onEvent(EventMessageRecord eventMessageRecord);
	}
	
	private String path;
	protected BufferedReader reader;
	private ProducerConfig producerConfig;
	protected Producer<String, String> producer;
	protected String topic;
	protected Logger logger;

	private long delayMillis;
	
	protected List<EventListener> listeners;

	protected static final long DEFAULT_SEND_DELAY = 10;

	protected static final int FAILURES_TO_GIVEUP = 3;

	public EventFileReader(String filePath, String topic,
			ProducerConfig kafkaProducerConfig) {
		this(filePath, topic, kafkaProducerConfig, DEFAULT_SEND_DELAY);
	}

	public EventFileReader(String filePath, String topic,
			ProducerConfig kafkaProducerConfig, long sendDelayMillis) {
		
		logger = LoggerFactory.getLogger(this.getClass());
		
		path = filePath;
		
		producerConfig = kafkaProducerConfig;
		
		this.topic = topic;
		
		delayMillis = sendDelayMillis;
		
		listeners = new ArrayList<EventListener>();
	}

	public void addListener(EventListener eventListener){
		listeners.add(eventListener);
	}
	
	public void removeListener(EventListener eventListener){
		for (EventListener listener : listeners) {
			if(listener.equals(eventListener)){
				listeners.remove(listener);
			}
		}
	}
	
	public void removeAllListeners(){
		listeners.clear();
	}
	
	public void open() throws EventReaderException {
		try {
			reader = new BufferedReader(new FileReader(path));

			logger.info(String.format("Open file %s for read.", path));

			producer = new Producer<String, String>(producerConfig);

			logger.info(String.format("Open producer to send to kafka: %s",
					producerConfig.toString()));
		} catch (FileNotFoundException e) {
			throw new EventReaderException(e);
		}
	}

	protected EventMessageRecord nextEventMessageRecord() throws IOException {
		String line = reader.readLine();
		return line != null ? new EventMessageRecord(line, delayMillis) : null;
	}

	public void streamEvents() {
		try {
			open();
		} catch (EventReaderException e) {
			logger.error(e.getMessage(), e);
		}

		if (reader == null) {
			logger.warn("Reader is null, probably not initialized, - no data will be streamed.");
			return;
		}

		int failures = 0;

		boolean done = false;

		while (!done) {
			try {
				EventMessageRecord eventMessageRecord = nextEventMessageRecord();
				if (eventMessageRecord != null) {
					Thread.sleep(eventMessageRecord.sendDelayMillis);

					logger.debug(String.format("Line to send to topic %s: %s",
							topic, eventMessageRecord.messageText));
					KeyedMessage<String, String> message = new KeyedMessage<String, String>(
							topic, eventMessageRecord.messageText);

					producer.send(message);
					notifyListeners(eventMessageRecord);
				} else {
					done = true;
					logger.info("End of file reached.");
				}

			} catch (IOException e) {
				logger.warn("Read from file failed", e);
				failures++;
				if (failures >= FAILURES_TO_GIVEUP) {
					done = true;
					logger.error(String.format(
							"%d attempts to read from file failed. Giving up.",
							failures));
				}
			} catch (InterruptedException e) {
			}
		}

		close();
	}

	public void close() {
		try {

			if (reader != null) {
				reader.close();
				reader = null;
			}

			if (producer != null) {
				producer.close();
				producer = null;
			}

		} catch (IOException e) {
			logger.warn("Closing the reader failed", e);
		}
	}

	protected void notifyListeners(EventMessageRecord eventMessageRecord){
		for (EventListener listener : listeners) {
			listener.onEvent(eventMessageRecord);
		}
	}
}
