package org.speedd.dm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

import backtype.storm.task.IOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TrafficDMBoltTest {
	
	public class TestCollector extends OutputCollector {

		public List<Object> tuple;
		
		public TestCollector(IOutputCollector delegate) {
			super(delegate);
		}
		
		@Override
		public List<Integer> emit(List<Object> tuple) {
			this.tuple = tuple;
			
			return null;
		}
		
	}
	
	private Tuple mockTuple(List<Object> values){
		Tuple tuple = mock(Tuple.class);
		when(tuple.getValueByField("message")).thenReturn(values.get(1));
		
		return tuple;
	}
	
	@Test
	public void testDMBolt() {
		DMPlaceholderBolt dmBolt = new DMPlaceholderBolt();
		
		TestCollector collector = new TestCollector(null);
		
		dmBolt.prepare(null, null, collector);
		
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", "0024a4dc00003354");
		attrs.put("density", 2.0);
		
		//TODO add more attributes
		
		Event event = SpeeddEventFactory.getInstance().createEvent("PredictedCongestion", 0, attrs);
		
		Values values = new Values("1", event);
		
		dmBolt.execute(mockTuple(values));
		
		List<Object> outTuple = collector.tuple;

		assertNotNull(outTuple);
		
		Event actionEvent = (Event)outTuple.get(1);
		
		double meterRate = (double)actionEvent.getAttributes().get("newMeterRate");
		
		assertEquals(2, meterRate, 0);
	}
}
