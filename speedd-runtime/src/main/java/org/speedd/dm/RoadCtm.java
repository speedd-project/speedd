package org.speedd.dm;

public class RoadCtm extends Road {	    
	
    // default constructor: all parameters are set by the user
    public RoadCtm(int intersection_begin, int intersection_end, Integer sensor_begin, Integer sensor_end, String type, Ctm params) {
        this.intersection_begin = intersection_begin;
        this.intersection_end = intersection_end;
        this.sensor_begin = sensor_begin;
        this.sensor_end = sensor_end;
        this.type = type;
        this.params = params;
    }
	
    // get demand
    public double getDemand() {
    	return Math.min(this.params.F, this.params.v * this.ncars/this.params.l);
    }
    // get supply
    public double getSupply() {
    	return Math.min(this.params.F, this.params.rhom - this.ncars/this.params.l) * this.params.w;
    }

}
