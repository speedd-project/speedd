package org.speedd.dm;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;



/**
 * DM traffic use case V2.0:
 * Implementation of general traffic network
 *
 * @author mschmitt
 *
 */
public class network {
    public final Map<Integer,Intersection> Intersections;
    public final Map<Integer,Road> Roads;
    public final Map<Integer,Integer> sensor2road;
    public final Map<Integer,Integer> actuator2intersection;
    
    // constructor - Grenoble rocade (hardcoded) 
    public network(String networkId) {
    	// Convention for sensor labels:-
		// sens_in : sensor on the mainline, flow INto a cell
		// sens_ou : sensor on the mainline, flow OUt of a cell
		// sens_qu : sensor on beginning of a QUeue
		// sens_on : sensor on an ONramp entering the freeway
		// sens_of : sensor on an OFframp
        if (networkId.equals("section1")) {
			// road parameters   1     2     3    (4)
			int[] sens_in = {   -1,   -1,   -1,   -1};
			int[] sens_ou = { 4087, 4084, 4244,   -1};   
			int[] sens_qu = {   -1, 4085,   -1,   -1};
			int[] sens_on = { 1708, 1703,   -1,   -1};
			int[] sens_of = {   -1,   -1,   -1,   -1};
			double[] beta = {   0.,   0.,   0.,   0.};
			int[] actu_id = {   -1, 4489,   -1,   -1};
			
			double[] ql   = {  0.1,  0.4,  -1.,  -1.};
			double[] length={  0.5,  0.4,  0.8,  0.4};
			double[] v    = {  90.,  90.,  90.,  90.};
			double[] rhoc = {  50.,  50.,  50.,  50.};
			double[] rhom = { 250., 250., 250., 250.};
			
			// network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,1);
			network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,v, rhoc, rhom,1);
			
			this.Intersections = buffer.Intersections;
			this.Roads = buffer.Roads;
        } else if (networkId.equals("section2")) {
			// road parameters   4     5     6     7     8     9   (10)
			int[] sens_in = {   -1,   -1,   -1, 3812,   -1,   -1,   -1};
			int[] sens_ou = {   -1, 3813,   -1, 3811, 3810, 4355,   -1};   
			int[] sens_qu = {   -1,  - 1,   -1, 4132,   -1,   -1,   -1};
			int[] sens_on = {   -1, 1687,   -1, 1679, 1675,   -1,   -1};
			int[] sens_of = { 1691,   -1, 1683,   -1,   -1,   -1,   -1};
			double[] beta = {  0.2,   0.,  0.2,   0.,   0.,   0.,   0.};
			int[] actu_id = {   -1,   -1,   -1, 4488,   -1,   -1,   -1};
			
			double[] ql   = {  -1., 0.12,  -1., 0.15, 0.12,  -1.,  -1.};
			double[] length={  0.4,  0.4,  0.8,  0.5, 0.45, 0.75,  1.3}; 
			double[] v    = {  90.,  90.,  90.,  90.,  90.,  90.,  90.};
			double[] rhoc = {  50.,  50.,  50.,  50.,  50.,  50.,  50.};
			double[] rhom = { 250., 250., 250., 250., 250., 250., 250.};
			
			//network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,4);
			network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,v, rhoc, rhom,4);
			
			this.Intersections = buffer.Intersections;
			this.Roads = buffer.Roads;
        } else if (networkId.equals("section3")) {
			// road parameters  10    11   11o   (12)
			int[] sens_in = {   -1, 4061,       4391};
			int[] sens_ou = {   -1, 4381,         -1};   
			int[] sens_qu = {   -1, 4134,         -1};
			int[] sens_on = {   -1, 1666,         -1};
			int[] sens_of = { 1670,   -1,         -1};
			double[] beta = {  0.2,   0.,         0.};
			int[] actu_id = {   -1, 4487,         -1};
			
			double[] ql   = {  -1.,  0.3,        -1.};
			double[] length={  1.3,  0.5,        0.9};
			double[] v    = {  90.,  90.,        90.};
			double[] rhoc = {  50.,  50.,        50.};
			double[] rhom = { 250., 250.,       250.};
			
			//network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,10);
			network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,v, rhoc, rhom,10);
			
			this.Intersections = buffer.Intersections;
			this.Roads = buffer.Roads;
        } else if (networkId.equals("section4")) {
			// road parameters  12    13    14    15   15c   (16)
			int[] sens_in = { 4391, 4375,   -1, 4057,       4166};
			int[] sens_ou = {   -1, 4058,   -1, 4056,         -1};   
			int[] sens_qu = {   -1, 4135,   -1, 4136,         -1};
			int[] sens_on = {   -1, 1658,   -1, 1650,         -1};
			int[] sens_of = { 1662,   -1, 1654,   -1,         -1};
			double[] beta = {  0.2,   0.,  0.2,   0.,         0.};
			int[] actu_id = {   -1, 4486,   -1, 4453,         -1};
			
			double[] ql   = {  -1.,  0.2,  -1.,  0.2,        -1.};
			double[] length={  0.9,  0.5, 0.65, 0.55,       0.65};
			double[] v    = {  90.,  90.,  90.,  90.,        90.};
			double[] rhoc = {  50.,  50.,  50.,  50.,        50.};
			double[] rhom = { 250., 250., 250., 250.,       250.};
			
			//network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,13);
			network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,v, rhoc, rhom,12);
			
			this.Intersections = buffer.Intersections;
			this.Roads = buffer.Roads;
        } else if (networkId.equals("section5")) {
			// road parameters  16    17    18    19    20
			int[] sens_in = { 4166, 4055,   -1, 4053      };
			int[] sens_ou = {   -1, 4054,   -1, 4052      };   
			int[] sens_qu = {   -1, 4138,   -1,   -1      };
			int[] sens_on = {   -1, 1642,   -1, 1634      };
			int[] sens_of = { 1646,   -1, 1638,   -1      };
			double[] beta = {  0.2,   0.,  0.2,   0.      };
			int[] actu_id = {   -1, 4490,   -1,   -1      };
			
			double[] ql   = {  -1.,  0.2,  -1., 0.06      };
			double[] length={ 0.65, 0.55, 0.45,  0.4      };
			double[] v    = {  90.,  90.,  90.,  90.      };
			double[] rhoc = {  50.,  50.,  50.,  50.      };
			double[] rhom = { 250., 250., 250., 250.      };
			
			//network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,18);
			network buffer = makeFreeway(sens_in,sens_ou,sens_on,sens_qu,sens_of,actu_id,beta,length,ql,v, rhoc, rhom,16);
			
			this.Intersections = buffer.Intersections;
			this.Roads = buffer.Roads;
        } else {
        	this.Intersections = null;
        	this.Roads = null;
        	throw(new IllegalArgumentException("Unknwon subnetwork name."));
        }
        // create lookup tables
        this.actuator2intersection = this.makeActuator2Intersection();
        this.sensor2road = this.makeSensor2road();
    }
   
    /**
     * Constructor using an externally defined set of roads and intersections.
     * All IDs have to be set correctly, the references of intersections to 
     * adjacent roads are handled by this constructor.
     * @param Intersections	...
     * @param Roads			...
     */
    public network(Map<Integer,Intersection> Intersections, Map<Integer,Road> Roads) {
    	this.Intersections = Intersections;
    	this.Roads = Roads;
        // set references between roads and intersections
    	// consistency checks are done for every intersection
        for (Map.Entry<Integer,Intersection> entry : this.Intersections.entrySet()) {
    		entry.getValue().setRoadReferences(this.Roads);
        }
        // create lookup tables
        this.actuator2intersection = this.makeActuator2Intersection();
        this.sensor2road = this.makeSensor2road();
    }
    
    /**
     * Computes the traffic flows one step of length dt[h] ahead. This 
     * prediction is based on the model used in the underlying network 
     * instance, the states of the network and the chosen traffic light
     * signals. The predicted flows are returned, but the internal states
     * of the network are NOT updated.
     *
     * @param  iTLPmap  traffic light signals for intersections
     * @param  exDemand external traffic demand
     * @param  dt		simulation time
     * @return 			resulting flows in- and out of roads
     */
    public Map<Integer,Double> predictFlows(Map<Integer,Double[]> iTLFmap, double dt) {
    	
    	// define datastructure for flows
    	Map<Integer,Double> flows = new HashMap<Integer,Double>();
    	
    	// iterate over intersections to compute internal flows
    	for (Map.Entry<Integer,Intersection> entry : this.Intersections.entrySet()) {
    		Intersection myintersection = entry.getValue();
    		Double[] iTLP = iTLFmap.get(entry.getKey());
    		if (iTLP == null) {
    			iTLP = myintersection.defaultAction;
    		}
    		Map<Integer,Double> local_flows = myintersection.computeFlows(iTLP, dt);
    		flows.putAll(local_flows); // collect all flows
    	}
    	
    	return flows;
    } 
    
    /**
     * Predicts the number of cars in every road one step of length dt[h]
     * ahead. This prediction is based on the model used in the underlying
     * network instance, the states of the network and the chosen traffic light
     * signals. The predicted number of cars are returned. The internal state
     * is NOT updated
     *
     * @param  iTLPmap  traffic light signals for intersections
     * @param  exDemand external traffic demand
     * @param  dt		simulation time
     * @return 			predicted number of cars in every road
     */    
    public Map<Integer,Double> predictDensity(Map<Integer,Double> flows, Map<Integer,Double> externalDemand) {

    	Map<Integer,Double> ncars = new HashMap<Integer,Double>();
    	// iterate over roads
    	for (Map.Entry<Integer,Road> entry : this.Roads.entrySet()) {
    		// get 'road' object
    		Road myroad = entry.getValue();
    		double delta_ncars = 0;
    		
    		// get inflow
    		Double inflow = flows.get(entry.getKey());
    		if (inflow != null) {
    			delta_ncars += inflow; 
    		}
    		// get outflow
    		Double outflow = flows.get(-entry.getKey());
    		if (outflow != null) {
    			delta_ncars -= outflow;
    		}
    		// get external flow
    		Double externalFlow = externalDemand.get(entry.getKey());
    		if (externalFlow != null) {
    			delta_ncars += externalFlow;
    		}

    		ncars.put(entry.getKey(), delta_ncars + myroad.ncars); // collect all densities
    	}
    	
    	return ncars; // return list of the new densities in the network
    }
    
    /**
     * Simple observer. Given a measurement of a system state, i.e. the 
     * measured number of cars in at least one road, the internal states of
     * the network are updated as a weighted average between measured state
     * and internal, predicted state. Chose updateRate = 1 to disregard the
     * internal state entirely.
     *
     * @param  densityMeasurements	map of road IDs and corresponding density
     * 								measurements
     * @param  updateRate			weighting factor between internal state and
     * 								measurements
     * @return 						list of updated densities in the network
     */ 
    public Map<Integer,Double> correctDensities(Map<Integer,Double> densityMeasurements, double updateRate) {
    	Map<Integer,Double> ncars = new HashMap<Integer,Double>();
    	// iterate over roads
    	for (Map.Entry<Integer,Road> entry : this.Roads.entrySet()) {
    		Road myroad = entry.getValue();
    		Integer road_id = entry.getKey();
    		
    		Double measurement = densityMeasurements.get(road_id);
    		if (measurement == null) {
    			ncars.put(road_id,myroad.ncars); // nothing changes
    		} else {
    			double new_density = (updateRate*measurement) + ((1-updateRate)*myroad.ncars);
    			myroad.setDensity(new_density);
    			ncars.put(road_id,new_density);
    		}
    	}
    	
    	return ncars; // return list of the new densities in the network
    }
    
    /**
     * Initialization function for the internal state of the network. The 
     * internal state (number of cars per road) is set to the values in the 
     * function parameter. If a road is missing from the parameter map, the
     * corresponding number of cars is set to zero.
     *
     * @param  densityMeasurements	map of road IDs and corresponding density
     * 								values
     */ 
    public void initDensitites(Map<Integer,Double> init_ncars) {
    	this.correctDensities(init_ncars,1.); // regression step with update factor 1 is equivalent to setting all the densities
    }
    
    /**
     * Print information about the network topology on the standard output.
     */
    public void printNetwork() {
    	// iterate over intersections
    	for (Map.Entry<Integer,Intersection> entry : this.Intersections.entrySet()) {
    		Intersection inter = entry.getValue();
    		String intersectionString = "Intersection "+inter.id+", in: ";
    		for (int ii=0;ii<inter.roads_in_ID.length;ii++) {
    			intersectionString = intersectionString + inter.roads_in_ID[ii] + " ";
    		}
    		intersectionString = intersectionString + ", out: ";
    		for (int ii=0;ii<inter.roads_out_ID.length;ii++) {
    			intersectionString = intersectionString + inter.roads_out_ID[ii] + " ";
    		}
    		System.out.println(intersectionString);
    	}
    	// iterate over roads
    	for (Map.Entry<Integer,Road> entry : this.Roads.entrySet()) {
    		Road myRoad = entry.getValue();
    		System.out.println("Road "+entry.getKey()+", from "+myRoad.intersection_begin+", to "+myRoad.intersection_end);
    	}
    }
    
	/**
	 * Creates a freeway network from the given parameters. TBD: complete documentation
	 * 
	 * @param sens_in
	 * @param sens_ou
	 * @param sens_on
	 * @param sens_qu
	 * @param sens_of
	 * @param actu_id
	 * @param beta
	 * @param length
	 * @param ql
	 * @return
	 */
	public static network makeFreeway(int[] sens_in, int[] sens_ou, int[] sens_on, int[] sens_qu, int[] sens_of, int[] actu_id, double[] beta, double[] length, double[] ql, int offset) {
		
		// create datastructures
		Map<Integer,Intersection> intersections = new HashMap<Integer,Intersection>();
		Map<Integer,Road> roads = new HashMap<Integer,Road>();
		
		// dimension checks
		int n = sens_in.length;
		if ((n >= 100) || (sens_ou.length != n) || (sens_on.length != n) || (sens_qu.length != n) || (sens_of.length != n) || (beta.length != n) || (actu_id.length != n)) {
			throw(new InvalidParameterException());
		}
		
		// iteratively build the freeway
		for (int ii=0; ii<n; ii++) {
			Ctm freewayCtm = new Ctm("freeway",length[ii]);
			Road cell; // allocate
			if (ii == 0) {
				cell = new RoadCtm(-1, ii+offset, sens_in[ii], sens_ou[ii], "freeway", freewayCtm); // "-1" indicates beginning of freeway
			} else {
				cell = new RoadCtm(ii+offset-1, ii+offset, sens_in[ii], sens_ou[ii], "freeway", freewayCtm);
			}
			roads.put(ii+offset, cell);
			
			// determine type of intersection. Assumption: no onramp and offramp at the same place!
			if (ii < n-1) {
				if (sens_on[ii] < 0) {
					// no onramp
					if (sens_of[ii] < 0) {
						// neither onramp nor offramp
						Fifo paramsFifo = Fifo.makeSplit();
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset}, new int[] {ii+offset+1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
					} else {
						// offramp, no onramp
						Fifo paramsFifo = Fifo.makeOfframp(beta[ii]);
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset}, new int[] {ii+offset+1,-1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
					}
				} else {
					if (sens_qu[ii] < 0) {
						// unmetered onramp
						Fifo paramsFifo = Fifo.makeOnramp();
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset,100+ii+offset}, new int[] {ii+offset+1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
						// also need to create the queue
						Ctm queueCtm = new Ctm("small",ql[ii]);
						Road queue = new RoadCtm(-1, ii+offset, sens_qu[ii], sens_on[ii], "onramp", queueCtm);
						roads.put(ii+offset+100, queue);
					} else {
						// metered onramp
						Fifo paramsFifo = Fifo.makeMeteredOnramp();
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset,100+offset+ii}, new int[] {ii+offset+1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
						// also need to create the queue
						Ctm queueCtm = new Ctm("small",ql[ii]);
						Road queue = new RoadCtm(-1, ii+offset, sens_qu[ii], sens_on[ii], "onramp", queueCtm);
						roads.put(ii+offset+100, queue);
					}
				}
			} else {
				// ii = n-1 : end of freeway
				Fifo paramsFifo = Fifo.makeSplit();
				IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset}, new int[] {-1}, actu_id[ii], paramsFifo); 
				intersections.put(ii+offset, intersection);
			}

		}
		
		return new network(intersections,roads); 
	}
	
	/**
	 * Creates a freeway network from the given parameters. TBD: complete documentation
	 * 
	 * @param sens_in
	 * @param sens_ou
	 * @param sens_on
	 * @param sens_qu
	 * @param sens_of
	 * @param actu_id    
	 * @param beta
	 * @param length
	 * @param ql
	 * @return
	 */
	public static network makeFreeway(int[] sens_in, int[] sens_ou, int[] sens_on, int[] sens_qu, int[] sens_of, int[] actu_id, double[] beta, 
			double[] length, double[] ql, double[] v, double[] rhoc, double[] rhom, int offset) {
		
		// create datastructures
		Map<Integer,Intersection> intersections = new HashMap<Integer,Intersection>();
		Map<Integer,Road> roads = new HashMap<Integer,Road>();
		
		// dimension checks
		int n = sens_in.length;
		if ((n >= 100) || (sens_ou.length != n) || (sens_on.length != n) || (sens_qu.length != n) || (sens_of.length != n) || (beta.length != n) || 
				(actu_id.length != n) || (beta.length != n) || (length.length != n) || (ql.length != n) || (v.length != n) || (rhoc.length != n) || (rhom.length != n)) {
			throw(new InvalidParameterException());
		}
		
		// iteratively build the freeway
		for (int ii=0; ii<n; ii++) {
			Ctm freewayCtm = new Ctm(v[ii], rhoc[ii], rhom[ii], length[ii]);
			Road cell; // allocate
			if (ii == 0) {
				cell = new RoadCtm(-1, ii+offset, sens_in[ii], sens_ou[ii], "freeway", freewayCtm); // "-1" indicates beginning of freeway
			} else {
				cell = new RoadCtm(ii+offset-1, ii+offset, sens_in[ii], sens_ou[ii], "freeway", freewayCtm);
			}
			roads.put(ii+offset, cell);
			
			// determine type of intersection. Assumption: no onramp and offramp at the same place!
			if (ii < n-1) {
				if (sens_on[ii] < 0) {
					// no onramp
					if (sens_of[ii] < 0) {
						// neither onramp nor offramp
						Fifo paramsFifo = Fifo.makeSplit();
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset}, new int[] {ii+offset+1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
					} else {
						// offramp, no onramp
						Fifo paramsFifo = Fifo.makeOfframp(beta[ii]);
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset}, new int[] {ii+offset+1,-1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
					}
				} else {
					if (sens_qu[ii] < 0) {
						// unmetered onramp
						Fifo paramsFifo = Fifo.makeOnramp();
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset,100+ii+offset}, new int[] {ii+offset+1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
						// also need to create the queue
						Ctm queueCtm = new Ctm("small",ql[ii]);
						Road queue = new RoadCtm(-1, ii+offset, sens_qu[ii], sens_on[ii], "onramp", queueCtm);
						roads.put(ii+offset+100, queue);
					} else {
						// metered onramp
						Fifo paramsFifo = Fifo.makeMeteredOnramp();
						IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset,100+offset+ii}, new int[] {ii+offset+1}, actu_id[ii], paramsFifo); 
						intersections.put(ii+offset, intersection);
						// also need to create the queue
						Ctm queueCtm = new Ctm("small",ql[ii]);
						Road queue = new RoadCtm(-1, ii+offset, sens_qu[ii], sens_on[ii], "onramp", queueCtm);
						roads.put(ii+offset+100, queue);
					}
				}
			} else {
				// ii = n-1 : end of freeway
				Fifo paramsFifo = Fifo.makeSplit();
				IntersectionFifo intersection = new IntersectionFifo(ii+offset, new int[] {ii+offset}, new int[] {-1}, actu_id[ii], paramsFifo); 
				intersections.put(ii+offset, intersection);
			}

		}
		
		return new network(intersections,roads); 
	}
	
	   
    /**
     * Create lookup table mapping sensorId to roadId. To be called in the
     * constructor.
     * @return lookup table
     */
    private Map<Integer,Integer> makeSensor2road() {
    	// create datastructure
    	Map<Integer,Integer> sensor2road = new HashMap<Integer,Integer>();
    	// iterate over roads
    	for (Map.Entry<Integer,Road> entry : this.Roads.entrySet()) {
    		if (entry.getValue().sensor_begin != -1) {
    			sensor2road.put(entry.getValue().sensor_begin, entry.getKey());
    		}
    		if (entry.getValue().sensor_end != -1) {
    			sensor2road.put(entry.getValue().sensor_end, entry.getKey());
    		}
    	}
    	return sensor2road;
    }
    
    /**
     * Create lookup table mapping actuatorId to intersectionId. To be called
     * in the constructor.
     * @return
     */
    private Map<Integer,Integer> makeActuator2Intersection() {
    	// create datastructure
    	Map<Integer,Integer> actuator2intersection = new HashMap<Integer,Integer>();
    	// iterate over roads
    	for (Map.Entry<Integer,Intersection> entry : this.Intersections.entrySet()) {
    		if (entry.getValue().ActuatorId != -1) {
    			actuator2intersection.put(entry.getValue().ActuatorId, entry.getKey());
    		}
    	}
    	return actuator2intersection;
    }
    
}





