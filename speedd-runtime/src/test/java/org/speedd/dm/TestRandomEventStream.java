package org.speedd.dm;

import static org.junit.Assert.*;
import backtype.storm.task.IOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.TupleImpl;
import backtype.storm.tuple.Values;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;
import org.speedd.dm.TestEventProcessing.TestCollector;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestRandomEventStream {
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

	Random randGen = new Random();

	@Test
	public void test() {
		String timeEventStr = "AverageOnRampValuesOverInterval";
		
		int N = 1000000; // number of events to be played
		int DT = 15* 1000; // max. time between two events, in ms
		long[] timedata;
		int[] eventType;
		timedata = new long[N];
		eventType = new int[N];

		// ======= events to consider =======
		// DONE - "PredictedCongestion"
		//  - "Congestion"
		// DONE - "ClearCongestion"
		// DONE - "setMeteringRateLimits" 
		//  - "RampCooperation" 
		//  - "PredictedRampOverflow" 
		//  - "ClearRampOverflow" 
		//  - "AverageOnRampValuesOverInterval" 
		// DONE - "AverageDensityAndSpeedPerLocation"
		final int p_predictedCongestion = 10;
		final int p_congestion = 5;
		final int p_clear = 10;
		final int p_limits = 10;
		final int p_coordinate = 0; // not implemented yet
		final int p_overflow = 5;
		final int p_clearOverflow = 5;
		final int p_onramp = 20;
		final int p_mainline = 35;
		
		TestCollector  collector = new TestCollector(null);
		
		TrafficDecisionMakerBolt myBolt = new TrafficDecisionMakerBolt();
		myBolt.prepare(null,null,collector);
		
		long timestamp = randGen.nextInt(1000000);
		
		int N_setTrafficLightPhases = 0;
		int N_aggregatedQueueRampLength = 0;
		
		for (int ii=0;ii<N;ii++) {
			
			// advance time
			timestamp += randGen.nextInt(DT);
			
			int eventVar = randGen.nextInt(p_predictedCongestion + p_congestion + p_clear + p_limits + p_coordinate + p_overflow + p_clearOverflow + p_onramp + p_mainline);
			
			// create random event
			Event newEvent;
			if (eventVar < p_predictedCongestion) {
				newEvent = createPredictedCongestion(timestamp);
				eventType[ii] = 1;
			} else if (eventVar < p_predictedCongestion + p_congestion) {
				newEvent = createCongestion(timestamp);
				eventType[ii] = 2;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear) {
				newEvent = clearCongestion(timestamp);
				eventType[ii] = 3;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear + p_limits) {
				newEvent = createLimits(timestamp);
				eventType[ii] = 4;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear + p_limits + p_coordinate) {
				newEvent = createCoordinate(timestamp);
				eventType[ii] = 5;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear + p_limits + p_coordinate + p_overflow) {
				newEvent = createOverflow(timestamp);
				eventType[ii] = 6;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear + p_limits + p_coordinate + p_overflow + p_clearOverflow) {
				newEvent = createClearOverflow(timestamp);
				eventType[ii] = 7;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear + p_limits + p_coordinate + p_overflow + p_clearOverflow + p_onramp) {
				newEvent = createOnramp(timestamp);
				eventType[ii] = 8;
			} else if (eventVar < p_predictedCongestion + p_congestion + p_clear + p_limits + p_coordinate + p_overflow + p_clearOverflow + p_onramp + p_mainline) {
				newEvent = createMainline(timestamp);
				eventType[ii] = 9;
			} else {
				eventType[ii] = -1;
				newEvent = null;
			}
			
			if (newEvent != null) {
				// System.out.println(newEvent.getEventName());
			}
			
			// send to "bolt"
			long starttime = System.nanoTime();
			myBolt.execute(newEvent);
			long stoptime = System.nanoTime();
			timedata[ii] = stoptime-starttime;

			 
			 List<Object> outTuple = collector.tuple;
			 if (outTuple != null) {
				 Event outEvent = (Event)outTuple.get(1);
				 if (outEvent.getEventName().equals("SetTrafficLightPhases")) {
					 double phase_time = (double)((int)outEvent.getAttributes().get("phase_time"));
					 String junction_id = (String) outEvent.getAttributes().get("junction_id");
					 assertTrue(phase_time >= 0);
					 assertTrue(phase_time <= 60);
					 assertTrue(junction_id.equals("4489") || junction_id.equals("4488") || junction_id.equals("4487") || 
							 junction_id.equals("4486") || junction_id.equals("4453") || junction_id.equals("4490"));
					 N_setTrafficLightPhases += 1;
				 } else if (outEvent.getEventName().equals("AggregatedQueueRampLength")) {
					 double queueLength = (Double)(outEvent.getAttributes().get("queueLength"));
					 double maxQueueLength = (Double)(outEvent.getAttributes().get("maxQueueLength"));
					 String sensor_id = (String) outEvent.getAttributes().get("sensorId");
					 assertTrue(queueLength >= 0);
					 assertTrue(maxQueueLength >= queueLength);
					 assertTrue(sensor_id.equals("4085") || sensor_id.equals("4132") || sensor_id.equals("4134") || 
							 sensor_id.equals("4135") || sensor_id.equals("4136") || sensor_id.equals("4138"));
					 N_aggregatedQueueRampLength += 1;
				 }
				 
			 }
			 	
			 
			
		}
		
		// write times to file
		PrintWriter writer;
		try {
			writer = new PrintWriter("duration.txt", "UTF-8");
			for (int ii=0;ii<N;ii++) {
				writer.println(String.valueOf(timedata[ii]));
			}
			writer.close();
			writer = new PrintWriter("eventType.txt", "UTF-8");
			for (int ii=0;ii<N;ii++) {
				writer.println(String.valueOf(eventType[ii]));
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		System.out.println("Finished!");
		System.out.println("Processed " + N_setTrafficLightPhases + " setTrafficLightPhases events");
		System.out.println("Processed " + N_aggregatedQueueRampLength + " aggregatedQueueRampLength events");

		
		
	}
	
	private sensorData getSensor( ) {
		
		final String[] dmPartitions = {"section1","section2","section3","section4","section5",  "wrongName"};
		
		int dmPartitionId = randGen.nextInt(dmPartitions.length);
		String dmPartition = dmPartitions[dmPartitionId];
		String sensorId;
		
		if (dmPartitionId == 0) {
			final String[] sensorIds = {"4087","4084","4244","4085","1708","1703",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 1) {
			final String[] sensorIds = {"3812","3813","3811","3810","4355","4132","1687","1679","1675","1691","1683",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 2) {
			final String[] sensorIds = {"4061","4381","4391","4134","1666","1670",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 3) {
			final String[] sensorIds = {"4375","4057","4058","4056","4166","4135","4136","1658","1650","1662","1654", "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 4) {
			final String[] sensorIds = {"4055","4053","4054","4052","4138","1642","1634","1646","1638",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else {
			final String[] sensorIds = {"4087","4084","4244","4085","1708","1703",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		}
		return new sensorData(sensorId,dmPartition);
	}
	private sensorData getOnramp( ) {
		
		final String[] dmPartitions = {"section1","section2","section3","section4","section5",  "wrongName"};
		
		int dmPartitionId = randGen.nextInt(dmPartitions.length);
		String dmPartition = dmPartitions[dmPartitionId];
		String sensorId;
		
		if (dmPartitionId == 0) {
			final String[] sensorIds = {"4085","1708","1703",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 1) {
			final String[] sensorIds = {"4132","1687","1679","1675",   "1691","0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 2) {
			final String[] sensorIds = {"4061","4381","4391","4134","1666",   "1670","0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 3) {
			final String[] sensorIds = {"4135","4136","1658","1650",   "1654","0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else if (dmPartitionId == 4) {
			final String[] sensorIds = {"4138","1642","1634",   "1638","0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		} else { // Those are random numbers to test robustness...
			final String[] sensorIds = {"4087","4084","4244","4085","1708","1703",  "0000","notANumber"};
			sensorId = sensorIds[randGen.nextInt(sensorIds.length)];
		}
		return new sensorData(sensorId,dmPartition);
	}

	// ======= events to consider =======
	// DONE - "PredictedCongestion"
	//  - "Congestion"
	// DONE - "ClearCongestion"
	// DONE - "setMeteringRateLimits" 
	//  - "RampCooperation" 
	//  - "PredictedRampOverflow" 
	//  - "ClearRampOverflow" 
	//  - "AverageOnRampValuesOverInterval" 
	// DONE - "AverageDensityAndSpeedPerLocation"
	
	// functions to create events
	private Event createPredictedCongestion(long timestamp) {
		// Create a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		return SpeeddEventFactory.getInstance().createEvent("PredictedCongestion", timestamp, attrs);
	}
	private Event createCongestion(long timestamp) {
		// Create a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		return SpeeddEventFactory.getInstance().createEvent("Congestion", timestamp, attrs);
	}
	private Event clearCongestion(long timestamp) {
		// Clear a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		return SpeeddEventFactory.getInstance().createEvent("ClearCongestion", timestamp, attrs);
	}
	private Event createMainline(long timestamp) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		attrs.put("average_occupancy", 50 + randGen.nextDouble() * 200.);
		attrs.put("average_flow", randGen.nextInt() * 4000);
		return SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocation", timestamp, attrs);
	}
	private Event createOnramp(long timestamp) {
		// Create an onramp measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		attrs.put("average_occupancy", randGen.nextDouble() * 125.);
		attrs.put("average_flow", randGen.nextInt() * 2000);
		return SpeeddEventFactory.getInstance().createEvent("AverageOnRampValuesOverInterval", timestamp, attrs);
	}
	private Event createLimits(long timestamp) {
			// Create a limit event
			Map<String, Object> attrs = new HashMap<String, Object>();
			sensorData newSensor = getSensor();
			attrs.put("sensorId", newSensor.sensorId);
			attrs.put("dmPartition", newSensor.dmPartition);
			attrs.put("lowerLimit", randGen.nextDouble()*1000);
			attrs.put("upperLimit", randGen.nextDouble()*2000);
			return SpeeddEventFactory.getInstance().createEvent("setMeteringRateLimits", timestamp, attrs);
		}
	private Event createCoordinate(long timestamp) {
		// Create a congestion event
		/* Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		return SpeeddEventFactory.getInstance().createEvent("Congestion", timestamp, attrs); */
		return null; // not implemented yet
	}
	private Event createOverflow(long timestamp) {
		// Create a ramp overflow event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getOnramp();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		attrs.put("certainty", 50); // FIXME: make random
		return SpeeddEventFactory.getInstance().createEvent("PredictedRampOverflow", timestamp, attrs);
	}	
	private Event createClearOverflow(long timestamp) {
		// Create a clear ramp overflow event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getOnramp();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		return SpeeddEventFactory.getInstance().createEvent("ClearRampOverflow", timestamp, attrs);
	}
}

class sensorData {
	String sensorId;
	String dmPartition;
	
	sensorData(String sensorId, String dmPartition) {
		this.sensorId = sensorId;
		this.dmPartition = dmPartition;
	}
}
