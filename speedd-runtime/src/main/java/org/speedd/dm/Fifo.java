package org.speedd.dm;

import java.security.InvalidParameterException;

public class Fifo {
	// class containing intersection parameters
	    public double[] priorities;
	    public double[][][] TLP; // short for: "Traffic Light Phases". Indices: [phase number][row-outgoing road index][column-incoming road index]
	    public double[][] TPM; // short for: "Turning Preference Matrix" Indices: [row-outgoing road index][column-incoming road index]
	    
	    // default constructor: all parameters are set by the user
	    public Fifo(double[] priorities, double[][][] TLP, double[][] TPM) {
	        this.priorities = priorities;
	        this.TLP = TLP;
	        this.TPM = TPM;
	    }
	    

		/**
		 * 
		 * @param turnLeft	[0,1] percentage of cars turning left
		 * @param turnRight [0,1] percentage of cars turning right
		 * @return
		 */
		public static Fifo make4wayParameters(double turnLeft, double turnRight) {
			double[] priorities = {1.,1.,1.,1.};	// equal priority
			double l = turnLeft;
			double[][][] TLP = { {{0.,0.,1-l,0.},{0.,0.,l,0.},{1-l,0.,0.,0.},{l,0.,0.,0.}},		// phase 1
								 {{0.,l,0.,0.},{0.,0.,0.,1-l},{0.,0.,0.,l},{0.,1-l,0.,0.}} };	// phase 2
			double[][] TPM = {{0.,l,1-l,0.},{0.,0.,l,1-l},{1-l,0.,0.,l},{l,1-l,0.,0.}};	
			return new Fifo(priorities, TLP, TPM);
		}
		
		/**
		 * Create the fifo parameters for a metered onramp.
		 * @return fifo parameters
		 */
		public static Fifo makeMeteredOnramp() {
			double[] priorities = {0.5, 0.5}; 			// equal priority
	        double[][][] TLP = {{{1.,1.}},{{1.,0.}}}; 	// {[1 1],[1 0]} green light, red light
	        double[][] TPM = {{1.,1.}}; 				// [1 1] all cars to the mainline
	        return new Fifo(priorities, TLP, TPM);
		}
		
		/**
		 * Create the fifo parameters for an unmetered onramp.
		 * @return fifo parameters
		 */
		public static Fifo makeOnramp() {
			double[] priorities = {0.5, 0.5}; 	// equal priority
	        double[][][] TLP = {{{1.,1.}}}; 	// {[1 1]} no metering
	        double[][] TPM = {{1.,1.}}; 		// [1 1] all cars to the mainline
	        return new Fifo(priorities, TLP, TPM);
		}
	    
		/**
		 * Create fifo parameters for offramp
		 * @param beta	split ration: percentage of cars leaving the freeway
		 * @return fifo parameters
		 */
		public static Fifo makeOfframp(double beta) {
			if ((beta <= 0) || (beta >= 1)) {
				throw(new InvalidParameterException());
			}
	        double[] priorities = {1.};
	        double[][][] TLP = {{{1-beta},{beta}}}; 	// {[1;1]}
	        double[][] TPM = {{1-beta},{beta}}; 	// [1;1] encode split ratio
	        return new Fifo(priorities, TLP, TPM);
		}
		
		/**
		 * Create fifo parameters splitting road into two cells
		 * @return fifo parameters
		 */
		public static Fifo makeSplit() {
			double[] priorities = {1.};
			double[][][] TLP = {{{1.}}};
			double[][] TPM = {{1.}};
			return new Fifo(priorities, TLP, TPM);
		}
		
	}