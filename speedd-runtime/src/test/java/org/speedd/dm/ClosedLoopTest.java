package org.speedd.dm;

import backtype.storm.task.IOutputCollector;
import backtype.storm.task.OutputCollector;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClosedLoopTest {	
	
	// not certain what this does
	public class TestCollector extends OutputCollector {

	public List<Object> tuple;
	
	public TestCollector(IOutputCollector delegate) {
		super(delegate);
	}
	
	@Override
	public List<Integer> emit(List<Object> tuple) {
		this.tuple = tuple;
		
		return null;
	}
	
}

	// queue lenghts needed to compute queue density externally
	private final static double[] ql   = GrenobleTopology.ql;
	// sensor location information
	private final static int[] sens_in = GrenobleTopology.sens_in;
	private final static int[] sens_ou = GrenobleTopology.sens_ou; 
	private final static int[] sens_me = GrenobleTopology.sens_me;  
	private final static int[] sens_qu = GrenobleTopology.sens_qu;
	private final static int[] sens_on = GrenobleTopology.sens_on;
	private final static int[] actu_id = GrenobleTopology.actu_id;
	// number of cells
	private final int N = 21;

	@Test
	public void test() {
		
		// simulation parameters - modify as desired
		Boolean CLOSED_LOOP = true;
		Boolean NOISE = true;

		double std_dens = 0.2;
		double std_flow = 0.05;

		double eps = 1e-3;

		double dt = 15./3600.; // t <= 16sec for stability
		int SAMPLING_INTERVAL = 4;
		int T = 500; // fitted for 'nice' congestion pattern
		
		double D_ML_MAX = 3800;
		double D_ON_MAX = 1300;
		
		// prepare for simulation
		Pwa_freeway freeway = new Pwa_freeway(); // create according to simplified grenoble model
		
		TestCollector  collector = new TestCollector(null); // for reading out-events
		TrafficDecisionMakerBolt myBolt = new TrafficDecisionMakerBolt(); // to be tested
		myBolt.prepare(null,null,collector); // link test-Bolt to collector
		
		// allocate some variables
		double[][] Phi = new double[T][22];		
		double[][] Rho = new double[T][21];
		double[][] Q = new double[T][21];
		double[][] R = new double[T][21];
		double[][] D = new double[T][21];
		double[][] Rate = new double[T][21];
		double[] rate = {1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800.}; // 21
		double TTT = 0.;
		
		// simulate for T steps
		for (int t=0; t<T; t++) {
			
			// determine demand
			double d_ml = D_ML_MAX * Math.max(0., Math.sin( Math.PI * 2* (((double) t)/T) ));
			double d_on = D_ON_MAX * Math.max(0., Math.sin( Math.PI * 2* (((double) t)/T) ));
			double[] demand = {0., d_on, 0., 0., 0., 0., d_on, 0., 0., 0., d_on, 0., 0., d_on, 0., d_on, 0., 0., d_on, 0., 0.}; //21
			
			// simulate, save back variables
			if (CLOSED_LOOP) {
				Phi[t] = freeway.sim_step(demand, rate, d_ml, dt); // closed loop
			} else {
				Phi[t] = freeway.sim_step(demand, demand, d_ml, dt); // rates == demand : open-loop
			}
			Rho[t] = freeway.get_densities();
			Q[t] = freeway.get_queues();
			R[t] = freeway.get_inflow();
			D[t] = freeway.get_demand();
			TTT += freeway.compute_TTT();
			
			// create measurement events
			for (int ii=0; ii<N; ii++) {
				Rate[t][ii] = rate[ii];
				Event[] out_events = null;
				
				if ((t % SAMPLING_INTERVAL) == 0) {
				
					// mainline measurements
					if (sens_ou[ii] > 0) {
						// get values
						double rho = Rho[t][ii];
						double phi = Phi[t][ii+1];
						if ( NOISE ) {
							phi = phi * (1 + std_flow*(Math.random() - 0.5));
							rho = rho * (1 + std_dens*(Math.random() - 0.5));
						}
						// set attributes
						Map<String, Object> attrs = new HashMap<String, Object>();
						attrs.put("sensorId", Integer.toString(sens_ou[ii]) );
						attrs.put("dmPartition", "test");
						attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * rho );
						attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * phi );
						attrs.put("average_speed", phi / (eps + rho) );
						attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * std_dens * rho );
						attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * std_flow * phi );
						// create & execute event
						Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocationOverInterval", 1, attrs);
						myBolt.execute(newEvent);
					}
					if (sens_me[ii] > 0) {
						// get values
						double rho = Rho[t][ii+1];
						double phi = Phi[t][ii+2];
						if ( NOISE ) {
							phi = phi * (1 + std_flow*(Math.random() - 0.5));
							rho = rho * (1 + std_dens*(Math.random() - 0.5));
						}
						// set attributes
						Map<String, Object> attrs = new HashMap<String, Object>();
						attrs.put("sensorId", Integer.toString(sens_me[ii]) );
						attrs.put("dmPartition", "test");
						attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * rho );
						attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * phi );
						attrs.put("average_speed", phi / (eps + rho) );
						attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * std_dens * rho );
						attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * std_flow * phi );
						// create & execute event
						Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocationOverInterval", 1, attrs);
						myBolt.execute(newEvent);
					}
					if (sens_in[ii] > 0) {
						// get values
						double rho = Rho[t][ii-1];
						double phi = Phi[t][ii];
						if ( NOISE ) {
							phi = phi * (1 + std_flow*(Math.random() - 0.5));
							rho = rho * (1 + std_dens*(Math.random() - 0.5));
						}
						// set attributes
						Map<String, Object> attrs = new HashMap<String, Object>();
						attrs.put("sensorId", Integer.toString(sens_in[ii]) );
						attrs.put("dmPartition", "test");
						attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * rho );
						attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * phi );
						attrs.put("average_speed", phi / (eps + rho) );
						attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * std_dens * rho );
						attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * std_flow * phi );
						// create & execute event
						Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocationOverInterval", 1, attrs);
						myBolt.execute(newEvent);
					}
					// onramp measurements
					if (sens_on[ii] > 0) {
						Map<String, Object> attrs = new HashMap<String, Object>();
						attrs.put("sensorId", Integer.toString(sens_on[ii]) );
						attrs.put("dmPartition", "test");
						attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * (Q[t][ii]/ql[ii]) );
						attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * R[t][ii]);
						attrs.put("average_speed", -1. );
						attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * std_flow * R[t][ii] );
						attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * 0.5 * (Q[t][ii]/ql[ii]) );
						Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageOnRampValuesOverInterval", 1, attrs);
						out_events = myBolt.execute(newEvent);
					}
					if (sens_qu[ii] > 0) {
						Map<String, Object> attrs = new HashMap<String, Object>();
						attrs.put("sensorId", Integer.toString(sens_qu[ii]) );
						attrs.put("dmPartition", "test");
						attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * (Q[t][ii]/ql[ii]) );
						attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * D[t][ii]);
						attrs.put("average_speed", -1. );
						attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * std_flow * D[t][ii] );
						attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * 0.5 * (Q[t][ii]/ql[ii]) );
						Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageOnRampValuesOverInterval", 1, attrs);
						myBolt.execute(newEvent);
					}
					// termination event for debugging
					if ((t==T-1) && (actu_id[ii] > 0)) {
						Map<String, Object> attrs = new HashMap<String, Object>();
						attrs.put("sensorId", Integer.toString( actu_id[ii] ) );
						attrs.put("dmPartition", "test");
						Event newEvent = SpeeddEventFactory.getInstance().createEvent("End", 1, attrs);
						myBolt.execute(newEvent);
					}
					
					// read eventual out-events
					if (out_events != null) {
						for (int jj=0; jj<out_events.length; jj++) {
							if (out_events[jj] != null) {
								if (out_events[jj].getEventName().equals("SetTrafficLightPhases")) {
									 double phase_time = (double)((int) out_events[jj].getAttributes().get("phase_time"));
									 int junction_id = (int) out_events[jj].getAttributes().get("junction_id");
									 // double phase_id = (int) out_events[jj].getAttributes().get("phase_id"); // assume this is the correct one?
									 rate[ GrenobleTopology.get_index(junction_id) ] = (1800./60.) * phase_time;
								 } else if (out_events[jj].getEventName().equals("AggregatedQueueRampLength")) {
									 // double queueLength = (Double) (out_events[jj].getAttributes().get("queueLength"));
									 // double maxQueueLength = (Double) (out_events[jj].getAttributes().get("maxQueueLength"));
									 // String sensor_id = (String) out_events[jj].getAttributes().get("sensorId");
									 // COMMENT MS: Plausibility of predictions checked via log files.
								 } else if (out_events[jj].getEventName().equals("CoordinateRamps")) {
									 myBolt.execute( out_events[jj] );
								 }
							}
						}
					} else {
						// don't change anything, keep rates as they are!
					}
				}
				
			}
			
		}
		System.out.println("Total Travel Time: " + Double.toString(TTT));
		
		
		PrintWriter writer;
		try {
			File file = new File ("dens.txt");
			writer = new PrintWriter(file, "UTF-8");
			writer.println(Arrays.deepToString(Rho));
			writer.close();
			
			file = new File ("flows.txt");
			writer = new PrintWriter(file, "UTF-8");
			writer.println(Arrays.deepToString(Phi));
			writer.close();
			
			file = new File ("queues.txt");
			writer = new PrintWriter(file, "UTF-8");
			writer.println(Arrays.deepToString(Q));
			writer.close();
			
			file = new File ("rates.txt");
			writer = new PrintWriter(file, "UTF-8");
			writer.println(Arrays.deepToString(Rate));
			writer.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
	}

}
