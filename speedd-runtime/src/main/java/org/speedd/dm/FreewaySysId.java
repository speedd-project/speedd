package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

public class FreewaySysId {
	
	// objects
	private final GaussianProcess gp_fd;
	private final GaussianProcess gp_noise;
	private final Ctm priorCtm;
	Map<Integer,double[]> data = new HashMap<Integer,double[]>(); 	
	
	private double rhoc = 50.; // initial guess
	
	// constants for FD-GP, precomputed
	private final double fd_sigma_v = 500.;
	private final double fd_sigma_n = 100.;
	private final double[] fd_scales = {500.};
	
	// constants for noise-GP, precomputed
	private final double noise_sigma_v = 30.;
	private final double noise_sigma_n = 30.;
	private final double[] noise_scales = {500.};
	
	/**
	 * Default constructor.
	 * 
	 * @param v		free-flow velocity (km/h)
	 * @param rhoc	critical density (cars/km)
	 * @param rhom	jam density (cars/km)
	 * @param l		cell lenght (km)
	 */
	public FreewaySysId(double v, double rhoc, double rhom, double l, double dF) {
		// create ctm
		this.priorCtm = new Ctm(v, rhoc, rhom, l, dF);
		
		// create Gaussian Processes
		double[] buffer = {0.};
		RealVector[] trainings_data = new RealVector[1]; trainings_data[0] = new ArrayRealVector( buffer );
		RealVector trainings_targets = new ArrayRealVector( buffer );
		this.gp_fd = new GaussianProcess(trainings_data, trainings_targets, this.fd_scales, this.fd_sigma_n, this.fd_sigma_v);
		this.gp_noise = new GaussianProcess(trainings_data, trainings_targets, this.noise_scales, this.noise_sigma_n, this.noise_sigma_v);
		
		// put initial datum
		double[] default_datum = {0., 0., 0.};
		this.data.put(0, default_datum); // add default datum: need at least one datum to estimate GP.
	}

	/**
	 * DEBUG functionality
	 */
	public double[][] getGP() {
		

		this.reestimateGP();
		
		// prepare
		int n = this.data.size();
		double[] densities = new double[n];
		double[] flows = new double[n];
		double[] noise = new double[n];
		
		int ii=0;
		// read out data to training data and training targets
		for (Map.Entry<Integer, double[]> entry : this.data.entrySet())
		{
		    densities[ii] = entry.getValue()[0];
		    flows[ii] = entry.getValue()[1] + this.computePrior(densities[ii]) ;
		    noise[ii] = entry.getValue()[2];
		    ii++;
		}
		
		
		int N = 250; 
		double[] pred_flows = new double[N];
		double[] pred_sigma = new double[N];
		for (int jj=0; jj<N; jj++) {
			pred_flows[jj] = this.predictFlow((double) jj);
			pred_sigma[jj] = this.predictStd((double) jj);
		}
		
		double[][] buffer = new double[5][];
		buffer[0] = densities;
		buffer[1] = flows;
		buffer[2] = noise;
		buffer[3] = pred_flows;
		buffer[4] = pred_sigma;
		return buffer;
	}
	
	/**
	 * Add one new data point (density,flow) to GPs. Does NOT re-estimate.
	 * @param flow		measured flow
	 * @param density	measured density
	 */
	public void addDatum(double flow, double density) {

		double postFlow = flow - computePrior(density); // posterior-flow (GP perspective)
		double[] buffer = {density}; // create test point
		RealVector xt = new ArrayRealVector(buffer);
		double[] predictions = gp_fd.predict(xt);
		// double noise = (postFlow - predictions[0]) * (postFlow - predictions[0]); // local noise estimate
		double noise = Math.abs(postFlow - predictions[0]) ; // local noise estimate
		
		// save data point
		Integer key = (int) density; // rounding
		double[] value = {density, postFlow, noise};
		data.put(key, value);
	}
	
	/**
	 * Delete the stored data and re-estimate the GP.
	 */
	public void clearData() {
		this.data.clear(); // delete all data
		
		double[] default_datum = {0., 0., 0.};
		this.data.put(0, default_datum); // add default datum: need at least one datum to estimate GP.
		
		this.reestimateGP(); // reestimate GP. Since there is only the default datum, the result is the prior.
	}
	
	/**
	 * Re-estimate the GP models.
	 */
	public double reestimateGP() {
		// prepare
		int n = this.data.size();
		RealVector[] training_data = new RealVector[n];
		double[] flows = new double[n];
		double[] noise = new double[n];
		int ii=0;
		
		// read out data to training data and training targets
		for (Map.Entry<Integer, double[]> entry : this.data.entrySet())
		{
		    double[] density = {entry.getValue()[0]};
		    training_data[ii] = new ArrayRealVector( density );
		    flows[ii] = entry.getValue()[1];
		    noise[ii] = entry.getValue()[2];
		    ii++;
		}
		RealVector fd_training_targets = new ArrayRealVector( flows );
		RealVector noise_training_targets = new ArrayRealVector( noise );
		
		// updata GPs
		this.gp_fd.newTrainingData(training_data, fd_training_targets);
		this.gp_noise.newTrainingData(training_data, noise_training_targets);
		
		// reset critical density
		this.rhoc = find_rhoc();
		return this.rhoc;
	}
	
	/**
	 * Getter for critical density.
	 * @return
	 */
	public double get_rhoc() {
		return this.rhoc;
	}
	
	/**
	 * Predict flow according to fundamental diagram.
	 * 
	 * @param density
	 * @return
	 */
	public double predictFlow(double density) {		
		// create test point
		double[] buffer = {density};
		RealVector xt = new ArrayRealVector(buffer);
		
		double result[] = this.gp_fd.predict(xt);
		return computePrior(density) + result[0];
	}
	
	/**
	 * Predict flow according to fundamental diagram.
	 * 
	 * @param density
	 * @return
	 */
	public double predictStd(double density) {		
		// create test point
		double[] buffer = {density};
		RealVector xt = new ArrayRealVector(buffer);
		
		double result[] = this.gp_noise.predict(xt);
		return result[0];
	}
	

	/**
	 * Estimate critical density with limited accuracy.
	 * @return
	 */
	private double find_rhoc() {
		// one-dimensional problem, accuracy is not critical, therefore, gridding is preferred to gradient methods
		double best_flow = 0.;
		double best_dens = 0.;
		for (int ii=45; ii<60; ii++) { // warm-start for AIMSUN
			double ii_dens = (double) ii;
			double ii_flow = predictFlow( ii_dens );
			if ( ii_flow > best_flow) {
				best_flow = ii_flow;
				best_dens = ii_dens;
			}
		}
		return best_dens;
	}
	
	/**
	 * Compute prior flow estimate using the piecewise-affine FD.
	 * 
	 * @param density	Density for which PWA-FD is to be evaluated.
	 * @return			Estimate of flow at density
	 */
	private double computePrior(double density) {
		double demand = this.priorCtm.v * density;
		double supply = (this.priorCtm.rhom - density) * this.priorCtm.w;
		return Math.min(demand, Math.min(4000, supply));
	}
}



