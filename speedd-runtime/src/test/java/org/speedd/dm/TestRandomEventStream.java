package org.speedd.dm;

import static org.junit.Assert.*;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestRandomEventStream {
	
	Random randGen = new Random();

	@Test
	public void test() {
		
		int N = 10000; // number of events to be played
		int DT = 15* 1000; // max. time between two events, in ms
		
		final int p_congestion = 15;
		final int p_clear = 5;
		final int p_mainline = 50;
		final int p_onramp = 20;
		final int p_limits = 10;
		
		OutputCollector collector = new OutputCollector(null);
		
		TrafficDecisionMakerBolt myBolt = new TrafficDecisionMakerBolt();
		myBolt.prepare(null,null,collector);
		
		long timestamp = randGen.nextInt(1000000);
		
		for (int ii=0;ii<N;ii++) {
			
			// advance time
			timestamp += randGen.nextInt(DT);
			
			int eventVar = randGen.nextInt(p_congestion + p_clear + p_mainline + p_onramp + p_limits);
			
			// create random event
			Event newEvent;
			if (eventVar < p_congestion) {
				newEvent = createCongestion(timestamp);
			} else if (eventVar < p_congestion + p_clear) {
				newEvent = clearCongestion(timestamp);
			} else if (eventVar < p_congestion + p_clear + p_mainline) {
				newEvent = createMainline(timestamp);
			} else if (eventVar < p_congestion + p_clear + p_mainline + p_onramp) {
				newEvent = createOnramp(timestamp);
			} else if (eventVar < p_congestion + p_clear + p_mainline + p_onramp + p_limits) {
				newEvent = createLimits(timestamp);
			} else {
				newEvent = null;
			}
			
			if (newEvent != null) {
				System.out.println(newEvent.getEventName());
			}
			
			// send to "bolt"
			 myBolt.execute(newEvent);
			 
			
		}
		
		System.out.println("Finished!");

		
		
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

	// functions to create events
	private Event createCongestion(long timestamp) {
		// Create a congestion event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		return SpeeddEventFactory.getInstance().createEvent("PredictedCongestion", timestamp, attrs);
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
		attrs.put("average_flow", randGen.nextDouble() * 4000);
		return SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPersensorIdOverInterval", timestamp, attrs);
	}
	private Event createOnramp(long timestamp) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		sensorData newSensor = getSensor();
		attrs.put("sensorId", newSensor.sensorId);
		attrs.put("dmPartition", newSensor.dmPartition);
		attrs.put("average_occupancy", randGen.nextDouble() * 125.);
		attrs.put("average_flow", randGen.nextDouble() * 2000);
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

}

class sensorData {
	String sensorId;
	String dmPartition;
	
	sensorData(String sensorId, String dmPartition) {
		this.sensorId = sensorId;
		this.dmPartition = dmPartition;
	}
}
