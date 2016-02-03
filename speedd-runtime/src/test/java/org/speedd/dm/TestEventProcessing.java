package org.speedd.dm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

import backtype.storm.task.IOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TestEventProcessing {
	
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
		TrafficDecisionMakerBolt dmBolt = new TrafficDecisionMakerBolt();
		
		TestCollector collector = new TestCollector(null);
		
		dmBolt.prepare(null, null, collector);
		
		// Play events
		Random rand = new Random(); 
		
		// (a) no limits active
		double flow_qu = 20;
		double dens_qu = 0;
		double flow_ma = 0;
		double dens_ma = 0;
		
		dmBolt.execute(mockTuple(new Values("1", createCongestion(4056,"section4")))); // activate ramp metering
		dmBolt.execute(mockTuple(new Values("1", createMainline(4056,"section4",flow_ma,dens_ma))));
		dmBolt.execute(mockTuple(new Values("1", createOnramp(4136,"section4",flow_qu,dens_qu))));
		List<Object> outTuple = collector.tuple;
		assertNotNull(outTuple);
		Event actionEvent = (Event)outTuple.get(1);
		double new_rate = 1.; // FIXME: Add correct value
		verifyAction(actionEvent, 3995, new_rate);
		
		
		// (b) upper limit
		flow_ma = 0;
		dens_ma = 0;
		double upperLimit = 900; // random flow < onramp flow
		
		dmBolt.execute(mockTuple(new Values("1", createMainline(4056,"section4",flow_ma,dens_ma))));
		dmBolt.execute(mockTuple(new Values("1", createLimits(3995,"section4", -1, upperLimit))));
		dmBolt.execute(mockTuple(new Values("1", createOnramp(4136,"section4",flow_qu,dens_qu))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, 3995, upperLimit/1800); // upper limit
		
		// (c) nonsensical identifier "location"
		collector.tuple = null;
		try{
			dmBolt.execute(mockTuple(new Values("1", createOnramp(4136,"abcde",0,0) )));
		} catch(IllegalArgumentException e) {
			// success
		}
		dmBolt.execute(mockTuple(new Values("1", createOnramp(12345,"section4",0,0) )));
		try {
			dmBolt.execute(mockTuple(new Values("1", createCongestion(4056,"abcde") )));
		} catch(IllegalArgumentException e) {
			// success
		}
		dmBolt.execute(mockTuple(new Values("1", createCongestion(12345,"section4") )));
		outTuple = collector.tuple;
		assertNull(outTuple);
		
//		// (d) lower limit
//		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",400))));
//		dmBolt.execute(mockTuple(new Values("1", createLimits("0024a4dc00003354", 200, -1))));
//		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",200)))); // high density, lower limit will be ative
//		outTuple = collector.tuple;
//		assertNotNull(outTuple);
//		actionEvent = (Event)outTuple.get(1);
//		verifyAction(actionEvent, "0024a4dc00003354", 200);
//		
//		// (e) exact critical density
//		flow = (int) (200 + 80 * rand.nextDouble()); // onramp flow within limits: 200 <= flow <= 280
//		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",flow))));
//		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354", 64))));
//		outTuple = collector.tuple;
//		assertNotNull(outTuple);
//		actionEvent = (Event)outTuple.get(1);
//		verifyAction(actionEvent, "0024a4dc00003354", flow);
//		
//		// (f) missing flow measurement
//		collector.tuple = null;
//		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354", 0)))); // low density, but no recent flow measurement
//		outTuple = collector.tuple;
//		assertNull(outTuple);
//		
//		// (g) remove lower limit
//		dmBolt.execute(mockTuple(new Values("1", createLimits("0024a4dc00003354", -1, -1) )));
//		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",300) )));
//		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",188))));
//		outTuple = collector.tuple;
//		assertNotNull(outTuple);
//		actionEvent = (Event)outTuple.get(1);
//		verifyAction(actionEvent, "0024a4dc00003354", 0); // lower limit removed: zero is absolut lower limit
//		
//		// (h) disable ramp metering
//		collector.tuple = null;
//		dmBolt.execute(mockTuple(new Values("1", clearCongestion("0024a4dc00003354"))));
//		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",300) )));
//		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",77))));
//		outTuple = collector.tuple;
//		assertNull(outTuple);
//		
//		// (i) Reactivate ramp metering: old limits should still be in place
//		dmBolt.execute(mockTuple(new Values("1", createCongestion("0024a4dc00003354"))));
//		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",300) )));
//		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",188))));
//		outTuple = collector.tuple;
//		assertNotNull(outTuple);
//		actionEvent = (Event)outTuple.get(1);
//		verifyAction(actionEvent, "0024a4dc00003354", 0); // lower limit removed: zero is absolut lower limit
		
		// dmBolt.execute(mockTuple(new Values("",  )));
		
	}
	
	// functions to create events
	private Event createCongestion(int location, String dmlocation) {
		// Create a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("dm_location", dmlocation);
		return SpeeddEventFactory.getInstance().createEvent("PredictedCongestion", System.nanoTime(), attrs);
	}
	private Event clearCongestion(int location, String dmlocation) {
		// Clear a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("dm_location", dmlocation);
		return SpeeddEventFactory.getInstance().createEvent("ClearCongestion", System.nanoTime(), attrs);
	}
	private Event createMainline(int location, String dmlocation, double density, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("dm_location", dmlocation);
		attrs.put("average_density", density);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("mainlineAverages", System.nanoTime(), attrs);
	}
	private Event createOnramp(int location, String dmlocation, double density, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("dm_location", dmlocation);
		attrs.put("average_density", density);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("onrampAverages", System.nanoTime(), attrs);
	}
	private Event createOfframp(int location, String dmlocation, double density, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("dm_location", dmlocation);
		attrs.put("average_density", density);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("offrampAverages", System.nanoTime(), attrs);
	}
	private Event createLimits(int location, String dm_location, double lower, double upper) {
		// Create a limit event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("dm_location", dm_location);
		attrs.put("lowerLimit", lower);
		attrs.put("upperLimit", upper);
		return SpeeddEventFactory.getInstance().createEvent("setMeteringRateLimits", System.nanoTime(), attrs);
	}

	// function to verify actions
	private void verifyAction(Event actionEvent, int expected_actu_id, double expectedMeterRate) {
		// read attributes
		Double meterRate = (Double)actionEvent.getAttributes().get("newMeteringRate");
		int actuator_id = (int)actionEvent.getAttributes().get("location");
		String dm_location = (String)actionEvent.getAttributes().get("dm_location");
		// compare
		assertEquals(expected_actu_id, actuator_id);
		assertEquals(expectedMeterRate, meterRate, 1e-3);
	}
	
}