package org.speedd.traffic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.speedd.Fields;
import org.speedd.data.Event;

import backtype.storm.task.OutputCollector;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TrafficEnrichmentTestBolt {
	protected static final String ATTR_LOCATION = "location";
	protected static final String ATTR_DETECTOR_ID = "sensorId";
	protected static final String ATTR_LANE = "lane";
	protected static final String ATTR_VEHICLES = "average_flow";	
	protected static final String ATTR_TIMESTAMP = "timestamp";
	protected static final String ATTR_AVG_SPEED = "average_speed";	
	protected static final String ATTR_NORMALIZED_DENSITY = "average_density";
	protected static final String ATTR_DENSITY = "density";	
	protected static final String ATTR_DM_PARTITION = "dm_partition";		
	

	
	
	public class TestCollector extends BasicOutputCollector {

		public List<Object> tuple;
		
		public TestCollector(OutputCollector collector) {
			super(collector);
		}
		
		@Override
		public List<Integer> emit(List<Object> tuple) {
			this.tuple = tuple;
			
			return null;
		}
		
	}
	
	private Tuple mockTuple(List<Object> values){
		Tuple tuple = mock(Tuple.class);
		when (tuple.getValueByField(Fields.FIELD_ATTRIBUTES)).thenReturn(values.get(2));
		when (tuple.getValueByField(Fields.FIELD_PROTON_EVENT_NAME)).thenReturn(values.get(0));
		when (tuple.getValueByField(Fields.FIELD_TIMESTAMP)).thenReturn(values.get(1));
		
		return tuple;
	}
	

	@Test
	public void test() throws IOException {
		

		TrafficEnricherBolt enricherBolt = new TrafficEnricherBolt("./src/test/resources/rochade_sensor_and_aimsun_labels_partitions.csv");
		TestCollector collector = new TestCollector(null);
		long currentTimestamp = Calendar.getInstance().getTimeInMillis();
		Map<String,Object> eventAttributes = new HashMap<String,Object>();
		eventAttributes.put(ATTR_TIMESTAMP, currentTimestamp);
		eventAttributes.put(ATTR_DETECTOR_ID,"1675");						
		eventAttributes.put(ATTR_VEHICLES, 10);			
		eventAttributes.put(ATTR_AVG_SPEED, 86.5);
		eventAttributes.put(ATTR_NORMALIZED_DENSITY, 0.98);
		eventAttributes.put(ATTR_DENSITY,123);
		String eventName = "AverageDensityAndSpeedPerLocation";
		
		Tuple input = mockTuple(new Values(eventName,currentTimestamp ,eventAttributes));
		enricherBolt.execute(input,collector);
		
		List<Object> outTuple = collector.tuple;
		assertNotNull(outTuple);
		assertEquals(outTuple.get(0),eventName);
		assertEquals(outTuple.get(1),currentTimestamp);
		
		Map<String,Object> eventUpdatedAttributes = (Map<String, Object>) outTuple.get(2);		
		assertEquals(eventUpdatedAttributes.get(ATTR_DM_PARTITION),"section2");
		assertEquals(eventUpdatedAttributes.get(ATTR_LANE),"onramp");		
		assertEquals(eventUpdatedAttributes.get(ATTR_LOCATION),"0024a4dc00001b67");
		
	}

}
