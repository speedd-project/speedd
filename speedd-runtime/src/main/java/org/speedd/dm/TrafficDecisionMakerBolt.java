package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * DM traffic use case V2.0
 * TBD
 * 
 * @author mschmitt, cramesh, akofman
 *
 */
public class TrafficDecisionMakerBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	private OutputCollector collector;
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	Logger logger = LoggerFactory.getLogger(TrafficDecisionMakerBolt.class);
	
	Map<String, network> networkMap = new HashMap<String, network>();
	Map<String, DistributedRM> distRMmap = new HashMap<String, DistributedRM>();
    Map<String, eventDrivenObserver> observerMap = new HashMap<String, eventDrivenObserver>();
    
    final Boolean DEBUG = false;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	/**
	 * Wrapper function for main routine, executed whenever an event arrives.
	 * @param input: contains the event
	 */
	@Override
	public void execute(Tuple input) {
		Event event = (Event)input.getValueByField("message");
		execute(event);
	}
	
	/**
	 * Main routine, separated for testing purposes
	 * @param event
	 */
	public void execute(Event event) {	
		if (event == null) {
			return; // do nothing
		}
		
		// read event
		String eventName = event.getEventName();
		long timestamp = event.getTimestamp();
		Map<String, Object> attributes = event.getAttributes();
		String dmPartition = (String) attributes.get("dmPartition"); // attribute "dm_sensorId" needs to be present
		Integer sensorId;
		try {
			sensorId = Integer.parseInt((String) attributes.get("sensorId")); // attribute "sensorId" needs to be present
		} catch (NumberFormatException e) {
			sensorId = null; // will make the event being ignored
		}
		
		
		if ((dmPartition != null) && (sensorId != null))
		{
			// ============================================================== //
			// INTERNAL status update --> create controller and observer instance, if necessary
			network freeway = networkMap.get(dmPartition); // read sub-network data
            if (freeway == null) {
                // create instance of subnetwork if not yet created
            	try {
            		freeway = new network(dmPartition);
            	} catch(IllegalArgumentException e) {
            		return; // if name of dmPartition is not known, do nothing
            	}
                // save instance
                networkMap.put(dmPartition, freeway);
            }
            DistributedRM controller = distRMmap.get(dmPartition);
            if (controller == null) {
                // create instance of controller if not yet created
                controller = new DistributedRM(freeway);
                // save instance
                distRMmap.put(dmPartition, controller);
            }
            eventDrivenObserver observer = observerMap.get(dmPartition);
            if (observer == null) {
                // create instance of data if not yet created
                observer = new eventDrivenObserver(freeway);
                // save instance
                observerMap.put(dmPartition, observer);
            }  
            // now everything is present ...
            
            // ============================================================== //
            // MEASUREMENT event --> hand over to OBSERVER
            if (eventName.equals("AverageDensityAndSpeedPerLocation") || eventName.equals("AverageOnRampValuesOverInterval"))
            {
            	observer.processEvent(event);
        		if (freeway.sensor2road.get(sensorId) != null) { // check if valid sensor id
        			if ((freeway.Roads.get(freeway.sensor2road.get(sensorId)).sensor_begin == sensorId) && (freeway.Roads.get(freeway.sensor2road.get(sensorId)).type.equals("onramp"))) {
	            		int roadId = freeway.sensor2road.get(sensorId);
	            		double maxQueueLength = freeway.Roads.get(roadId).params.l * 1000; // conversion km --> m
	            		double queueLength = freeway.Roads.get(roadId).ncars * 8; // conversion [cars] --> m  	         
	            		
	            		Event outEvent = makeOnrampEvent(dmPartition, sensorId, queueLength, maxQueueLength, timestamp);
	            		
	            		// Emit event: Use DM partition for partitioning by kafka
	                	outEvent = addLocation(outEvent);
	                	collector.emit(new Values(dmPartition, outEvent));
        			}
        		}
            	
            }
 
            // ============================================================== //
			// COMPLEX event --> hand over to CONTROLLER
			if (eventName.equals("PredictedCongestion") || eventName.equals("Congestion") || eventName.equals("ClearCongestion") ||
					eventName.equals("setMeteringRateLimits") || eventName.equals("RampCooperation") || eventName.equals("PredictedRampOverflow") ||
					eventName.equals("ClearRampOverflow") || eventName.equals("AverageOnRampValuesOverInterval") || eventName.equals("AverageDensityAndSpeedPerLocation")) {	
				// Call ProcessEvent to deal with the event
				Event[] outEvents = controller.processEvent(event);
				
				if (outEvents != null) {
	                for (int ii=0; ii<outEvents.length; ii++) {
	                	Event outEvent = addLocation(outEvents[ii]);
						// Use DM partition for partitioning by kafka
	                	collector.emit(new Values("1", outEvent));	
	                } 
	                
				}

			}
			
		} else {
			logger.warn("dmPartition and/or sensorId is null or invalid for tuple " + event);
			// Discard event and do nothing. Could throw an exception, report an error etc.
		}
				
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}
	
    /**
     * Create an aggregatedQueueLengthEvent.
     * @param dmPartition
     * @param sensorId
     * @param queueLength
     * @param maxQueueLength
     * @param timestamp
     * @return
     */
	private Event makeOnrampEvent(String dmPartition, int sensorId, Double queueLength, Double maxQueueLength, long timestamp) {
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("certainty", 1.0); // FIXME: Use variance as a proxy.
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", dmPartition);
		attrs.put("queueLength", queueLength);
		attrs.put("maxQueueLength", maxQueueLength);
		return eventFactory.createEvent("AggregatedQueueRampLength", timestamp, attrs);
	}
    
	/**
	 * Function to add "location" attribute to events.
	 * TO BE REMOVED LATER.
	 */
	private Event addLocation(Event event) {
		String eventName = event.getEventName();
		long timestamp = event.getTimestamp();
		Map<String, Object> attributes = event.getAttributes();
		
		String sensorId = (String) attributes.get("sensorId");
		if (sensorId == null) {
			sensorId = (String) attributes.get("junction_id");
		}
		attributes.put("location", lookupLocation(sensorId));
		return eventFactory.createEvent(eventName, timestamp, attributes);
	}
	
	/**
	 * Lookup table for "old" location id.
	 * TO BE REMOVED LATER.
	 */
	private String lookupLocation(String s) {
		if (s.equals("4078") || s.equals("1708")) {
			return new String("0024a4dc00003356");
		} else 		if (s.equals("4048") || s.equals("1703") || s.equals("4085") || s.equals("4489")) {
			return new String("0024a4dc00003354");
		} else 		if (s.equals("4244")) {
			return new String("0024a4dc0000343c");
		} else 		if (s.equals("1687") || s.equals("1691") || s.equals("3813") || s.equals("3814")) {
			return new String("0024a4dc0000343b");
		} else 		if (s.equals("1679") || s.equals("1683") || s.equals("3811") || s.equals("4132") || s.equals("3812") || s.equals("4488")) {
			return new String("0024a4dc00003445");
		} else 		if (s.equals("3810") || s.equals("1675") || s.equals("3810")) {
			return new String("0024a4dc00001b67");
		} else 		if (s.equals("4355")) {
			return new String("0024a4dc00003357");
		} else 		if (s.equals("4061") || s.equals("1670") || s.equals("4134")) {
			return new String("0024a4dc00000ddd");
		} else 			if (s.equals("4381") || s.equals("1666") || s.equals("4391") || s.equals("4487")) {
			return new String("0024a4dc00003355");
		} else 			if (s.equals("4375") || s.equals("1662") || s.equals("4135") || s.equals("4486")) {
			return new String("0024a4dc000021d1");
		} else 			if (s.equals("4058") || s.equals("1658")) {
			return new String("0024a4dc0000343f");
		} else 			if (s.equals("4057") || s.equals("1654") || s.equals("4136")) {
			return new String("0024a4dc00001b5c");
		} else 			if (s.equals("4056") || s.equals("1650") || s.equals("4166") || s.equals("4453")) {
			return new String("0024a4dc000025eb");
		} else 			if (s.equals("4055") || s.equals("1646")) {
			return new String("0024a4dc000025ea");
		} else 			if (s.equals("4138")) {
			return new String("0024a4dc00001c99");
		} else 			if (s.equals("4054") || s.equals("1642") || s.equals("4490")) {
			return new String("0024a4dc000013c6");
		} else 			if (s.equals("4053") || s.equals("1638")) {
			return new String("0024a4dc00003444");
		} else 			if (s.equals("4052") || s.equals("1634")) {
			return new String("0024a4dc000025ec");
		} else 			if (s.equals("1629") || s.equals("1630") || s.equals("1628")) {
			return new String("0024a4dc0000343e");
		} else {
			return new String("locationIdNotFound");
		}
	}
}


