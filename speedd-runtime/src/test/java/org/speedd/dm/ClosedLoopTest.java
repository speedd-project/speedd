package org.speedd.dm;

import static org.junit.Assert.*;
import backtype.storm.task.IOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.TupleImpl;
import backtype.storm.tuple.Values;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
	private final static double[] ql   = {  0.1,  0.4,  -1.,  -1., 0.12,  -1., 0.15, 0.12,  -1.,  -1.,  0.3,        -1.,  0.2,  -1.,  0.2,        -1.,  0.2,  -1., 0.06      };
	// sensor location information
	private final static int[] sens_in = {   -1,   -1,   -1,   -1,   -1,   -1, 3812,   -1,   -1,   -1, 4061,       4391, 4375,   -1, 4057,       4166, 4055,   -1, 4053      };
	private final static int[] sens_ou = { 4087, 4084, 4244,   -1, 3813,   -1, 3811, 3810, 4355,   -1, 4381,         -1, 4058,   -1, 4056,         -1, 4054,   -1, 4052      };  
	private final static int[] sens_qu = {   -1, 4085,   -1,   -1,  - 1,   -1, 4132,   -1,   -1,   -1, 4134,         -1, 4135,   -1, 4136,         -1, 4138,   -1,   -1      };
	private final static int[] sens_on = { 1708, 1703,   -1,   -1, 1687,   -1, 1679, 1675,   -1,   -1, 1666,         -1, 1658,   -1, 1650,         -1, 1642,   -1, 1634      };
	private final static int[] sens_of = {   -1,   -1,   -1, 1691,   -1, 1683,   -1,   -1,   -1, 1670,   -1,       1662,   -1, 1654,   -1,       1646,   -1, 1638,   -1      };
	private final static int[] actu_id = {   -1, 4489,   -1,   -1,   -1,   -1, 4488,   -1,   -1,   -1, 4487,         -1, 4486,   -1, 4453,         -1, 4490,   -1,   -1      };
	// number of cells
	private final int N = 19;

	@Test
	public void test() {

		double dt = 15./3600.; // t <= 16sec for stability
		int T = 500; // fitted for 'nice' congestion pattern
		
		double D_ML_MAX = 3500;
		double D_ON_MAX = 1200;
		
		
		Boolean CLOSED_LOOP = true;
		

		double[] fd_scales = {500.};
		
		
		// FIXME: Make explicit. Hard to judge if results make sense.
		RealVector[] trainings_data = new RealVector[4]; 
		double[] buffer = {0.}; trainings_data[0] = new ArrayRealVector( buffer );
		buffer[0] = 40.; trainings_data[1] = new ArrayRealVector( buffer );
		buffer[0] = 90.; trainings_data[2] = new ArrayRealVector( buffer );
		buffer[0] = 200.; trainings_data[3] = new ArrayRealVector( buffer );
		double[] targets = {0., 5000., 3000., 0.};
		RealVector trainings_targets = new ArrayRealVector(targets);
		GaussianProcess realFD = new GaussianProcess(trainings_data, trainings_targets, fd_scales, 1., 200.);
		
		
		
		
		

		Pwa_freeway freeway = new Pwa_freeway(); // create according to simplified grenoble model
		
		TestCollector  collector = new TestCollector(null); // for reading out-events
		TrafficDecisionMakerBolt myBolt = new TrafficDecisionMakerBolt(); // to be tested
		myBolt.prepare(null,null,collector); // link test-Bolt to collector
		
		// allocate some variables
		double[][] Phi = new double[T][20];		
		double[][] Rho = new double[T][19];
		double[][] Q = new double[T][19];
		double[][] R = new double[T][19];
		double[][] D = new double[T][19];
		double[][] Rate = new double[T][19];
		
		double[] rate = {1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800., 1800.};
		
		// simulate for T steps
		for (int t=0; t<T; t++) {
			
			// determine demand
			double d_ml = D_ML_MAX * Math.max(0., Math.sin( Math.PI * 2* (((double) t)/T) ));
			double d_on = D_ON_MAX * Math.max(0., Math.sin( Math.PI * 2* (((double) t)/T) ));
			double[] demand = {0., d_on, 0., 0., 0., 0., d_on, 0., 0., 0., d_on, 0., d_on, 0., d_on, 0., d_on, 0., 0.};
			
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
			
			
			
			// create measurement events
			for (int ii=0; ii<N; ii++) {
				
				Rate[t][ii] = rate[ii];
				
				double eps = 1e-6;
				
				Event[] out_events = null;
				
				if (sens_ou[ii] > 0) {
					Map<String, Object> attrs = new HashMap<String, Object>();
					attrs.put("sensorId", Integer.toString(sens_ou[ii]) );
					attrs.put("dmPartition", "test");
					
					double ds_rho = Rho[t][ii] * (Phi[t][ii] / (eps + Phi[t][ii] + R[t][ii]));
					attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * ds_rho );
					double[] datum = {ds_rho}; double phi = realFD.predict( new ArrayRealVector( datum ) )[0] * (1 + 0.3*(Math.random() - 0.5));
					attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * phi * (phi / (eps + phi + R[t][ii])) );
					
					attrs.put("average_speed", Phi[t][ii] / (eps + Rho[t][ii]) );
					attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * 0.1 * Phi[t][ii] );
					attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * 0.2 * Rho[t][ii] );
					Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocationOverInterval", 1, attrs);
					myBolt.execute(newEvent);
				}
				if (sens_in[ii] > 0) {
					Map<String, Object> attrs = new HashMap<String, Object>();
					attrs.put("sensorId", Integer.toString(sens_in[ii]) );
					attrs.put("dmPartition", "test");
					
					double ds_rho = Rho[t][ii] * (Phi[t][ii] / (eps + Phi[t][ii] + R[t][ii]));
					attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * ds_rho );
					double[] datum = {ds_rho}; double phi = realFD.predict( new ArrayRealVector( datum ) )[0] * (1 + 0.3*(Math.random() - 0.5));
					attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * phi * (phi / (eps + phi + R[t][ii])) );
					
					attrs.put("average_speed", Phi[t][ii] / (eps + Rho[t][ii]) );
					attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * 0.1 * Phi[t][ii] );
					attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * 0.2 * Rho[t][ii] );
					Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocationOverInterval", 1, attrs);
					myBolt.execute(newEvent);
				}
				if (sens_on[ii] > 0) {
					Map<String, Object> attrs = new HashMap<String, Object>();
					attrs.put("sensorId", Integer.toString(sens_on[ii]) );
					attrs.put("dmPartition", "test");
					attrs.put("average_occupancy", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * (Q[t][ii]/ql[ii]) );
					attrs.put("average_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * R[t][ii]);
					attrs.put("average_speed", -1. );
					attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * 0.000001 * R[t][ii] );
					attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * 0.002 * (Q[t][ii]/ql[ii]) );
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
					attrs.put("standard_dev_flow", (1/TrafficDecisionMakerBolt.CARS_2_FLOW) * 0.000001 * D[t][ii] );
					attrs.put("standard_dev_density", (1/TrafficDecisionMakerBolt.OCCU_2_DENS) * 0.002 * (Q[t][ii]/ql[ii]) );
					Event newEvent = SpeeddEventFactory.getInstance().createEvent("AverageOnRampValuesOverInterval", 1, attrs);
					myBolt.execute(newEvent);
				}
				
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
								 
								 rate[ GrenobleTopology.get_index(junction_id) - 1 ] = (1800./60.) * phase_time;
	
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
