package org.speedd.dm;

import java.util.Map;

public abstract class Intersection {
	// abstract class containing topology information of one intersection
	    public int id;
	    public int[] roads_in_ID; // index value of incoming roads
	    public int[] roads_out_ID; // index value of outgoing roads
	    protected Road[] roads_in; // reference to incoming roads (-objects)
	    protected Road[] roads_out; // reference to outgoing roads (-objects)
	    public int ActuatorId;
	    public int nTLP; // Assume discrete input: number of traffic light phases
	    public Double[] defaultAction; // Assumption: ...
	    public Double[] activeAction = defaultAction; // Assumption: Until new speedd-action, chosen action is repeated
	    
	    // no constructor, abstract class
	    
	    // set references to roads
	    public void setRoadReferences(Map<Integer,Road> Roads) {
	    	// Note: Roads leaving the network have index -1. In this case, no corresponding value
	    	// is found in the dictionary and null is returned.
	    	this.roads_in = new Road[roads_in_ID.length];
	    	for(int ii=0;ii<roads_in_ID.length;ii++) {
	    		if (roads_in_ID[ii] >= 0) {
	    			Road inRoad = Roads.get(roads_in_ID[ii]);
	    			if (inRoad != null) {
	    				// incoming road found
	    				if (inRoad.intersection_end == this.id) {
	    					// IDs are consistent
	    					this.roads_in[ii] =  inRoad; // set reference in intersection object to road object
	    				} else {
	    					// IDs are inconsistent
	    					throw(new IllegalArgumentException("At intersection "+this.id+", incoming road with ID "+roads_in_ID[ii]+" was expected, but this road ends at intersection "+inRoad.intersection_end));
	    				}
	    				
	    			} else {
	    				// invalid id
	    				throw(new IllegalArgumentException("At intersection "+this.id+", incoming road with ID "+roads_in_ID[ii]+" was not found."));
	    			}
	    		} else {
	    			// invalid, incoming roads must not be empty
	    			throw(new IllegalArgumentException("At intersection "+this.id+", incoming road has invalid ID "+roads_in_ID[ii]+", but road ID may not be negative."));
	    		}
	    	}
	    	this.roads_out = new Road[roads_out_ID.length];
	    	for(int ii=0;ii<roads_out_ID.length;ii++) {
	    		int outRoadId = roads_out_ID[ii];
	    		if (outRoadId >= 0) {
	    			// valid id
	    			Road outRoad = Roads.get(roads_out_ID[ii]);
	    			if (outRoad != null) {
		    			if (outRoad.intersection_begin == this.id) {
		    				// IDs are consistent
		    				this.roads_out[ii] = outRoad; // set reference in intersection object to road object
		    			} else {
		    				// IDs are inconsistent
		    				throw(new IllegalArgumentException("At intersection "+this.id+", outgoing road with ID "+roads_out_ID[ii]+" was expected, but this road begins at intersection "+outRoad.intersection_end));
	    				}	    				
	    			} else {
	    				// invalid id
	    				throw(new IllegalArgumentException("At intersection "+this.id+", incoming road with ID "+roads_out_ID[ii]+" was not found."));
	    			}
	    		} else {
	    			if (outRoadId == -1) {
	    				// end of the network
	    				this.roads_out[ii] = null;
	    			} else {
	    				// invalid id
	    				throw(new IllegalArgumentException("At intersection "+this.id+", outgoing road has invalid ID "+roads_out_ID[ii]+", but road ID may not be negative."));
	    			}
	    		}
	    		
	    		
	    		
	    	}
	    }

	    // compute flows - this depends on the model of the intersection dynamics
	    public abstract Map<Integer,Double> computeFlows(Double[] iTLF, double dt);
	}
