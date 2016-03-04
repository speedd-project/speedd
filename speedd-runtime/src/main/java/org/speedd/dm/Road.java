package org.speedd.dm;

public abstract class Road {
	// class containing topology information of one road
	    public int intersection_begin; // FIXME: Make these final.
	    public int intersection_end;
	    public int sensor_begin;
	    public int sensor_end;
	    public String type; // should be changed to a custom type
	    public Ctm params;
	    public double ncars = 0;
	    
	    // get demand
	    public abstract double getDemand();
	    // get supply
	    public abstract double getSupply();
	    // update density
	    public double updateDensity(double delta_n) {
	    	this.ncars = this.ncars + delta_n;
	    	this.ncars = Math.max(this.ncars, 0); // sanity check: no negative cars
	    	this.ncars = Math.min(this.ncars, this.params.l*this.params.rhom); // sanity check: jam density
	    	if (Double.isNaN(this.ncars)) {
	    		throw new ArithmeticException("Density estimate is NaN.");
	    	}
	    	return this.ncars;
	    }
	    // set density
	    public void setDensity(double ncars_new) {
	    	this.ncars = Math.max(ncars_new, 0); // sanity check: no negative cars
	    	this.ncars = Math.min(this.ncars, this.params.l*this.params.rhom); // sanity check: jam density
	    	if (Double.isNaN(this.ncars)) {
	    		throw new ArithmeticException("Density estimate is NaN.");
	    	}
	    }
	}

