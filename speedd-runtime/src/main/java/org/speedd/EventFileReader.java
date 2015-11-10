package org.speedd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

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
		private Statistics stats;
		
		public ProducerSendCallback(Logger log, Statistics stats) {
			this.log = log;
			this.stats = stats;
		}

		@Override
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			if(exception == null){
				this.stats.nSent.incrementAndGet();
			}
			else {
				log.warn("Send failed", exception);
				this.stats.nFailed.incrementAndGet();
			}
		}
		
	}
	
	public static class Statistics {
		private AtomicLong nFailed;
		private AtomicLong nSent;
		private AtomicLong nAttempted;
		private long startTime;
		private long endTime;
		
		protected void reset(){
			nFailed = new AtomicLong(0);
			nSent  = new AtomicLong(0);
			nAttempted  = new AtomicLong(0);
			startTime = System.currentTimeMillis();
			endTime = 0;
		}
		
		public Statistics(){
			reset();
		}
		
		public long getNumOfAttempts(){
			return nAttempted.get();
		}
		
		public long getNumOfSent(){
			return nSent.get();
		}
		
		public long getNumOfFailed(){
			return nFailed.get();
		}
		
		public long getStartTimestamp(){
			return startTime;
		}
		
		public long getEndTimestamp(){
			return endTime;
		}
		
		public long getElapsedTimeMilliseconds(){
			return isFinished()? endTime - startTime : 0;
		}
		
		public boolean isFinished(){
			return endTime > 0;
		}

		public void setFinished() {
			endTime = System.currentTimeMillis();
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
	
	private Statistics stats;

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
		
		stats = new Statistics();
		
		sendCallback = new ProducerSendCallback(logger, stats);
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
		
		stats.reset();

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
					
					stats.nAttempted.incrementAndGet();
					
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

		stats.setFinished();
		
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
	
	public Statistics getStatistics(){
		return stats;
	}
}
