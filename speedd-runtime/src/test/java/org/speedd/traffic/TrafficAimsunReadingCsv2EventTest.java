package org.speedd.traffic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

public class TrafficAimsunReadingCsv2EventTest {
	private static final TrafficAimsunReadingCsv2Event parser = new TrafficAimsunReadingCsv2Event(SpeeddEventFactory.getInstance());

	@Test
	public void testCsv2Event() throws Exception {
		String eventCsv = "2015-12-16 06:00:15,1375,60.607787719,2,1,11.8837352842,7.82520854977,4.05852673446,7.328";
		
		Event event = parser.fromBytes(eventCsv.getBytes(Charset.forName("UTF-8")));
		
		assertNotNull(event);
	}
	
	@Test
	public void testEmptyLine() throws Exception {
		String empty = "";
		
		Event event = parser.fromBytes(empty.getBytes(Charset.forName("UTF-8")));
		
		assertNull(event);
	}
	
	
	@Test
	public void testMany() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("c:\\temp\\inevents2.txt"));
		
		boolean done = false;
		
		try {
		while(!done){
			String csv = reader.readLine();
			
			if(csv != null) {
				Event event = parser.fromBytes(csv.getBytes(Charset.forName("UTF-8")));
				if(csv.trim().isEmpty()){
					assertNull(event);
				} else {
					assertNotNull(event);
				}
			} else {
				done = true;
			}
		}
		} finally {
			if(reader != null){
				reader.close();
			}
		}
	}

}
