package org.speedd.dm;

public class FreewayCoordination {
	
	// references to other DM sub-components
	private final FreewayMeteredOnramp main_class;
	private final FreewayStateEstimator mainline;
	private final FreewayStateEstimator queue;
	private final FreewayController controller;
	private final FreewaySysId sysId;
	
	// internal coordination state
	private boolean queue_control = false;
	private boolean request_coordination = false;
	private boolean density_control = false;
	private int last_coordination_event = 0;
	// auxiliary variables
	private double rho_last = 0.;
	private double delta_rho = 0.;
	
	// coordination constants
	private final double GAMMA_1 = 0.7; // % of critical density to turn ON ramp metering
	private final double GAMMA_2 = 0.5; // % of critical density to turn OFF ramp metering 
	private final double GAMMA_3 = 0.5; // % of queue occupancy to turn OFF queue coordination 
	private final double GAMMA_4 = 0.7; // % of queue occupancy to turn ON queue coordination
	
	/**
	 * Default constructor.
	 * @param mainline		State Observer of the mainline.
	 * @param queue			State Observer of the queue.
	 * @param controller	Low-level ramp metering controller.
	 * @param sysId			System Identification of the mainline.
	 */
	public FreewayCoordination(FreewayStateEstimator mainline, FreewayStateEstimator queue, FreewayController controller, FreewaySysId sysId, FreewayMeteredOnramp main_class) {
		// set references
		this.mainline = mainline;
		this.queue = queue;
		this.controller = controller;
		this.sysId = sysId;
		this.main_class = main_class;
	}
	
	// j7Gw94hfy
	
	// (0) Getter functions for (discrete) state
	
	/**
	 * Check if queue control is active.
	 * @return boolean
	 */
	public boolean get_queue_control() {
		return this.queue_control;
	}
	
	/**
	 * Check if density control is active.
	 * @return boolean
	 */
	public boolean get_density_control() {
		return this.density_control;
	}
	
	/**
	 * Check if coordination is being requested.
	 * @return boolean
	 */
	public boolean get_request_coordination() {
		return this.request_coordination;
	}
	
	
	
	// (1) State transitions based on periodic checks
	
	/**
	 * Periodical update (before every execution of the low-level control
	 * function) of the state of coordination.
	 * 
	 * @return	coordination value: either current onramp density or toggle-signal (0,-1)
	 */
	public double evaluateCoordination() {
		double queue_occupancy = queue.getDensity();
		double mainline_density = main_class.get_merge_density();
		
		// update 
		this.delta_rho = mainline_density - rho_last;
		this.rho_last = mainline_density;
		
		// check if coordination request is out-dated
		if (this.queue_control) {
			if (this.last_coordination_event > 2) {
				this.queue_control = false;
				this.last_coordination_event = 0;
			} else {
				this.last_coordination_event++;
			}
		}
		
		// check conditions for ramp metering
		if (this.density_control) {
			// ramp metering is active
			if ( mainline_density <= GAMMA_2 * this.sysId.get_rhoc() ) {
				this.density_control = false;
			} // else don't do anything
		} else {
			// ramp metering is not active
			if ( mainline_density >= GAMMA_1 * this.sysId.get_rhoc() ) {
				this.density_control = true;
			}
		}
		
		if ( (queue_occupancy < GAMMA_3 * 125.) ) { // release coordination
			if ( this.request_coordination ) {
				this.request_coordination = false;
				return 0.;
			} else {
				return -1.;
			}
		}
		if ( (queue_occupancy >= GAMMA_4 * 125.) ) { // request coordination
			this.request_coordination = true;
			// return Math.max(queue_occupancy - 125*0.2, 0.);
			return queue_occupancy * 0.8;
		}
		
		if (this.request_coordination) {
			// return Math.max(queue_occupancy - 125*0.2, 0.);
			return queue_occupancy * 0.8;
		} else {
			return -1.;
		} 
		
	}

	
	// (2) State transitions based on detected/ predicted events:
	
	/**
	 * Process Congestion by updating the state of the coordination
	 */
	public void congestion() {
		if ( this.density_control ) {
			// ramp metering already turned on, but seems insufficient
			this.request_coordination = true;
		} else {
			// re-estimate fundamental diagram
			this.reestimate_rhoc();
			// turn on ramp metering 
			this.density_control = true;
		}
	}
	
	/**
	 * Process Cleared Congestion by updating the state of coordination
	 */
	public void clearCongestion() {
		if ( this.density_control && !this.queue_control ) {
			// turn off ramp metering, if no coordination is requested
			this.density_control = false;
		}
	}
	
	/**
	 * Process Predicted Congestion by updating the state of the coordination.
	 * @param p_con 	probability of congestion, as reported by event
	 */
	public void predictedCongestion(double p_con) {
		if (density_control) {
			// ramp metering already active, check if coordination is necessary
			if ( ttt_trade_off(p_con) ) {
				// turn on ds coordination
				this.request_coordination = true;
			}
		} else {
			// turn on ramp metering
			this.density_control = true;
		}
	}
	
	/**
	 * Process Predicted Ramp Overflow by updating the state of the coordination
	 */
	public void predictedRampOverflow(double p_rampoverflow) {
		// update ramp queue length estimate - maybe the Kalman filter missed
		// something?
		this.queue.setDensity( Math.max( 120 * p_rampoverflow, this.queue.getDensity()) );
		// also react as for predicted congestion, since ramp overflow will
		// activate automatic override and cause a congestion as well
		predictedCongestion( p_rampoverflow );
	}

	/**
	 * Process Ramp Coordination by updating the state of the coordination.
	 * @param target_queue_density
	 */
	public void rampCoordination(double target_queue_density) {
		this.last_coordination_event = 0;
		// check if valid metering rate
		if (target_queue_density > 0) {
			controller.set_target_density_queue( target_queue_density );
			this.queue_control = true;
		} else {
			controller.set_target_density_queue( -1000. ); // turn off
			this.queue_control = false;
		}

	}
	
	
	// (3) auxiliary functions
	
	/**
	 * Performs the TTT tradeoff in case of predicted congestion/ ramp overflow
	 * @param p_con		probability of congestion
	 * @return			true if coordination is recommended
	 */
	private boolean ttt_trade_off(double p_con) {
		// compute bounds on additional waiting time
		
		double q_ds = this.main_class.q_length;
		double q_us = GrenobleTopology.getQueue( GrenobleTopology.get_upstream_ramp( this.main_class.k ) );
		if (q_us == -1) {
			return false; // no coordination possible
		}
		
		double rho_t_ds = this.mainline.getDensity();
		double rhoc_ds = this.sysId.get_rhoc();
		double l_ds = this.main_class.ctm_data.l;
		double q_t_ds = this.queue.getDensity();
		double l_q_ds = this.main_class.q_length;
		
		double T = TrafficDecisionMakerBolt.dt * (rhoc_ds - rho_t_ds) / this.delta_rho;
		
		double capacity_drop = GrenobleTopology.dF[1] * this.sysId.predictFlow(rhoc_ds);
		double duration = 1.5; // FIXME: congestion duration, replace with data-driven estimates, in hours
		
		double delta_T_ml = p_con * q_us * T * capacity_drop * duration / (l_ds*(rhoc_ds-rho_t_ds) + l_q_ds*(125-q_t_ds));
		double delta_T_ramp =  T * (q_ds * q_us)/(q_ds + q_us) ;

		return ( (p_con * delta_T_ml) > ((1-p_con) * delta_T_ramp) ); // trade-off
	}
	
	/**
	 * Re-estimation of critical density.
	 */
	private void reestimate_rhoc() {
		this.sysId.reestimateGP(); // reestimate fundamental diagram
		controller.set_target_density_mainline( this.sysId.get_rhoc() ); // read rhoc
	}
	
}
