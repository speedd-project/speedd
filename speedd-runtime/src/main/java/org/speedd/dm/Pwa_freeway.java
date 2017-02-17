package org.speedd.dm;

public class Pwa_freeway {
	/*
	 * Class implementing a piecewise-affine model of the Grenoble freeway.
	 * Only intended for testing the event-driven SPEEDD decision making in
	 * closed loop.
	 */
	
	private boolean NON_MONOTONIC = true;
	
	private double CRIT_RANGE = 20.;
	
	private double[] rho = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] q   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] r   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] d   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	
	private final double[] length = GrenobleTopology.length;
	private final double[] v      = GrenobleTopology.v;
	private final double[] rhoc   = GrenobleTopology.rhoc;
	private final double[] rhom   = GrenobleTopology.rhom;
	private final double[] beta   = GrenobleTopology.beta;
	private final double[] dF     = GrenobleTopology.dF;
	
	private final int n = 21;

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
			q[k] = q[k] + dt * (demand[k] - inflow);
			r[k] = inflow;
			d[k] = demand[k];
			
			rho[k] = rho[k] + (dt/this.length[k]) * ( phi[k] - phi[k+1]/(1-beta[k]) + inflow );
		}
		return phi;
	}

	/**
	 * 
	 * @return
	 */
	public double[] get_queues() {
		double[] buffer = new double[this.q.length];
		System.arraycopy(this.q, 0, buffer, 0, this.q.length);
		return buffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[] get_densities() {
		double[] buffer = new double[this.rho.length];
		System.arraycopy(this.rho, 0, buffer, 0, this.rho.length);
		return buffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[] get_inflow() {
		double[] buffer = new double[this.r.length];
		System.arraycopy(this.r, 0, buffer, 0, this.r.length);
		return buffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[] get_demand() {
		double[] buffer = new double[this.d.length];
		System.arraycopy(this.d, 0, buffer, 0, this.d.length);
		return buffer;
	}
	
	/**
	 * Compute flow ( k ) --> ( k+1 )
	 * @param k
	 * @return
	 */
	private double compute_flow(int k, double d_ml) {
		
		double scale_us = 1.;
		// if (this.NON_MONOTONIC && (k > 0)) {
		if ( (k > 0) ) {
			scale_us = 1 + dF[k-1];
		}
		double scale_ds = 1.;
		// if (this.NON_MONOTONIC && (k < dF.length)) {
		if ( (k < dF.length)) {
			scale_ds = 1 + dF[k];
		}
		
		double F_us = scale_us * rhoc[Math.max(0,k-1)] * v[Math.max(0,k-1)];
		double F_ds = scale_ds * rhoc[ Math.min(rhoc.length-1,k) ] * v[Math.min(v.length-1,k)];
		if ( k == 18 ) {
			F_ds = scale_ds * 4450.; // NOTE: Modification to replicate real-world traffic patterns.
		} else if (k == 19 ) {
			F_us = scale_us * 4450.; // ... 
		}

		double demand; // compute traffic DEMAND
		if (k > 0) {
			demand = scale_us * this.v[k-1] * this.rho[k-1] * (1-this.beta[k-1]);
			demand = Math.min(F_us, demand);
			if (this.NON_MONOTONIC && ( this.rho[k-1] > this.rhoc[k-1] + CRIT_RANGE )) {
				demand = F_us / scale_us ;
			}
		} else {
			demand = d_ml;
		}
		
		double supply; // compute SUPPLY of free space
		if ((k > 0) && (k < n)) {
			double w = this.v[k] * rhoc[k] / (rhom[k] - (rhoc[k] + CRIT_RANGE));
			supply = scale_ds * (this.rhom[k] - this.rho[k]) * w;
			supply = Math.min(F_ds, supply);
		} else {
			supply = 100000; // essentially infinity - IMPORTANT to prevent lock-out of demand!!!
		}
		
		return Math.min(demand, supply);
	}

	public double compute_TTT() {
		double TTT = 0.;
		for (int ii=0; ii<this.rho.length; ii++) {
			TTT += this.rho[ii]*this.length[ii];
			TTT += this.q[ii];
		}
		return TTT;
	}

}
