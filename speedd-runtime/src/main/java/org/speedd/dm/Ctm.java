package org.speedd.dm;

public class Ctm {
	// class containing ctm parameters for one road
	    public Double v;
	    public Double w;
	    public Double rhoc;
	    public Double rhom;
	    public Double l;
	    public double F = 0;
	    
	    // default constructor: all parameters are set by the user
	    public Ctm(Double v, Double rhoc, Double rhom, Double l) {
	        this.v = v;
	        this.w = v * (rhoc)/(rhom-rhoc);
	        this.rhoc = rhoc;
	        this.rhom = rhom;
	        this.l = l;
	        this.F = v*rhoc;
	    }
	    
	    // "lazy" constructor: uses default values for different road types
	    public Ctm(String type, Double length) {
	        this.l = length; // no default value
	        if(type.equals("freeway")) {
	            this.v = 90.;
	            this.rhoc = 50.;
	            this.rhom = 250.;
	        }
	        else if(type.equals("city")) {
	            this.v = 50.;
	            this.rhoc = 50.;
	            this.rhom = 250.;
	        }
	        else if(type.equals("small")) {
	            this.v = 50.;
	            this.rhoc = 25.;
	            this.rhom = 125.;
	        }
	        this.w = v * (rhoc)/(rhom-rhoc);
	        this.F = v*rhoc;
	    }
	}
