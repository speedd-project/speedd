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
    Map<String, subnetData> subnetDataMap = new HashMap<String, subnetData>();
    
    public final double dt = 15/3600;
    public final double tperiod = 120/3600;


	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		Event event = (Event)input.getValueByField("message");
		
		// read event
		String eventName = event.getEventName();
		long timestamp = event.getTimestamp();
		Map<String, Object> attributes = event.getAttributes();
		// attribute "dm_location" needs to be present, since partitioning is done according to it
		String DMlocation = (String) attributes.get("dm_location");
		// attribute "location" needs to be present, since that is also required
		Integer location = (Integer) attributes.get("location"); // FIXME: there should be a check.
		
		
		if (DMlocation != null)
		{
            
			network freeway = networkMap.get(DMlocation); // read sub-network data
            if (freeway == null) {
                // create instance of subnetwork if not yet created
                freeway = new network(DMlocation);
                // save instance
                networkMap.put(DMlocation,freeway);

            }
            DistributedRM distController = distRMmap.get(DMlocation);
            if (distController == null) {
                // create instance of controller if not yet created
                distController = new DistributedRM(freeway);
                // save instance
                distRMmap.put(DMlocation,distController);
            }
            subnetData localData = subnetDataMap.get(DMlocation);
            if (localData == null) {
                // create instance of data if not yet created
                localData = new subnetData();
                // save instance
                subnetDataMap.put(DMlocation,localData);
            }  
            // now everything is present ...
            
            if (eventName.equals("mainlineAverages") || eventName.equals("onrampAverages"))
            {
            	// check if external inflow
            	int roadId = distController.findRoadId(location);
	            	if (roadId != -1) {
	            	// FIXME: Some implicit assumptions about a sensor being on the road entering a subnetwork here...
	            	if (freeway.Roads.get(roadId).intersection_begin == -1) {
	            		// external inflow
	            		// just save value
	            		Double inflow = (Double) attributes.get("average_flow");
	                    localData.externalDemand.put(location, inflow);
	            	} else {
	            		// internal measurement
	            		// potentially simulate for one step
	            		Double density = (Double) attributes.get("average_density");
	                    localData.densityMeasurements.put(location, density);
	                    Double flow = (Double) attributes.get("average_flow");
	                    localData.flowMeasurements.put(location,  flow);
	                    
	                    // Update system state
	                    if ((timestamp - localData.tUpdate) > dt) {
	                    	double T = timestamp - localData.tUpdate;
	                        // predict flows
	                    	Map<Integer,Double> flows = freeway.predictFlows(localData.iTLPmap, T);
	                    	// Flow correction step
	                    	flows = this.updateData(flows, localData.flowMeasurements, 1.); // FIXME: Magic number --> estimate variance instead?
	                    	// predict densities
	                    	Map<Integer,Double> densities = freeway.predictDensity(flows, localData.externalDemand);  	
	                    	// Density correction step
	                    	densities = this.updateData(densities, localData.densityMeasurements, 1.); // FIXME: Magic number --> estimate variance instead?
	                    	// saveback
	                    	freeway.initDensitites(densities);
	                    	
	                    	// clear measurement structs
	                    	localData.densityMeasurements.clear();
	                    	localData.flowMeasurements.clear();
	                    	localData.externalDemand.clear();
	                    	// reset time of last measurement
	                    	localData.tUpdate = timestamp;
	                    }
	            	}
            	}
            }
 
			
			if (eventName.equals("PredictedCongestion") || eventName.equals("Congestion") || eventName.equals("ClearCongestion") ||
					eventName.equals("setMeteringRateLimits") || eventName.equals("RampCooperation") || eventName.equals("rampOverFlow") ||
					eventName.equals("clearRampOverFlow") || eventName.equals("onrampAverages")) {	
				// Call ProcessEvent to deal with the event
				Event outEvent = distController.processEvent2(event);
				
				if (outEvent != null) {
	                // Use sensor labels for partitioning by kafka
	                collector.emit(new Values(location, outEvent));
	                // Save decision also locally - FIXME: Move this functionality to other function
					if (outEvent.getEventName().equals("newMeteringRate")) {
						// FIXME: move functionality
					}
					
				}


			}
			
			// Events "AverageDensityAndSpeedPerLocation" are not used, we use the 2minsAverage... instead.
			networkMap.put(DMlocation, freeway); // saveback local network state
		} else {
			logger.warn("location is null for tuple " + input);
			// Discard event and do nothing. Could throw an exception, report an error etc.
		}
				
	}


	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}
    
    private class subnetData {
    	public Map<Integer,Double[]> iTLPmap = new HashMap<Integer,Double[]>();
    	public Map<Integer,Double> externalDemand = new HashMap<Integer,Double>();
        public Map<Integer,Double> flowMeasurements = new HashMap<Integer,Double>();
        public Map<Integer,Double> densityMeasurements = new HashMap<Integer,Double>();
        public long tUpdate = 0;
    }
    
    private Map<Integer,Double> updateData(Map<Integer,Double> estimate, Map<Integer,Double> measurement, double lambda) {
    	if ((lambda > 1) || (lambda < 0)) {
    		throw(new IllegalArgumentException("Expect 0 <= lambda <= 1, but lambda = "+lambda));
    	}
    	// iterate over entries
    	Map<Integer,Double> update = new HashMap<Integer,Double>();
    	for (Map.Entry<Integer,Double> entry : estimate.entrySet()) {
    		Integer key = entry.getKey();
    		if (measurement.get(key) != null) {
    			// corresponding measurement exists
    			update.put(key, (1-lambda)*entry.getValue() + lambda*measurement.get(key));
    		} else {
    			update.put(key, entry.getValue());
    		}
    	}
    	return update;
    }

}
