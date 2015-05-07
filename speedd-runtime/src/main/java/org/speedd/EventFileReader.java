package org.speedd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
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
	
	private static class ProducerSendCallback implements Callback {
		private final Logger log;
		
		public ProducerSendCallback(Logger log) {
			this.log = log;
		}

		@Override
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			if(exception != null){
				log.warn("Send failed", exception);
			}
		}
		
	}
	
	private String path;
	protected BufferedReader reader;
	private Properties producerProps;
	protected KafkaProducer<String, String> producer;
	protected String topic;
	protected Logger logger;

	private long delayMillis;
	
	protected List<EventListener> listeners;
	
	protected ProducerSendCallback sendCallback;

	protected static final long DEFAULT_SEND_DELAY = 10;

	protected static final int FAILURES_TO_GIVEUP = 3;

	public EventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties) {
		this(filePath, topic, kafkaProducerProperties, DEFAULT_SEND_DELAY);
	}

	public EventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties, long sendDelayMillis) {
		
		logger = LoggerFactory.getLogger(this.getClass());
		
		path = filePath;
		
		producerProps = kafkaProducerProperties;
		
		this.topic = topic;
		
		delayMillis = sendDelayMillis;
		
		listeners = new ArrayList<EventListener>();
		
		sendCallback = new ProducerSendCallback(logger);
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

			producer = new KafkaProducer<String, String>(producerProps);

			logger.info(String.format("Open producer to send to kafka: %s",
					producerProps.toString()));
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
					ProducerRecord<String, String> message = new ProducerRecord<String, String>(
							topic, eventMessageRecord.messageText);

					producer.send(message, sendCallback);
					
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
