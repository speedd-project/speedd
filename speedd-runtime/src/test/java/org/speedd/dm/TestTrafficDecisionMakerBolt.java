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

import backtype.storm.event__init;
import backtype.storm.task.IOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TestTrafficDecisionMakerBolt {
	
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
		double flow = 400;
		double density = 44 + 40*rand.nextDouble(); // random density 44<=density<=84, critical_density = 64
		density = 64;
		dmBolt.execute(mockTuple(new Values("1", createCongestion("0024a4dc00003354")))); // activate ramp metering
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",flow))));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",density))));
		List<Object> outTuple = collector.tuple;
		assertNotNull(outTuple);
		Event actionEvent = (Event)outTuple.get(1);
		double new_rate = 400 + (3.6*70/64) * (64-density); // no limits active
		verifyAction(actionEvent, "0024a4dc00003354", new_rate);
		
		// (b) upper limit
		density = 50*rand.nextDouble(); // random density < critical density
		double upperLimit = 380 - 100*rand.nextDouble(); // random flow < onramp flow
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",400))));
		dmBolt.execute(mockTuple(new Values("1", createLimits("0024a4dc00003354", -1, upperLimit))));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",(int) density)))); // low density, upper limit will be active
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "0024a4dc00003354", upperLimit); // upper limit
		
		// (c) nonsensical identifier "location"
		collector.tuple = null;
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("abcde",300) )));
		dmBolt.execute(mockTuple(new Values("1", createCongestion("abcde") )));
		outTuple = collector.tuple;
		assertNull(outTuple);
		
		// (d) lower limit
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",400))));
		dmBolt.execute(mockTuple(new Values("1", createLimits("0024a4dc00003354", 200, -1))));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",200)))); // high density, lower limit will be ative
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "0024a4dc00003354", 200);
		
		// (e) exact critical density
		flow = (int) (200 + 80 * rand.nextDouble()); // onramp flow within limits: 200 <= flow <= 280
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",flow))));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354", 64))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "0024a4dc00003354", flow);
		
		// (f) missing flow measurement
		collector.tuple = null;
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354", 0)))); // low density, but no recent flow measurement
		outTuple = collector.tuple;
		assertNull(outTuple);
		
		// (g) remove lower limit
		dmBolt.execute(mockTuple(new Values("1", createLimits("0024a4dc00003354", -1, -1) )));
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",300) )));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",188))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "0024a4dc00003354", 0); // lower limit removed: zero is absolut lower limit
		
		// (h) disable ramp metering
		collector.tuple = null;
		dmBolt.execute(mockTuple(new Values("1", clearCongestion("0024a4dc00003354"))));
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",300) )));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",77))));
		outTuple = collector.tuple;
		assertNull(outTuple);
		
		// (i) Reactivate ramp metering: old limits should still be in place
		dmBolt.execute(mockTuple(new Values("1", createCongestion("0024a4dc00003354"))));
		dmBolt.execute(mockTuple(new Values("1", createOnRampFlow("0024a4dc00003354",300) )));
		dmBolt.execute(mockTuple(new Values("1", createDensity("0024a4dc00003354",188))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "0024a4dc00003354", 0); // lower limit removed: zero is absolut lower limit
		
		// dmBolt.execute(mockTuple(new Values("",  )));
		
	}
	
	// functions to create events
	private Event createCongestion(String location) {
		// Create a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		return SpeeddEventFactory.getInstance().createEvent("PredictedCongestion", System.nanoTime(), attrs);
	}
	private Event clearCongestion(String location) {
		// Clear a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		return SpeeddEventFactory.getInstance().createEvent("ClearCongestion", System.nanoTime(), attrs);
	}
	private Event createOnRampFlow(String location, double flow) {
		// Create a flow measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("OnRampFlow", System.nanoTime(), attrs);
	}
	private Event createDensity(String location, double density) {
		// Create a flow measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("average_density", density);
		return SpeeddEventFactory.getInstance().createEvent("2minsAverageDensityAndSpeedPerLocation", System.nanoTime(), attrs);
	}
	private Event createLimits(String location, double lower, double upper) {
		// Create a limit event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("location", location);
		attrs.put("lowerLimit", lower);
		attrs.put("upperLimit", upper);
		return SpeeddEventFactory.getInstance().createEvent("setMeteringRateLimits", System.nanoTime(), attrs);
	}

	// function to verify actions
	private void verifyAction(Event actionEvent, String expectedLocation, double expectedMeterRate) {
		assertEquals("UpdateMeteringRateAction", actionEvent.getEventName());
		// read attributes
		Double meterRate = (Double)actionEvent.getAttributes().get("newMeteringRate");
		String location = (String)actionEvent.getAttributes().get("location");
		// compare
		assertEquals(expectedLocation, location);
		assertEquals(expectedMeterRate, meterRate, 1e-3);
	}
	
}


