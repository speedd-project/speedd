package org.speedd.dm;

public class Ctm {
		// local parameters
	    public Double v;
	    public Double w;
	    public Double rhoc;
	    public Double rhom;
	    public Double l;
	    public double F;
	    public double dF;
	    
	    /**
	     * Default constructor with the minimal amount of information, missing
	     * data are automatically computed
	     * 
	     * @param v		free-flow velocity (km/h)
	     * @param rhoc	critical density (cars/km)
	     * @param rhom	jam density (cars/km)
	     * @param l		cell length (km)
	     */
	    public Ctm(double v, double rhoc, double rhom, double l, double dF) {
	        this.v = v;
	        this.w = v * (rhoc)/(rhom-rhoc);
	        this.rhoc = rhoc;
	        this.rhom = rhom;
	        this.l = l;
	        this.F = v*rhoc;
	        this.dF = dF;
	    }
	    
	}
