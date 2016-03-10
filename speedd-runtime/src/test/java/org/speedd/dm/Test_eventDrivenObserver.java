package org.speedd.dm;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;
import org.junit.Test;

public class Test_eventDrivenObserver {
	
	final double OCCU_NCARS_CONVERSION = (8/5) * 125;
	final double H2MS = 3600 * 1000; // convert hours to milliseconds

	@Test
	public void test() {
		
		/* test freeway: split, metered onramp, offramp, onramp, split, metered onramp, split
		 * 
		 *                +--105--                  |                         +--101--
		 *                v                         v                         v
		 * ( 6 ) <--6-- ( 5 ) <--5-- ( 4 ) <--4-- ( 3 ) <--3-- ( 2 ) <--2-- ( 1 ) <--1-- 
		 *                                                       v
		*/
		// topology information - those should be inputs
		int[] sens_in   = {  1,   2,   3,  -1,   5,   6}; // sensors beginning of cell: sens_id = cell_id
		int[] sens_ou   = {401,  -1, 403,  -1, 405,  -1}; // sensors at the end of the cell: sens_id = cell_id + 400
		int[] sens_on   = {101,  -1, 103,  -1, 105,  -1}; // sensors at the onramp merge: sens_id = cell_id + 100
		int[] sens_qu   = {201,  -1,  -1,  -1, 205,  -1}; // sensors at the beginning of the queue: sens_id = cell_id + 200
		int[] sens_of   = { -1, 302,  -1,  -1,  -1,  -1}; // sensors at the offramp: sens_id + 300
		int[] actu_id   = {  1,  -1,  -1,  -1,   5,  -1}; // actuator_id = intersection_id
		// FIXME: Create struct for these
		double[] beta   = { -1, 0.2,  -1,  -1,  -1,  -1};
		double[] length = { .5,  .5,  .5,  .5,  .5,  .5};
		double[] ql 	= { .5,  0.,  .5,  0.,  .5,  0.};
		
		// create test freeway - this is already a test, if definition is faulty, exception is thrown
		network freeway = network.makeFreeway(sens_in, sens_ou, sens_on, sens_qu, sens_of, actu_id, beta, length, ql, 1);
		network freeway_obs = network.makeFreeway(sens_in, sens_ou, sens_on, sens_qu, sens_of, actu_id, beta, length, ql, 1);
		
		// freeway.printNetwork(); // debug purposes
		
		// create observer
		EventDrivenObserver observer = new EventDrivenObserver(freeway_obs);
		
		// set parameters for demand
		final double PEAK_MAINLINE = 4000.;
		final double PEAK_ONRAMP = 1000.;
		final int n_sample = 60;
		Map<Integer,Double[]> tlpmap = new HashMap<Integer,Double[]>(); // traffic light phases
		
		final double DELTA_NOISEFREE = .001;
		
		// do NOT change these, they are assumed to be fixed:
		final double T_MEASUREMENT = 1./60.; // (h)
		final int n_iter = 4; // ... such that one CTM period is 15s 
		final double T_PERIOD = T_MEASUREMENT/n_iter; 
		final double DURATION = T_MEASUREMENT * n_sample;
		
		// allocate some variables for data storage
		Map<Integer,Double> flowsMap = new HashMap<Integer,Double>();
		Map<Integer,Double> flowsSample = new HashMap<Integer,Double>();
		Map<Integer,Double> ncarsMap = new HashMap<Integer,Double>();
		
		double[][] ncars_ref = new double[10][n_sample]; // [cell][time]
		double[][] ncars_est = new double[10][n_sample]; // [cell][time]
		
		
		for (int ii=0; ii<n_sample; ii++) {
			// construct external demand
			double demand_mainline = T_PERIOD * PEAK_MAINLINE * Math.sin(Math.PI * ii * T_MEASUREMENT / DURATION); // in # of cars
			double demand_onramp = T_PERIOD * PEAK_ONRAMP * Math.sin(Math.PI * ii * T_MEASUREMENT / DURATION); // in # of cars
			Map<Integer,Double> external_demand = new HashMap<Integer,Double>();
			external_demand.put(1,  demand_mainline);
			external_demand.put(101, demand_onramp);
			external_demand.put(103, demand_onramp);
			external_demand.put(105, demand_onramp);
			
			
			// simulate reference model
			flowsMap = new HashMap<Integer,Double>(); // clear variables
			for (int jj=0; jj<n_iter; jj++) {
				flowsSample = freeway.predictFlows(tlpmap, T_PERIOD);
				ncarsMap = freeway.predictDensity(flowsSample, external_demand);		
				flowsMap = AddMaps.add(flowsSample,flowsMap);
				freeway.initDensitites(ncarsMap); // saveback
				
			}
			
			// create measurements
			// FIXME: automate this?
			
			// System.out.println((long) (H2MS * T_MEASUREMENT*ii)); // looks good...
			
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 1, freeway.Roads.get(1).ncars/freeway.Roads.get(1).params.l, 4.*demand_mainline));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 2, freeway.Roads.get(2).ncars/freeway.Roads.get(2).params.l, flowsMap.get(2)));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 3, freeway.Roads.get(3).ncars/freeway.Roads.get(3).params.l, flowsMap.get(3)));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 5, freeway.Roads.get(5).ncars/freeway.Roads.get(5).params.l, flowsMap.get(5)));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 6, freeway.Roads.get(6).ncars/freeway.Roads.get(6).params.l, flowsMap.get(6)));
			
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 401, freeway.Roads.get(1).ncars/freeway.Roads.get(1).params.l, flowsMap.get(-1)));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 403, freeway.Roads.get(3).ncars/freeway.Roads.get(3).params.l, flowsMap.get(-3)));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 405, freeway.Roads.get(5).ncars/freeway.Roads.get(5).params.l, flowsMap.get(-5)));
			
			observer.processEvent(onrampMeasurement((long) (H2MS * T_MEASUREMENT*ii), 101, freeway.Roads.get(101).ncars/freeway.Roads.get(101).params.l, flowsMap.get(-101)));
			observer.processEvent(onrampMeasurement((long) (H2MS * T_MEASUREMENT*ii), 103, freeway.Roads.get(103).ncars/freeway.Roads.get(103).params.l, flowsMap.get(-103)));
			observer.processEvent(onrampMeasurement((long) (H2MS * T_MEASUREMENT*ii), 105, freeway.Roads.get(105).ncars/freeway.Roads.get(105).params.l, flowsMap.get(-105)));
			
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 201, freeway.Roads.get(101).ncars/freeway.Roads.get(101).params.l, 4*demand_onramp));
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 205, freeway.Roads.get(105).ncars/freeway.Roads.get(105).params.l, 4*demand_onramp));
			
			observer.processEvent(createMeasurement((long) (H2MS * T_MEASUREMENT*ii), 302, 0., 0.)); // note: sensor doesn't actually exist
			
			// saveback
			saveback(ncars_ref,freeway, ii);
			saveback(ncars_est,freeway_obs, ii);
			
			// compare estimates with ground truth
//			assertEquals(freeway.Roads.get(1).ncars, freeway_obs.Roads.get(1).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(2).ncars, freeway_obs.Roads.get(2).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(3).ncars, freeway_obs.Roads.get(3).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(4).ncars, freeway_obs.Roads.get(4).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(5).ncars, freeway_obs.Roads.get(5).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(6).ncars, freeway_obs.Roads.get(6).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(101).ncars, freeway_obs.Roads.get(101).ncars, DELTA_NOISEFREE);
//			assertEquals(freeway.Roads.get(103).ncars, freeway_obs.Roads.get(105).ncars, DELTA_NOISEFREE);
			
		}
		
		// debugging
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[1])); // cell 1
		System.out.println(Arrays.toString(ncars_est[1])); // cell 1

		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[2])); // cell 2
		System.out.println(Arrays.toString(ncars_est[2])); // cell 2
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[3])); // cell 3
		System.out.println(Arrays.toString(ncars_est[3])); // cell 3
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[4])); // cell 4
		System.out.println(Arrays.toString(ncars_est[4])); // cell 4
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[5])); // cell 5
		System.out.println(Arrays.toString(ncars_est[5])); // cell 5
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[6])); // cell 6
		System.out.println(Arrays.toString(ncars_est[6])); // cell 6		
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[7])); // cell 6
		System.out.println(Arrays.toString(ncars_est[7])); // cell 6		
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[8])); // cell 6
		System.out.println(Arrays.toString(ncars_est[8])); // cell 6		
		
		System.out.println();
		System.out.println(Arrays.toString(ncars_ref[9])); // cell 6
		System.out.println(Arrays.toString(ncars_est[9])); // cell 6		
		

	} // end of public Test()
	
	/**
	 * Create an 
	 * @param timestamp
	 * @param sensorId
	 * @param ncars
	 * @param flow
	 * @return
	 */
	private Event createMeasurement(long timestamp, int sensorId, double ncars, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", "not needed");
		attrs.put("average_occupancy", ncars/OCCU_NCARS_CONVERSION);
		attrs.put("average_flow", (Integer) ((int) flow));
		return SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocation", timestamp, attrs);
	}
	private Event onrampMeasurement(long timestamp, int sensorId, double ncars, double flow) {
		// Create a mainline measurement event
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("sensorId", Integer.toString(sensorId));
		attrs.put("dmPartition", "not needed");
		attrs.put("average_occupancy", ncars/OCCU_NCARS_CONVERSION);
		attrs.put("average_flow", (Integer) ((int) flow));
		return SpeeddEventFactory.getInstance().createEvent("AverageDensityAndSpeedPerLocation", timestamp, attrs);
	}

	private <T> void PlotMap(Map<Integer,T> map) {
		for (Map.Entry<Integer,T> entry : map.entrySet()) {
			System.out.println("key: " + entry.getKey() + " value: " + entry.getValue());
		}
	}

	private void saveback(double[][] ncars, network freeway, int time) {
		ncars[1][time] = freeway.Roads.get(1).ncars;
		ncars[2][time] = freeway.Roads.get(2).ncars;
		ncars[3][time] = freeway.Roads.get(3).ncars;
		ncars[4][time] = freeway.Roads.get(4).ncars;
		ncars[5][time] = freeway.Roads.get(5).ncars;
		ncars[6][time] = freeway.Roads.get(6).ncars;
		ncars[7][time] = freeway.Roads.get(101).ncars;
		ncars[8][time] = freeway.Roads.get(103).ncars;
		ncars[9][time] = freeway.Roads.get(105).ncars;
	}
}





