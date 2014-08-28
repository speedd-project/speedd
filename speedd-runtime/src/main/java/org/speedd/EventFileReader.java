package org.speedd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventFileReader {
	private String path;
	private BufferedReader reader;
	private ProducerConfig producerConfig;
	private Producer<String, String> producer;
	private String topic;
	private int FAILURES_TO_GIVEUP = 3;
	private Logger logger = LoggerFactory.getLogger(EventFileReader.class);

	public EventFileReader(String filePath, String topic, ProducerConfig kafkaProducerConfig) {
		path = filePath;
		producerConfig = kafkaProducerConfig;
		this.topic = topic;
	}

	public void open() throws EventReaderException {
		try {
		reader = new BufferedReader(new FileReader(path));

		logger.info(String.format("Open file %s for read.", path));
		
		producer = new Producer<String, String>(producerConfig);
		
		logger.info(String.format("Open producer to send to kafka: %s", producerConfig.toString()));
		} catch (FileNotFoundException e){
			throw new EventReaderException(e);
		}
	}

	public void streamEvents(int delayMillis) {
		try {
			open();
		} catch (EventReaderException e){
			logger.error(e.getMessage(),e);
		}
		
		if (reader == null) {
			logger.warn("Reader is null, probably not initialized, - no data will be streamed.");
			return;
		}

		int failures = 0;

		boolean done = false;

		while (!done) {
			try {
				String line = reader.readLine();
				if(line != null){
					logger.debug(String.format("Line to send to topic %s: %s", topic, line));
					KeyedMessage<String, String> message = new KeyedMessage<String, String>(topic, line);
					producer.send(message);
				}
				else {
					done = true;
					logger.info("End of file reached.");
				}

				Thread.sleep(delayMillis);
			} catch (IOException e) {
				logger.warn("Read from file failed", e);
				failures++;
				if(failures >= FAILURES_TO_GIVEUP){
					done = true;
					logger.error(String.format("%d attempts to read from file failed. Giving up.", failures));
				}
			} catch (InterruptedException e) {}
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

}
