package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.speedd.data.Event;

public class EventDrivenObserver {
	private network roadnetwork;
	private long lastUpdate = 0;
	
	private final int n_iter = 4;
	private final double T_PERIOD = 15./3600.; // (h)
	private final double T_MEASUREMENT = n_iter * T_PERIOD; // (h)
	private final double L_DENS = 0.5; // 0.5;
	private final double L_FLOW = 0.5; // 0.8;
	private final double H2MS = 3600. * 1000.; // convert hours to milliseconds
	
	private Map<Integer,Double> flowsMap;
	private Map<Integer,Double> ncarsMap;
	protected Map<Integer,Double[]> tlpmap = new HashMap<Integer,Double[]>();
	protected Map<Integer,Double> external_inflowsMap = new HashMap<Integer,Double>();
	
	
	/**
	 * Constructor for event-driven extended kalman filter
	 * @param roadnetwork - reference to road network that is observed
	 */
	public EventDrivenObserver(network roadnetwork) {
		this.roadnetwork = roadnetwork;
	}
	
	/**
	 * Function responsible for unpacking events
	 * @param e - event to be handled
	 * @return
	 */
	public void processEvent(Event event) {
		
		String eventName = event.getEventName();
		
        if (eventName.equals("AverageDensityAndSpeedPerLocation") || eventName.equals("AverageOnRampValuesOverInterval"))
        {
        	// read attributes
    		long timestamp = event.getTimestamp();
        	Map<String, Object> attributes = event.getAttributes();
        	if ((attributes.get("average_flow") != null) && (attributes.get("average_occupancy") != null) && (attributes.get("sensorId") != null)) {
        		// continue
        		double flow = (Integer) attributes.get("average_flow")/1.0;
        		
            	double dens = (8/5) * 125 * ((double) attributes.get("average_occupancy")); // Conversion factor: 8m space per 5m car, 125cars/km max. occup.
            	int sensorId = Integer.parseInt((String) attributes.get("sensorId"));
        		
            	// check if sensor is present in network
                if (roadnetwork.sensor2road.get(sensorId) != null) { // BEGIN#1
                	int roadId = roadnetwork.sensor2road.get(sensorId);
                	double ncars = dens * roadnetwork.Roads.get(roadId).params.l; // conversion density --> number of cars
                	
                	// INITIALIZATION
                	if (lastUpdate == 0) {
                		lastUpdate = timestamp - (long) (1.5 * H2MS * T_MEASUREMENT); // initialize
                	}
                	
                	// PREDICTION STEP - if one period has passed
                	if (timestamp - lastUpdate > (H2MS * T_MEASUREMENT) ) {
                		ncarsMap = new HashMap<Integer,Double>(); // clear variable
            			flowsMap = new HashMap<Integer,Double>(); // clear variable
            			
            			// because of stability constraints, need to do multiple distinct steps
                		for(int ii=0; ii<n_iter; ii++) {
                			// Comment: active traffic light signals are stored internally in the network structure. Therefore, 
                			// in the prediction of the flows, an empty structure is passed.
    	            		Map<Integer,Double> flowsMap_ii = roadnetwork.predictFlows(new HashMap<Integer,Double[]>(), T_PERIOD);
    	            		ncarsMap = roadnetwork.predictDensity(flowsMap_ii,external_inflowsMap);
    	            		flowsMap = AddMaps.add(flowsMap_ii,flowsMap);
    	            		roadnetwork.initDensitites(ncarsMap); // set densities according to predictions
                		}
                		lastUpdate += H2MS * T_MEASUREMENT;
                	} 
                	
                	// CORRECTION STEP - every time a measurement arrives
                	Road road = roadnetwork.Roads.get(roadId);
                	
                	if ((road.sensor_begin == sensorId) && (road.intersection_begin == -1)) {
                		// EXTERNAL INFLOW, add vehicles to network
                		
                		double inflow_prediction = 0.;
                		if (external_inflowsMap.get(roadId) != null) {
                			inflow_prediction = n_iter * external_inflowsMap.get(roadId);
                		}
                        road.updateDensity(flow - inflow_prediction);
                        external_inflowsMap.put(roadId,flow/n_iter); // saveback: for short-term external demand prediction  
                	} else { 
                		// INTERNAL MEASUREMENT
                		
                		// DENSITY UPDATE
                		if (!road.type.equals("onramp")) {
                			road.updateDensity(L_DENS * (ncars - ncarsMap.get(roadId)));
                		} else if (road.type.equals("onramp") && (road.sensor_begin == sensorId)) {
                			// queue sensor: can only be used to correct density which is too small
                			if (ncars > ncarsMap.get(roadId)) {
                				road.updateDensity(L_DENS * (ncars - ncarsMap.get(roadId)));
                			}
                		} 
                		
                		// FLOW UPDATE
                		if (road.sensor_begin == sensorId) {
                			// inflow in road - add cars
                			if (flowsMap.get(roadId) != null) {
                				road.updateDensity(L_FLOW * (flow - flowsMap.get(roadId))); // need INFLOW (+index)
                			} else {
                				road.updateDensity(L_FLOW * (flow - 0)); // no predicted flow available...
                			}
                			
                			// FIXME: Patch in later?
//                			// Correct incoming road
//                			IntersectionFifo intersection_begin = (IntersectionFifo) roadnetwork.Intersections.get(road.intersection_begin);
//                			if (intersection_begin != null) {
//    	            			if (intersection_begin.roads_in.length == 1) {
//    	            				// only ONE incoming road
//    	            				
//    	            				int upstream_id = intersection_begin.roads_in_ID[0];
//    	            				Road road_in = roadnetwork.Roads.get(upstream_id);
//    	            				
//    	            				if ((road_in != null) && (road_in.sensor_end == -1)) {
//    	            					// if no separate sensor
//    	            					double upstream_flow = flowsMap.get(-upstream_id); // need upstream OUTFLOW (-index)
//    	            					int jj = findIndex(intersection_begin.roads_out_ID, roadId);
//    	            					flow = flow / intersection_begin.params.TPM[jj][0];
//    	            					// System.out.println(sensorId + " " + upstream_id + "flow: " + flow + " expected: " + upstream_flow); // DEBUG
//    	            					road_in.updateDensity(-L_FLOW * (flow - upstream_flow)); 
//    	            					flowsMap.put(upstream_id, upstream_flow - flow); // update flow table: part of the flow has been accounted for
//    	            				} // else: if more than one incoming road, cannot infer anything meaningful.
//    	            			}
//                			}
                			
                			
                		} else if (road.sensor_end == sensorId) {
                			// outflow from road - subtract cars
                			road.updateDensity(-L_FLOW * (flow - flowsMap.get(-roadId))); // need OUTFLOW (-index)
                			
                			// FIXME: Patch in later?
//                			// Correct outgoing roads.
//                			IntersectionFifo intersection_end = (IntersectionFifo) roadnetwork.Intersections.get(road.intersection_end);
//                			int jj = findIndex(intersection_end.roads_in_ID, roadId);
//                			
//                			if ((jj != -1) && (intersection_end.roads_in_ID[jj] == roadId)) {
//    	            			for (int ii=0; ii<intersection_end.roads_out.length; ii++) {
//    	            				// iterate over downstream roads
//    	            				int downstream_id = intersection_end.roads_out_ID[ii];
//    	            				Road road_out = roadnetwork.Roads.get(downstream_id);
//    	            				if ((road_out != null) && (road_out.sensor_begin != -1)) {
//    	            					// if no separate sensor
//    	            					double flow_out = flow * intersection_end.params.TPM[ii][jj];
//    	            					
//    	            					double downstream_flow = flowsMap.get(downstream_id); // need downstream INFLOW (+index)
//    	            					road_out.updateDensity(L_FLOW * (flow_out - downstream_flow));
//    	            					flowsMap.put(downstream_id, downstream_flow - flow); // update flow table: part of the flow has been accounted for
//    	            				}
//    	            			}
//                			} else {
//                				throw(new IllegalArgumentException("Error in sensor-road lookup-table. You should never see this error."));
//                			}
                			
                		} else {
                			throw(new IllegalArgumentException("Error in sensor-road lookup-table. You should never see this error."));
                		}
                		
                	} // END if (internal measurement)
    	            	
            	} // END if (sensor present in network)

        	}  // END if (event fields are present)
            
        } // END if (eventName)
        
	} // END processEvent()
	
	private int findIndex(int[] array, int key) {
		for (int ii=0; ii<array.length; ii++) {
			if (array[ii] == key) {
				return ii;
			}
		}
		return -1;
	}
	
	
}
