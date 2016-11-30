package org.speedd.dm;

public class Pwa_freeway {
	/*
	 * Class implementing a piecewise-affine model of the Grenoble freeway.
	 * Only intended for testing the event-driven SPEEDD decision making in
	 * closed loop.
	 */
	
	private boolean NON_MONOTONIC = true;
	
	private double[] rho = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] q   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] r   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	private double[] d   = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
	
	private final double[] length = GrenobleTopology.length;
	private final double[] v      = GrenobleTopology.v;
	private final double[] rhoc   = GrenobleTopology.rhoc;
	private final double[] rhom   = GrenobleTopology.rhom;
	private final double[] beta   = GrenobleTopology.beta;
	
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
			
			double inflow;
			
			if (k>0) {
				inflow = Math.min(rates[k-1], q[k-1]/dt + demand[k-1]);
				inflow = Math.min(1800., Math.max(0., inflow)); // hard inflow bounds
			}
			else {
				inflow = 0.;
			}
			
			rho[k] = rho[k] + (dt/this.length[k]) * ( phi[k] - phi[k+1]/(1-beta[k]) + inflow );
			if (k>0) {
				q[k-1] = q[k-1] + dt * (demand[k-1] - inflow);
				r[k-1] = inflow;
				d[k-1] = demand[k-1];
			}
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
		double demand;
		double F = Math.min(rhoc[Math.max(0,k-1)]*v[Math.max(0,k-1)], rhoc[Math.min(18,k)]*v[Math.min(18,k)]);
		
		if (k > 0) {
			demand = this.v[k-1] * this.rho[k-1] * (1-this.beta[k-1]);
			if ((this.rho[k-1] > this.rhoc[k-1]) && this.NON_MONOTONIC) {
				demand = 0.85 * F ; // - ( this.rho[k-1] - this.rhoc[k-1] )*3;
			}
		} else {
			demand = d_ml;
		}
		
		double supply;
		if (k < n) {
			double w = this.v[k] * (rhoc[k])/(rhom[k]-rhoc[k]);
			supply = (this.rhom[k] - this.rho[k]) * w;
			if (k == 0) {
				supply = 100000; // essentially infinity - IMPORTANT to prevent lock-out of demand!!!
			}
		} else {
			supply = 100000; // essentially infinity.
		}
		
		
		return Math.min(F, Math.min(demand,  supply));
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
