package org.speedd.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.speedd.EventFileReader;
import org.speedd.EventReaderException;

public class BufferedEventFileReader extends EventFileReader {
	ArrayList<String> buffer;
	
	protected static final int INITIAL_BUFFER_CAPACITY = 10000;
	
	private int cursor;
	
	private int numEvents;
	
	private boolean isOpen;
	
	private int reps;
	
	private int repCount;
	
	protected static final int DEFAULT_REPS = 1;
	
	public BufferedEventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties, long sendDelayMillis, int reps){
		super(filePath, topic, kafkaProducerProperties, sendDelayMillis);
		buffer = new ArrayList<String>(INITIAL_BUFFER_CAPACITY);
		this.reps = reps;
	}
	public BufferedEventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties, long sendDelayMillis) {
		this(filePath, topic, kafkaProducerProperties, sendDelayMillis, DEFAULT_REPS);
	}
	
	@Override
	protected void open() throws EventReaderException {
		if(isOpen){
			return;
		}
		
		super.open();
		
		boolean done = false;
		
		while(!done) {
			String line;
			
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new EventReaderException(e);
			}
			
			if (line != null){
				buffer.add(line);
			} else {
				done = true;
			}
		}
		
		cursor = -1;
		
		numEvents = buffer.size();
		
		repCount = -1;
		
		isOpen = true;
	}
	
	@Override
	protected void close() {
		if(!isOpen){
			return;
		}
		
		super.close();
		isOpen = false;
	}
	
	@Override
	protected EventMessageRecord nextEventMessageRecord() throws IOException {
		if(buffer.isEmpty() || repCount == reps){
			return null;
		}
		
		cursor = (cursor + 1) % numEvents;
		
		if(cursor == 0) {
			repCount++;
		}
		
		return repCount < reps? new EventMessageRecord(buffer.get(cursor), delayMicroseconds) : null;
	}

}
