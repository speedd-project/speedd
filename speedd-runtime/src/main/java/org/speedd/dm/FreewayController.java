package org.speedd.dm;

public class FreewayController {
	
	// reference to state estimators
	private final FreewayStateEstimator mainline;
	private final FreewayStateEstimator queue;
	
	// Ramp metering constants
	private final double MAX_RATE = 1800; // ... cars/h, i.e. one car every two seconds
	private final double MAX_QUEUE_DENSITY = 110.; // Purposefully chosen slightly lower than the standard 1car/(0.008km)
	
	private double K1 = 70; // 70: default value for ALINEA
	private double K2 = 0.; // 0: default value for ALINEA
	private double K_overflow = 0.33; // DON'T tune. 0 < K_overflow < 1, but close to 1 may lead to instabilities
	
	// internal state
	private double last_mainline_density = 0.;

	// control targets
	private double target_density_mainline;
	private double target_density_queue = this.MAX_QUEUE_DENSITY;
	private final double q_length;
	
	// ui-overwrite
	private double ui_min_rate = TrafficDecisionMakerBolt.RMIN;
	private double ui_max_rate = 1800.;
	
	// sampling time interval
	private double dt; // can be used for controller tuning
	
	/**
	 * Default constructor.
	 * 
	 * @param mainline					reference to state estimator for the mainline
	 * @param queue						reference to state estimator for the queue
	 * @param target_density_mainline	initial target density, usually the critical density
	 * @param dt						sampling time
	 */
	public FreewayController(FreewayStateEstimator mainline, FreewayStateEstimator queue, double target_density_mainline, double q_length, double dt) {
		this.mainline = mainline;
		this.queue = queue;
		
		this.target_density_mainline = target_density_mainline;
		this.q_length = q_length;
		this.dt = dt;
	}
	
	/**
	 * Setter for target_density_mainline
	 * @param target_density_mainline - in veh/km
	 */
	public void set_target_density_mainline(double target_density_mainline) {
		this.target_density_mainline = target_density_mainline;
	}
	
	/**
	 * Setter for target density queue
	 * @param target_density_queue - in veh/km (normalization w.r.t. queue lenght is helpful for coordination)
	 */
	public void set_target_density_queue(double target_density_queue) {
		if (target_density_queue > 0) {
			this.target_density_queue = Math.min( this.MAX_QUEUE_DENSITY, target_density_queue );
		} else {
			this.target_density_queue = this.MAX_QUEUE_DENSITY;
		}
	}
	
	/**
	 * Set upper metering rate limit.
	 * @param r_max
	 */
	public void set_upper_bound(double r_max) {
		if (r_max < 0) {
			this.ui_max_rate = 1800.;
		} else {
			this.ui_max_rate = Math.min(1800., r_max); // absolute limit
			this.ui_min_rate = Math.min(this.ui_min_rate, this.ui_max_rate); // min rate always lower than max rate
		}
	}
	
	/**
	 * Set lower metering rate limit.
	 * @param r_max
	 */
	public void set_lower_bound(double r_min) {
		if (r_min < 0) {
			this.ui_min_rate = TrafficDecisionMakerBolt.RMIN;
		} else {
			this.ui_min_rate = Math.max(TrafficDecisionMakerBolt.RMIN, r_min); // absolute limit
			this.ui_max_rate = Math.max(this.ui_max_rate, this.ui_min_rate); // max rate always higher than min rate
		}
	}
	
	/**
	 * Computes the metering rate based on most recent state estimate, externally defined control targets and internal state.
	 * @return	metering rate in cars/h
	 */
	public double computeMeteringRate() {
		
		// (1) get variables
		// (1a) mainline
		double mainline_density = this.mainline.getDensity();
		double mainline_flow = this.mainline.getFlow();
		// (1b) onramp
		double queue_density = this.queue.getDensity();
		double demand_estimate = this.queue.getInflow();
		double inflow_estimate = this.queue.getFlow();
		// (1c) estimation of downstream density in the merge-area
		double merge_density = mainline_density * ((mainline_flow + inflow_estimate) / (mainline_flow + (1e-6)));
		
		// (2) compute rate
		// (2a) compute mainline-tracking metering rate
		double meteringRate = alinea(merge_density, this.target_density_mainline, this.last_mainline_density, inflow_estimate);

		// (2b) compute onramp-tracking metering rate if applicable
		double queue_control = demand_estimate - this.K_overflow * (this.q_length / this.dt) * ( this.target_density_queue - queue_density );
		meteringRate = Math.min(meteringRate, queue_control);
		
		// (2c) ... considering threat of queue overflow - safety mechanism even if there's no event warning DM.
		double queue_overflow = demand_estimate - this.K_overflow * (this.q_length / this.dt) * ( this.MAX_QUEUE_DENSITY - queue_density );
		meteringRate = Math.max(meteringRate, queue_overflow); // ... release as many cars as there are entering
		
		// (3) saveback
		this.last_mainline_density = merge_density;
		// absolute metering rate limits
		meteringRate = Math.max(this.ui_min_rate, Math.min(meteringRate, this.ui_max_rate)); 
		return Math.max(0., Math.min(meteringRate, this.MAX_RATE)); // in cars/h
	}
	
	/**
	 * PI-Alinea equation
	 * @param value
	 * @param target
	 * @return
	 */
	private double alinea(double value, double target, double lastValue, double lastAction) {
		return lastAction + K1 * (target - value) - K2 * (value - lastValue);
	}

}
