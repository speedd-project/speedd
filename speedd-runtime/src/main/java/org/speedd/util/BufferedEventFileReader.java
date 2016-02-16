package org.speedd.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.speedd.EventFileReader;
import org.speedd.EventParser;
import org.speedd.EventReaderException;
import org.speedd.data.Event;

public class BufferedEventFileReader extends EventFileReader {
	ArrayList<EventMessageRecord> buffer;
	
	protected static final int INITIAL_BUFFER_CAPACITY = 10000;
	
	private int cursor;
	
	private int numEvents;
	
	private boolean isOpen;
	
	private int reps;
	
	private int repCount;
	
	protected static final int DEFAULT_REPS = 1;
	
	private EventParser eventParser;
	
	private String keyAttr;
	
	public BufferedEventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties, long sendDelayMillis, int reps, EventParser eventParser, String keyAttr){
		super(filePath, topic, kafkaProducerProperties, sendDelayMillis);
		buffer = new ArrayList<EventMessageRecord>(INITIAL_BUFFER_CAPACITY);
		this.reps = reps;
		this.eventParser = eventParser;
	}
	
	public BufferedEventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties, long sendDelayMillis, int reps){
		this(filePath, topic, kafkaProducerProperties, sendDelayMillis, reps, null, null);
	}
	
	public BufferedEventFileReader(String filePath, String topic,
			Properties kafkaProducerProperties, long sendDelayMillis) {
		this(filePath, topic, kafkaProducerProperties, sendDelayMillis, DEFAULT_REPS, null, null);
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
				buffer.add(createEventMessageRecord(line));
			} else {
				done = true;
			}
		}
		
		cursor = -1;
		
		numEvents = buffer.size();
		
		repCount = -1;
		
		isOpen = true;
	}
	
	private EventMessageRecord createEventMessageRecord(String line){
		if(eventParser != null && keyAttr != null){
			Event event = eventParser.fromBytes(line.getBytes());
			Object key = event.getAttributes().get(keyAttr);
			return new EventMessageRecord(line, delayMicroseconds, key.toString());
			
		} else {
			return new EventMessageRecord(line, delayMicroseconds);
		}
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
		
		return repCount < reps? buffer.get(cursor) : null;
	}

}
