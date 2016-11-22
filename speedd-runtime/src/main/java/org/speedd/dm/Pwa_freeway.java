package org.speedd.dm;

public class Pwa_freeway {
	/*
	 * Class implementing a piecewise-affine model of the Grenoble freeway.
	 * Only intended for testing the event-driven SPEEDD decision making in
	 * closed loop.
	 */
	
	private double[] rho = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] q = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] r = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] d = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	
	// private final double[] ql   = {  0.1,  0.4,  -1.,  -1., 0.12,  -1., 0.15, 0.12,  -1.,  -1.,  0.3,        -1.,  0.2,  -1.,  0.2,        -1.,  0.2,  -1., 0.06      };
	private final double[] length={  0.5,  0.4,  0.8,  0.4,  0.4,  0.8,  0.5, 0.45, 0.75,  1.3,  0.5,        0.9,  0.5, 0.65, 0.55,       0.65, 0.55, 0.45,  0.4      };
	private final double[] v    = {  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,        90.,  90.,  90.,  90.,        90.,  90.,  90.,  90.      };
	private final double[] rhoc = {  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,        50.,  50.,  50.,  50.,        50.,  50.,  50.,  50.      };
	private final double[] rhom = { 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250.,       250., 250., 250., 250.,       250., 250., 250., 250.      };
	private final double[] beta = {   0.,   0.,   0.,  0.2,   0.,  0.2,   0.,   0.,   0.,  0.2,   0.,        0.2,   0.,  0.2,   0.,        0.2,   0.,  0.2,   0.      };
	
	private final int n = 19;

	/**
	 * 
	 * @param demand
	 * @param rates
	 * @param d_ml
	 * @return
	 */
	public double[] sim_step(double[] demand, double[] rates, double d_ml, double dt) {
		
		// compute flows
		double[] phi = new double[n+1];
		for (int k=0; k<=n; k++) {
			phi[k] = compute_flow(k, d_ml);
		}
		
		// update densities & queues
		for (int k=0; k<n; k++) {
			
			double inflow = Math.min(rates[k], q[k]/dt + demand[k]);
			inflow = Math.min(1800., Math.max(0., inflow)); // hard inflow bounds
			
			rho[k] = rho[k] + (dt/this.length[k]) * ( phi[k] - phi[k+1]/(1-beta[k]) + inflow );
			q[k] = q[k] + dt * (demand[k] - inflow);
			r[k] = inflow;
			d[k] = demand[k];
		}
		
		return phi;
	}

	/**
	 * 
	 * @return
	 */
	public double[] get_queues() {
		double[] buffer = new double[19];
		System.arraycopy(this.q, 0, buffer, 0, this.q.length);
		return buffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[] get_densities() {
		double[] buffer = new double[19];
		System.arraycopy(this.rho, 0, buffer, 0, this.rho.length);
		return buffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[] get_inflow() {
		double[] buffer = new double[19];
		System.arraycopy(this.r, 0, buffer, 0, this.r.length);
		return buffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[] get_demand() {
		double[] buffer = new double[19];
		System.arraycopy(this.d, 0, buffer, 0, this.d.length);
		return buffer;
	}
	
	/**
	 * Compute flow ( k ) --> ( k+1 )
	 * @param k
	 * @return
	 */
	private double compute_flow(int k, double d_ml) {
		double demand;
		
		if (k > 0) {
			demand = this.v[k-1] * this.rho[k-1] * (1-this.beta[k-1]);
		} else {
			demand = d_ml;
		}
		
		double supply;
		if (k < n) {
			double w = this.v[k] * (rhoc[k])/(rhom[k]-rhoc[k]);
			supply = (this.rhom[k] - this.rho[k]) * w;
		} else {
			supply = 100000; // essentially infinity.
		}
		return Math.min(demand,  supply);
	}
}
