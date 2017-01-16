package org.speedd.dm;

public class FreewayStateEstimator {

	// estimate mean
	private double mu_flow_in;
	private double mu_flow_ramp;
	private double mu_flow_out;
	private double mu_density = 0; // assume empty freeway initially
	
	// estimate variance
	private double v2_flow_in;
	private double v2_flow_ramp;
	private double v2_flow_out;
	private double v2_density = 50; // uncertainty in the order of the critical density
	
	// invariant
	private final double max_dens;
	private final double length;
	private final double dt;
	
	private final double GAMMA_5 = 0.5;
	
	// internal buffer to achieve synchronous measurement processing
	private boolean new_in_measurement = false; // no measurement stored initially
	private boolean new_ramp_measurement = false; // no measurement stored initially

	/*
	 *  	       +-------------------+
	 * 			   |                   |
	 *   ----> (SensorIN)   Cell   (SensorOUT)  ---->
	 * 			   |                   |
	 *  	       +-------------------+
	 */
	
	/**
	 * Default constructor.
	 * 
	 * @param length	length of the cell (km). "-1" to indicate missing data is allowed.
	 * @param max_dens 	maximal density
	 * @param dt		time interval between measurements (h)
	 */
	public FreewayStateEstimator(double length, double max_dens, double dt) {
		this.length = length;
		this.max_dens = max_dens;
		this.dt = dt;
	}
	
	/**
	 * Setter for density estimate.	
	 * @param density
	 */
	public void setDensity(double density) {
		this.mu_density = density;
	}
	
	/**
	 * Getter for outflow estimate.
	 * @return	estimated outflow
	 */
	public double getFlow() {
		return mu_flow_out;
	}
	
	/**
	 * Getter for density estimate.
	 * @return	estimated density
	 */
	public double getDensity() {
		return mu_density;
	}
	
	/**
	 * Getter for inflow estimate.
	 * @return	estimated inflow
	 */
	public double getInflow() {
		return mu_flow_in;
	}
	
	/**
	 * Process a measurement of the flow INTO the cell at "SensorIN"
	 * 
	 * @param mean_flow		empirical mean of the inflow (cars/h)
	 * @param stdv_flow		empirical standard deviation of the inflow (cars/h)
	 * @param mean_dens		empirical mean of the density (cars/km)
	 * @param stdv_dens		empirical standard deviation of the density (cars/km)
	 * @param velocity		empirical mean of the velocity (km/h)
	 */
	public void processInMeasurement(double mean_flow, double stdv_flow, double mean_dens, double stdv_dens, double velocity) {
		// update flows, no dynamics
		this.mu_flow_in = Math.max(0., mean_flow);
		this.v2_flow_in = stdv_flow * stdv_flow;
		new_in_measurement = true;
	}
	
	/**
	 * Process a measurement of the ramp flow INTO the cell at "SensorRamp"
	 * 
	 * @param mean_flow		empirical mean of the inflow (cars/h)
	 * @param stdv_flow		empirical standard deviation of the inflow (cars/h)
	 * @param mean_dens		empirical mean of the density (cars/km)
	 * @param stdv_dens		empirical standard deviation of the density (cars/km)
	 * @param velocity		empirical mean of the velocity (km/h)
	 */
	public void processRampMeasurement(double mean_flow, double stdv_flow, double mean_dens, double stdv_dens, double velocity) {
		// update flows, no dynamics
		this.mu_flow_ramp = Math.max(0., mean_flow);
		this.v2_flow_ramp = stdv_flow * stdv_flow;
		new_ramp_measurement = true;
	}
	
	/**
	 * Process a measurement of the flow OUT of the cell at "SensorOUT"
	 * 
	 * @param mean_flow		empirical mean of the outflow (cars/h)
	 * @param stdv_flow		empirical standard deviation of the outflow (cars/h)
	 * @param mean_dens		empirical mean of the density (cars/km)
	 * @param stdv_dens		empirical standard deviation of the density (cars/km)
	 * @param velocity		empirical mean of the velocity (km/h)
	 */
	public void processOutMeasurement(double mean_flow, double stdv_flow, double mean_dens, double stdv_dens, double velocity) {
		
		// update flows, no dynamics
		this.mu_flow_out = Math.max(0., mean_flow);
		this.v2_flow_out = stdv_flow * stdv_flow;
		
		// compute density estimate
		if ( this.new_in_measurement && this.new_ramp_measurement &&  (stdv_dens < 100) ) {
			// Kalman filter
			// (1) prediction step
			double mu_density_pred = this.mu_density + (dt/this.length) * ( (this.mu_flow_in+this.mu_flow_ramp) - this.mu_flow_out);
			double v2_density_pred = this.v2_density + (dt*dt)/(this.length*this.length) * (this.v2_flow_in + this.v2_flow_ramp + this.v2_flow_out);
			
			// (2) estimate covariance
			double S = v2_density_pred + stdv_dens*stdv_dens;
			double K = v2_density_pred / ((1e-6) + S);
			
			// (3) correction step
			this.mu_density = Math.max(0., Math.min( this.max_dens, mu_density_pred + K * (mean_dens - mu_density_pred) ));
			this.v2_density = v2_density_pred - K*S*K;
		} else {
			// insufficient measurements to use Kalman filter
			this.mu_density = Math.max(0., Math.min( this.max_dens, this.GAMMA_5 * mean_dens + (1-this.GAMMA_5) * this.mu_density ));
			this.v2_density = stdv_dens*stdv_dens;
		}
		
		// Last inflow measurement has been used
		new_in_measurement = false;
		new_ramp_measurement = false;
	}

}
