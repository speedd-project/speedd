package org.speedd.dm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

public class FreewayMeteredOnramp extends FreewayCell {
	
	private final FreewayController controller;
	private final FreewayCoordination coordination;

	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	
	private int counter = 0;
	private final int reestimation_interval = 30; // should be in minutes
	
	private boolean just_active = false;
	
	private PrintWriter out;

	/**
	 * Default constructor for a metered onramp.
	 * 
	 * @param id_table
	 * @param ctm_data
	 * @param q_length
	 * @param dt
	 */
	public FreewayMeteredOnramp(FreewayCell_IdTable id_table, Ctm ctm_data, double q_length, double dt) {
		// set up attributes of superclass
		super(id_table, ctm_data, q_length, dt);
		
		// set up ramp metering
		this.controller = new FreewayController(this.mainline_stateEstimator, this.onramp_stateEstimator, this.ctm_data.rhoc, q_length, dt);
		this.coordination = new FreewayCoordination(this.mainline_stateEstimator, this.onramp_stateEstimator, this.controller, this.mainline_sysId, this);
		
		// Write to debug log.
		if ( TrafficDecisionMakerBolt.DEBUG ) {
			try {
				File logfile = new File(Integer.toString( this.id_table.actu_id ) + "log.txt");
				logfile.delete(  );
			    this.out = new PrintWriter(new BufferedWriter(new FileWriter(Integer.toString( this.id_table.actu_id ) + "log.txt", true)));
			} catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
		}

	}
	
	/**
	 * Main routine for event-driven freeway ramp-metering decision making.
	 * 
	 * @param event
	 * @return
	 */
	public Event[] processEvent(Event event) {

		String eventName = event.getEventName();
		Map<String, Object> attributes = event.getAttributes();
		long timestamp  = event.getTimestamp();
		
		/* ====================================================================
		 * (0) Debug events
		 */
		if ( eventName.equals("End") ) {
			if (TrafficDecisionMakerBolt.DEBUG) {
				out.close(); // write to debug log
				System.out.println( "Critical density of cell " + Integer.toString(this.k) + " : " + Double.toString(this.mainline_sysId.get_rhoc()) );
			}
		}
		
		/* ====================================================================
		 * (1) State estimation and System Identification
		 */
        if (eventName.equals(TrafficDecisionMakerBolt.MAINLINE_MEASUREMENT) || eventName.equals(TrafficDecisionMakerBolt.ONRAMP_MEASUREMENT) || eventName.equals(TrafficDecisionMakerBolt.OFFRAMP_MEASUREMENT)) {
        	// call superclass function
        	this.processMeasurement(event);
        }
        
        /* ====================================================================
         * (2) Coordination via Complex Events
         */ 
		if (eventName.equals(TrafficDecisionMakerBolt.METERING_LIMITS)) {
			Double minFlow = (Double)attributes.get("lowerLimit");
			if (minFlow != null) {
				this.controller.set_lower_bound( minFlow );
			}
			Double maxFlow = (Double)attributes.get("upperLimit");
			if (maxFlow != null) {
				this.controller.set_upper_bound( maxFlow );
			}
		} else if (eventName.equals(TrafficDecisionMakerBolt.INCIDENT)) {
			this.mainline_sysId.clearData(); // old data are no longer informative
		} else if (eventName.equals(TrafficDecisionMakerBolt.COORDINATE)) {
			if ((attributes.get("target_occupancy") != null)) {
				double target_queue_occupancy = (double) attributes.get("target_occupancy"); // No conversion factor, set by DM.
				this.coordination.rampCoordination(target_queue_occupancy);
			}
		} else if (eventName.equals(TrafficDecisionMakerBolt.CONGESTION)) {
			this.coordination.congestion(); // Process predicted congestion event by adapting coordination
		} else if (eventName.equals(TrafficDecisionMakerBolt.CLEAR_CONGESTION)) {
			this.coordination.clearCongestion(); // Process predicted congestion event by adapting coordination
		} else if (eventName.equals(TrafficDecisionMakerBolt.PREDICTED_CONGESTION)) {
			if ((attributes.get("certainty") != null)) {
				double p_con = (double) attributes.get("certainty");
				this.coordination.predictedCongestion(p_con); // Process predicted congestion event by adapting coordination
			}
		} else if (eventName.equals(TrafficDecisionMakerBolt.PREDICTED_OVERFLOW)) {
			if ((attributes.get("certainty") != null)) {
				double p_con = (double) attributes.get("certainty");
				this.coordination.predictedRampOverflow(p_con); // Process predicted ramp overflow event by adapting coordination
			}
		}
		
        /* ====================================================================
         * (3) Control triggered by most recent onramp-measurement
         */ 
		Event[] out_events = null;
		if (eventName.equals(TrafficDecisionMakerBolt.ONRAMP_MEASUREMENT)) {
        	int sens_id = Integer.parseInt((String) attributes.get("sensorId"));
        	
        	if (sens_id == this.id_table.sens_on) {
        		
        		out_events = new Event[5];
        		
        		// (3a) re-estimate FD if necessary
        		this.counter++;
        		if (this.counter >= this.reestimation_interval) {
        			this.controller.set_target_density_mainline( this.mainline_sysId.reestimateGP() ); // set new critical density	
        		}
        		
        		// Actual crit. densities, only for debugging
    			/* if (this.k == 1) {
        			this.controller.set_target_density_mainline( 59.6 ); // set new critical density
    			} else if (this.k == 6) {
        			this.controller.set_target_density_mainline( 52 ); // set new critical density
    			} else if (this.k == 10) {
        			this.controller.set_target_density_mainline( 48 ); // set new critical density
    			} else if (this.k == 13) {
        			this.controller.set_target_density_mainline( 49.5 ); // set new critical density
    			} else if (this.k == 15) {
        			this.controller.set_target_density_mainline( 51.2 ); // set new critical density
    			} else if (this.k == 18) {
        			this.controller.set_target_density_mainline( 56.1 ); // set new critical density
    			} */
        		
        		// (3b) periodic update of coordination algorithm
        		double target_queue_density = this.coordination.evaluateCoordination();
        		
        		// (3c.1) send coordination event if appropriate
        		if (target_queue_density >= -0.1) {
        			// Create upstream coordination event
			        Map<String, Object> outAttrs = new HashMap<String, Object>();
			        int upstream_ramp_id = GrenobleTopology.get_upstream_ramp( this.id_table.actu_id ); // get upstream id
			        outAttrs.put("junction_id", Integer.toString( upstream_ramp_id ) );
			        outAttrs.put("dmPartition", GrenobleTopology.get_dm_partition(this.k));
			        outAttrs.put("target_occupancy", target_queue_density);
			        out_events[0] = eventFactory.createEvent(TrafficDecisionMakerBolt.COORDINATE, timestamp, outAttrs);
        		}

        		// (3c) send metering rates, coordination information & queue length estimates, if appropriate
        		if ( this.coordination.get_density_control() || this.coordination.get_queue_control()) {
	        		
	        		// (3c.2) decide on metering rate
        			
        			// System.out.println( Double.toString(this.get_merge_density()) ); // ... was a debug check
        			
	        		double rate = this.controller.computeMeteringRate( this.get_merge_density() );
			        Map<String, Object> outAttrs = new HashMap<String, Object>();
			        outAttrs.put("junction_id", this.id_table.actu_id );
			        outAttrs.put("location", this.lookupLocation( this.id_table.actu_id ) );
			        outAttrs.put("dmPartition", GrenobleTopology.get_dm_partition(this.k));
			        outAttrs.put("phase_id", 2);
			        outAttrs.put("merge_density", this.get_merge_density() );
			        outAttrs.put("target_density", this.controller.get_target_density_mainline() );
			        outAttrs.put("phase_time", (int) (rate * TrafficDecisionMakerBolt.RATE2GREEN_INTERVAL)); // ASSUMPTION: phase 2 is "green"
			        out_events[3] = eventFactory.createEvent(TrafficDecisionMakerBolt.SET_RATES, timestamp, outAttrs);
			        
			        /* outAttrs = new HashMap<String, Object>();
			        outAttrs.put("junction_id", this.id_table.actu_id );
			        outAttrs.put("dmPartition", GrenobleTopology.get_dm_partition(this.k));
			        outAttrs.put("phase_id", 1);
			        outAttrs.put("phase_time", (int) (60 - rate * TrafficDecisionMakerBolt.RATE2GREEN_INTERVAL)); // ASSUMPTION: phase 1 is "red"
			        out_events[2] = eventFactory.createEvent(TrafficDecisionMakerBolt.SET_RATES, timestamp, outAttrs); */
			        
	        		// (3c.3) sent onramp queue length information
			        double queue_density = this.onramp_stateEstimator.getDensity();
			        outAttrs = new HashMap<String, Object>();
			        outAttrs.put("sensorid", Integer.toString(this.id_table.sens_on));
			        outAttrs.put("dmPartition", GrenobleTopology.get_dm_partition(this.k));
			        outAttrs.put("queueLength", queue_density * this.q_length); // in what units?
			        outAttrs.put("maxQueueLength", 125 * this.q_length); // same units as line before
			        out_events[1] = eventFactory.createEvent(TrafficDecisionMakerBolt.QUEUE_LENGTH, timestamp, outAttrs);
			        
			        this.just_active = true; // delayed update, this block is executed once more after ramp metering has been deactivated.
        		} else if (this.just_active) {
        			// ramp metering has just been deactivated
	        		// (3c.2) decide on metering rate
	        		double rate = 1800.; // ramp metering is deactivated --> set rate to maximum.
			        Map<String, Object> outAttrs = new HashMap<String, Object>();
			        outAttrs.put("junction_id", this.id_table.actu_id );
			        outAttrs.put("location", this.lookupLocation( this.id_table.actu_id ) );
			        outAttrs.put("dmPartition", GrenobleTopology.get_dm_partition(this.k));
			        outAttrs.put("phase_id", 2);
			        outAttrs.put("phase_time", (int) (rate * TrafficDecisionMakerBolt.RATE2GREEN_INTERVAL)); // ASSUMPTION: phase 2 is "green"
			        out_events[2] = eventFactory.createEvent(TrafficDecisionMakerBolt.SET_RATES, timestamp, outAttrs);
			        this.just_active = false;
        		}
        		
        		/* if (this.k == 18) { // Synchronisation event
        			Map<String, Object> outAttrs = new HashMap<String, Object>();
			        outAttrs.put("junction_id", 0 );
			        outAttrs.put("dmPartition", "null");
			        outAttrs.put("phase_id", 0);
			        outAttrs.put("phase_time", (int) 0);
			        out_events[4] = eventFactory.createEvent(TrafficDecisionMakerBolt.SET_RATES, timestamp, outAttrs);
        		} */

        		// Write to debug log.
        		if ( TrafficDecisionMakerBolt.DEBUG ) {
        			String str_q = Double.toString( this.onramp_stateEstimator.getDensity() ) ;
        			String str_rho = Double.toString( this.get_merge_density() );
        			// String str_rho = Double.toString( this.mainline_stateEstimator.getDensity() );
        			String str_phi = Double.toString( this.mainline_stateEstimator.getFlow() );
        			String str_density_control = Integer.toString( this.coordination.get_density_control() ? 1 : 0 );
        			String str_queue_control = Integer.toString( this.coordination.get_queue_control() ? 1 : 0 );
        			String str_request_coordination = Integer.toString( this.coordination.get_request_coordination() ? 1 : 0 );
        			String str_rhoc = Double.toString( this.mainline_sysId.get_rhoc() );
        			this.out.println(str_q + ", " + str_rho + ", " + str_phi + ", " + str_density_control + ", " + str_queue_control + ", " + str_request_coordination + ", " + str_rhoc);
        		}
        		
        	}
        } 
        
		return out_events; // return array of newly created events
	}
	
	/**
	 * Lookup table for "old" location id.
	 * USED FOR UI, EVEN IN CLOSED LOOP WITH AIMSUN.
	 */
	private String lookupLocation(int id) {
		if ((id == 4078) || (id == 1708)) {
			return new String("0024a4dc00003356");
		} else 		if ((id == 4048) || (id == 1703) || (id == 4085) || (id == 4489)) {
			return new String("0024a4dc00003354");
		} else 		if ((id == 4244)) {
			return new String("0024a4dc0000343c");
		} else 		if ((id == 1687) || (id == 1691) || (id == 3813) || (id == 3814)) {
			return new String("0024a4dc0000343b");
		} else 		if ((id == 1679) || (id == 1683) || (id == 3811) || (id == 4132) || (id == 3812) || (id == 4488)) {
			return new String("0024a4dc00003445");
		} else 		if ((id == 3810) || (id == 1675) || (id == 3810)) {
			return new String("0024a4dc00001b67");
		} else 		if ((id == 4355)) {
			return new String("0024a4dc00003357");
		} else 		if ((id == 4061) || (id == 1670) || (id == 4134)) {
			return new String("0024a4dc00000ddd");
		} else 			if ((id == 4381) || (id == 1666) || (id == 4391) || (id == 4487)) {
			return new String("0024a4dc00003355");
		} else 			if ((id == 4375) || (id == 1662)) {
			return new String("0024a4dc000021d1");
		} else 			if ((id == 4058) || (id == 1658) || (id == 4135) || (id == 4486)) {
			return new String("0024a4dc0000343f");
		} else 			if ((id == 4057) || (id == 1654) || (id == 4136)) {
			return new String("0024a4dc00001b5c");
		} else 			if ((id == 4056) || (id == 1650) || (id == 4166) || (id == 4453)) {
			return new String("0024a4dc000025eb");
		} else 			if ((id == 4055) || (id == 1646)) {
			return new String("0024a4dc000025ea");
		} else 			if ((id == 4138)) {
			return new String("0024a4dc00001c99");
		} else 			if ((id == 4054) || (id == 1642) || (id == 4490)) {
			return new String("0024a4dc000013c6");
		} else 			if ((id == 4053) || (id == 1638)) {
			return new String("0024a4dc00003444");
		} else 			if ((id == 4052) || (id == 1634)) {
			return new String("0024a4dc000025ec");
		} else 			if ((id == 1629) || (id == 1630) || (id == 1628)) {
			return new String("0024a4dc0000343e");
		} else {
			return new String("locationIdNotFound");
		}
	}



}
