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
	private final double OCCU_DENS_CONVERSION = 100;
	
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
		
		// NOTE: THE FOLLOWING TESTS REQUIRE THAT INSTEAD OF PROPER FILTERING, DENSITY MEASUREMENTS ARE ASSUMED TO BE EXACT.
		
		// (a) no limits active
		double flow_qu = 20;
		double dens_qu = 0;
		double flow_ma = 0;
		double dens_ma = 0;
		
		dmBolt.execute(mockTuple(new Values("1", createCongestion(0, 4056,"section4")))); // activate ramp metering
		dmBolt.execute(mockTuple(new Values("1", createMainline(0, 4056,"section4",dens_ma,flow_ma))));
		dmBolt.execute(mockTuple(new Values("1", createOnramp(0, 4136,"section4",dens_qu,flow_qu))));
		List<Object> outTuple = collector.tuple;
		assertNotNull(outTuple);
		Event actionEvent = (Event)outTuple.get(1);
		double new_rate = 1.; // FIXME: Add correct value
		verifyAction(actionEvent, "4453", new_rate);
		
		
		// (b) upper limit
		flow_ma = 0;
		dens_ma = 0;
		double upperLimit = 900; // random flow < onramp flow
		
		dmBolt.execute(mockTuple(new Values("1", createMainline(0, 4056,"section4",dens_ma,flow_ma))));
		dmBolt.execute(mockTuple(new Values("1", createLimits(0, 4453,"section4", -1, upperLimit))));
		dmBolt.execute(mockTuple(new Values("1", createOnramp(0, 4136,"section4",dens_qu,flow_qu))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "4453", upperLimit/1800); // upper limit
		
		// (c) nonsensical identifier "sensorId"
		collector.tuple = null;
		try{
			dmBolt.execute(mockTuple(new Values("1", createOnramp(0, 4136,"abcde",0,0) )));
		} catch(IllegalArgumentException e) {
			// success
		}
		dmBolt.execute(mockTuple(new Values("1", createOnramp(0, 12345,"section4",0,0) )));
		try {
			dmBolt.execute(mockTuple(new Values("1", createCongestion(0, 4056,"abcde") )));
		} catch(IllegalArgumentException e) {
			// success
		}
		dmBolt.execute(mockTuple(new Values("1", createCongestion(0, 12345,"section4") )));
		outTuple = collector.tuple;
		assertNull(outTuple);
		
		// (d) lower limit
		flow_ma = 0;
		dens_ma = 200; // high density, lower limit will be active 
		flow_qu = 100;
		dens_qu = 0;
		double lowerLimit = 180; // random flow < onramp flow
		
		dmBolt.execute(mockTuple(new Values("1", createMainline(30, 4056,"section4",dens_ma,flow_ma)))); // mainline congestion, just upstream
		dmBolt.execute(mockTuple(new Values("1", createMainline(30, 4166,"section4",dens_ma,flow_ma)))); // mainline congestion, just downstream
		dmBolt.execute(mockTuple(new Values("1", createLimits(30, 4453,"section4", lowerLimit, -1))));
		dmBolt.execute(mockTuple(new Values("1", createOnramp(31, 4136,"section4",dens_qu,flow_qu))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "4453", lowerLimit/1800); // upper limit
		
		// get some references that will be useful for writing robust tests
		network freeway = dmBolt.networkMap.get("section4");
		
		// (e) exact critical density
		double rhoc = freeway.Roads.get(freeway.sensor2road.get(4166)).params.rhoc;
		dens_ma = rhoc;
		dmBolt.execute(mockTuple(new Values("1", createMainline(60, 4166,"section4",dens_ma,flow_ma)))); // critical density
		dmBolt.execute(mockTuple(new Values("1", createMainline(60, 4056,"section4",dens_ma-10,flow_ma)))); // critical density	
		dmBolt.execute(mockTuple(new Values("1", createLimits(60, 4453,"section4", -1, -1)))); // disable limits
		dmBolt.execute(mockTuple(new Values("1", createOnramp(60, 4136,"section4",dens_qu,0))));
		outTuple = collector.tuple;
		assertNotNull(outTuple);
		actionEvent = (Event)outTuple.get(1);
		verifyAction(actionEvent, "4453", 0.); // upper limit
		
		// (f) disable ramp metering
		collector.tuple = null;
		dmBolt.execute(mockTuple(new Values("1", clearCongestion(90, 4166,"section4"))));
		dmBolt.execute(mockTuple(new Values("1", createOnramp(60, 4136,"section4",dens_qu,100))));
		outTuple = collector.tuple;
		assertNull(outTuple);
		
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
	private Event createCongestion(long timestamp, int sensorId, String dmsensorId) {
		// Create a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmsensorId);
		return SpeeddEventFactory.getInstance().createEvent("PredictedCongestion", timestamp, attrs);
	}
	private Event clearCongestion(long timestamp, int sensorId, String dmsensorId) {
		// Clear a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmsensorId);
		return SpeeddEventFactory.getInstance().createEvent("ClearCongestion", timestamp, attrs);
	}
	private Event createMainline(long timestamp, int sensorId, String dmsensorId, double density, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmsensorId);
		attrs.put("average_occupancy", density/OCCU_DENS_CONVERSION);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPersensorIdOverInterval", timestamp, attrs);
	}
	private Event createOnramp(long timestamp, int sensorId, String dmsensorId, double density, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmsensorId);
		attrs.put("average_occupancy", density/OCCU_DENS_CONVERSION);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("AverageOnRampValuesOverInterval", timestamp, attrs);
	}
	private Event createOfframp(long timestamp, int sensorId, String dmsensorId, double density, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmsensorId);
		attrs.put("average_occupancy", density/OCCU_DENS_CONVERSION);
		attrs.put("average_flow", flow);
		return SpeeddEventFactory.getInstance().createEvent("offrampAverages", timestamp, attrs);
	}
	private Event createLimits(long timestamp, int sensorId, String dmPartition, double lower, double upper) {
		// Create a limit event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmPartition);
		attrs.put("lowerLimit", lower);
		attrs.put("upperLimit", upper);
		return SpeeddEventFactory.getInstance().createEvent("setMeteringRateLimits", timestamp, attrs);
	}

	// function to verify actions
	private void verifyAction(Event actionEvent, String expected_actu_id, double expectedMeterRate) {
		// read attributes
		Double meterRate = (Double)actionEvent.getAttributes().get("newMeteringRate");
		String actuator_id = (String) actionEvent.getAttributes().get("sensorId");
		// String dmPartition = (String)actionEvent.getAttributes().get("dmPartition");
		// compare
		assertEquals(expected_actu_id, actuator_id);
		assertEquals(expectedMeterRate, meterRate, 1e-3);
	}
	
}