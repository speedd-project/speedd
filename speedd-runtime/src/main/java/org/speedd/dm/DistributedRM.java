package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;


public class DistributedRM {
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	
	private Map<Integer,onrampStruct> sensor2onramp; // map congestion events to onramps
	private Map<Integer,onrampStruct> intersection2onramp; // 1-to-1 map of intersections to onramp
	private final network freeway;
	private final double dt = 60./3600.; // FIXME: 2min period length. Make parameter

	/** 
	 * Constructor, storing a reference to the network to be controlled.
	 * 
	 * @param freeway		reference to the network to be controlled
	 */
	public DistributedRM(network freeway) {
		this.freeway = freeway; // set reference to network object
		this.sensor2onramp = new HashMap<Integer,onrampStruct>();
		this.intersection2onramp = new HashMap<Integer,onrampStruct>();
	}
	
	/**
	 * Generic function to process events
	 * 
	 * @param eventName
	 * @param timestamp
	 * @param attributes
	 */
	public onrampStruct processEvent(String eventName, long timestamp, Map<String, Object> attributes) {
		
		String buffer = (String) attributes.get("sensorId");
		Integer sensor_Id = Integer.parseInt((String) attributes.get("sensorId"));
		onrampStruct localRamp = null;
		
		if (sensor_Id != null) {
			// find corresponding intersection (onramp merge)=============== //
			localRamp = sensor2onramp(sensor_Id);
			
			// process event =============================================== //
			if (localRamp != null) {
				if (eventName.equals("PredictedCongestion") || eventName.equals("Congestion")) {
					// turn on ramp metering
					localRamp.operationMode = 1;
				}
				else if (eventName.equals("ClearCongestion")) {
					// turn off ramp metering
					localRamp.operationMode = 0;
				}
				else if (eventName.equals("setMeteringRateLimits")) {
					// set metering rate limits
					Double minFlow = (Double)attributes.get("lowerLimit");
					if (minFlow != null) {
						if (minFlow >= 0) localRamp.minFlow = minFlow;
						else localRamp.minFlow = .0; // disable lower limit
					}
					Double maxFlow = (Double)attributes.get("upperLimit");
					if (maxFlow != null) {
						if (maxFlow >= 0) localRamp.maxFlow = maxFlow;
						else localRamp.maxFlow = 1800.; // disable upper limit
					}
				}
				else if (eventName.equals("AverageOnRampValuesOverInterval")) {
					Double onrampFlow = (Double)attributes.get("average_flow");
					// FIXME: Add test that field present. Add test that this is the QUEUE flow
					
					localRamp.dutycycle = computeDutyCycle(sensor_Id, onrampFlow, this.dt);
					
					// saveback: need to store active action
					this.freeway.Intersections.get(localRamp.ramp).activeAction = convertToTLP(localRamp.dutycycle);
				}
			}
		} else {
			throw(new IllegalArgumentException("Field sensorId in event attributes is empty."));
		}

		return localRamp;
	}
	
	/**
	 * Wrapper function for event handling (packing and unpacking...)
	 * 
	 * @param inEvent
	 * @return
	 */
	public Event processEvent2(Event inEvent) {
		
		// read event
		String eventName = inEvent.getEventName();
		long timestamp = inEvent.getTimestamp();
		Map<String, Object> attributes = inEvent.getAttributes();
		onrampStruct localOnramp =  processEvent(eventName, timestamp, attributes);
		
		// next line contains as last elment the trigger to issue new ramp metering commands
		if (!(localOnramp == null) && (localOnramp.operationMode >= 1) && (eventName.equals("AverageOnRampValuesOverInterval"))) {
			// Create Action Event
	        Map<String, Object> outAttrs = new HashMap<String, Object>();
	        outAttrs.put("newMeteringRate", localOnramp.dutycycle); // compute action
	        outAttrs.put("sensorId", Integer.toString(localOnramp.actuatorId));
	        outAttrs.put("dm_sensorId", (String)attributes.get("dm_sensorId"));
	        
	        Event outEvent = eventFactory.createEvent("UpdateMeteringRateAction", timestamp, outAttrs);
			return outEvent;
		}
		return null;

	}
	
	/**
	 * Compute the dutycycle (ramp metering rate) for a single intersection.
	 * ASSUMPTIONS:
	 *  - upstream and downstream road belong to subnetwork (true for Rocade)
	 *  - 
	 * 
	 * @param ramp		onramp-merge, 'intersection' where the onramp enters
	 * 					the mainline
	 * @param freeway	local road network, needs to contain at least the roads
	 * 					adjacent to 'ramp'
	 * @param dt		simulation time
	 * @param flows		predicted mainline flows
	 * @param demand	external traffic demand
	 * @return			recommended dutycycle
	 */
	public double computeDutyCycle(int sensorId, double demand, double dt) {
		
		onrampStruct localOnramp = sensor2onramp(sensorId);
		
		if (localOnramp != null) {
			if (localOnramp.operationMode > 0) {
				// figure out topology
				Intersection ramp = this.freeway.Intersections.get(localOnramp.ramp);
				if (ramp == null) {
					throw(new IllegalArgumentException("FIXME. Onramp merge intersection missing or ID invalid."));
				}
				int upstream_id = ramp.roads_in_ID[ findRoad(ramp.roads_in, "freeway") ];
				int downstream_id = ramp.roads_out_ID[ findRoad(ramp.roads_out, "freeway") ];
				Road upstream = this.freeway.Roads.get(upstream_id);
				Road downstream = this.freeway.Roads.get(downstream_id);
				Road onramp = this.freeway.Roads.get(ramp.roads_in_ID[ findRoad(ramp.roads_in, "onramp") ]);
				if ((upstream == null) || (downstream == null) || (onramp == null)) {
					throw(new IllegalArgumentException("FIXME. Upstream, downstream or onramp missing from controlled subnetwork."));
				}
				
				// obtain local parameters		
				double ncars = Math.max(upstream.ncars * (downstream.params.l/upstream.params.l), downstream.ncars); // FIXME: Add term for inflow of previous step.
				double ncars_c = downstream.params.rhoc * downstream.params.l;
				double qmax = onramp.params.rhom * onramp.params.l;
				double q = onramp.ncars;
				double phi_out = 0; // FIXME: predict internally
				double phi_in = 0; // FIXME: predict internally
				
				// compute inflow
				double rmax = Math.min(1., localOnramp.maxFlow/1800.);	// (cars/h) : one car every two seconds
				double rmin = Math.max(0., localOnramp.minFlow/1800.);	// (cars/h) : assume trivial lower bound
				
				double delta_n = ncars_c - (ncars + dt*(phi_in-phi_out));
				double r = delta_n/(dt*1800);          					// conversion: # of cars --> dutycycle
				rmin = Math.max(rmin, ((demand*dt)-(qmax-q))/(dt*1800));  	// conversion: # of cars --> dutycycle

				return Math.min(rmax, Math.max(rmin, r));
			}
			// FIXME: Check also if coordination mode is active.
		}
		return -1;
	}
	
	/**
	 * Convert dutycycle [0,1] to the duration of individual traffic light
	 * phases.
	 * FIXME: Add checks.
	 * 
	 * @param 		dutycycle	
	 * @return		durations of individual traffic light phases
	 */
	public static Double[] convertToTLP(double dutycycle) {
		return new Double[] {dutycycle, 1-dutycycle};
	}
	
	/**
	 * Wrapper function?
	 * @param sensorId
	 * @return
	 */
	public onrampStruct sensor2onramp(int sensorId) {
		// check if ramp metering parameters have been defined for this onramp
		onrampStruct localRamp = this.sensor2onramp.get(sensorId);
		if (localRamp == null) {
			// entry is missing from lookup table, perform search:
			int onrampMergeId = findCorrespondingOnramp(sensorId);
			if (onrampMergeId != -1) {
				// corresponding onramp exists, retrieve or create object
				localRamp = this.intersection2onramp.get(onrampMergeId);
				if (localRamp == null) {
					// object is missing, create new one
					localRamp = new onrampStruct(onrampMergeId, this.freeway.Intersections.get(onrampMergeId).ActuatorId);
					// populate fields
					localRamp.upstreamRamp = -1; // FIXME: implement for coordination
					localRamp.downstreamRamp = -1; // FIXME: implement for coordination
					// save reference
					this.intersection2onramp.put(onrampMergeId, localRamp);
				}
				// object exists, but new reference necessary
				sensor2onramp.put(sensorId, localRamp);
			}
			// else: no corresponding onramp exists, return null
		}
		return localRamp;
	}
	
	/**
	 * Finds the intersection ID of the onramp merge corresponding to a given
	 * sensor ID.
	 * 
	 * @param 	sensorId
	 * @return	intersectionID of onramp merge
	 */
	private int findCorrespondingOnramp(Integer sensorId) {
		// find roadId
		// Integer roadId = findRoadId(sensorId);
		Integer roadId = this.freeway.sensor2road.get(sensorId);
		Road thisRoad = this.freeway.Roads.get(roadId);
		if (thisRoad != null) {
			if (thisRoad.type == "freeway") {
				
				// Case 1: Road immediately downstream of onramp
				int downstreamIntersectionId = thisRoad.intersection_end;
				if (downstreamIntersectionId != -1) {
					Intersection downstreamIntersection = this.freeway.Intersections.get(downstreamIntersectionId);
					if ((findRoad(downstreamIntersection.roads_in, "onramp") != -1) && (downstreamIntersection.defaultAction.length == 2)){
						// there exists an onramp, terminate search
						return downstreamIntersectionId;
					}
				}
				
				// Case 2: Backtracking upstream, until onramp is found
				int upstreamIntersectionId = thisRoad.intersection_begin;
				while (upstreamIntersectionId != -1) {
					Intersection upstreamIntersection = this.freeway.Intersections.get(upstreamIntersectionId);
					if ((findRoad(upstreamIntersection.roads_in, "onramp") != -1) && (upstreamIntersection.defaultAction.length == 2)) {
						// (1) there exists an onramp, (2) two actuation signals, i.e. it's metered: terminate search
						return upstreamIntersectionId;
					}
					int roadIndex = findRoad(upstreamIntersection.roads_in, "freeway");
					upstreamIntersectionId = (upstreamIntersection.roads_in[roadIndex]).intersection_begin;
				}
			} else if (thisRoad.type == "onramp") {
				return thisRoad.intersection_end;
			}
		}
		
		// check if actuator id
		Integer intersectionId = this.freeway.actuator2intersection.get(sensorId);
		if (intersectionId != null) {
			return intersectionId;
		}
		
		return -1; // no onramp found
	}
	
	/**
	 * Auxiliary function, that finds a road of a particular type ('freeway',
	 * 'onramp', ...) from an array of roads and returns the index. Only the 
	 * index of the first road of the given type found is returned.
	 * 
	 * @param roads		array of roads
	 * @param type		type of road to be searched for
	 * @return			index of first hit
	 */
	private static int findRoad(Road[] roads, String type) {
		for (int ii=0; ii<roads.length; ii++) {
			if (roads[ii].type.equals(type)) {
				return ii;
			}
		}
		return -1;
	}
	
	 	
	
}
