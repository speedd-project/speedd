package org.speedd.dm;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestFreeway {

	@Test
	public void test() {
		
		/* test freeway: split, metered onramp, offramp, onramp, split, metered onramp, split
		 * 
		 *                +--105--                  |                         +--101--
		 *                v                         v                         v
		 * ( 6 ) <--6-- ( 5 ) <--5-- ( 4 ) <--4-- ( 3 ) <--3-- ( 2 ) <--2-- ( 1 ) <--1-- ( 0 ) <--0--
		 *                                                       v
		*/
		// topology information - those should be inputs
		int[] sens_in   = {  0,   1,   2,   3,   4,   5,   6}; // sensors beginning of cell: sens_id = cell_id
		int[] sens_ou   = { -1,  -1,  -1, 403,  -1,  -1,  -1}; // sensors at the end of the cell: sens_id = cell_id + 400
		int[] sens_on   = { -1, 101,  -1, 103,  -1, 105,  -1}; // sensors at the onramp merge: sens_id = cell_id + 100
		int[] sens_qu   = { -1, 201,  -1,  -1,  -1, 205,  -1}; // sensors at the beginning of the queue: sens_id = cell_id + 200
		int[] sens_of   = { -1,  -1, 302,  -1,  -1,  -1,  -1}; // sensors at the offramp: sens_id + 300
		int[] actu_id   = { -1,   1,  -1,  -1,  -1,   5,  -1}; // actuator_id = intersection_id
		// FIXME: Create struct for these
		double[] beta   = { -1,  -1, 0.2,  -1,  -1,  -1,  -1};
		double[] length = { 1.,  1.,  1.,  1.,  1.,  1.,  1.};
		double[] ql 	= { .0,  .5,  0.,  .5,  0.,  .5,  0.};
		
		// create test freeway - this is already a test, if definition is faulty, exception is thrown
		network freeway = network.makeFreeway(sens_in, sens_ou, sens_on, sens_qu, sens_of, actu_id, beta, length, ql, 0);
		freeway.printNetwork(); // debug purposes
		
		// create controller
		DistributedRM controller = new DistributedRM(freeway);
		
		// test "sensor2onramp"
		assertEquals(null, controller.sensor2onramp(0)); // no onramp found
		assertEquals(null, controller.sensor2onramp(123)); // sensor doesn't exist
		assertEquals(1, controller.sensor2onramp(1).ramp);
		assertEquals(1, controller.sensor2onramp(2).ramp);
		assertEquals(1, controller.sensor2onramp(403).ramp);
		assertEquals(1, controller.sensor2onramp(4).ramp);
		assertEquals(5, controller.sensor2onramp(5).ramp);
		assertEquals(5, controller.sensor2onramp(6).ramp);
		assertEquals(1, controller.sensor2onramp(3).ramp);
		assertEquals(1, controller.sensor2onramp(403).ramp);
		// TODO: check entries of controller lookup tables!
		
		
		// test "processEvent"
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("sensorId", "403");
		onrampStruct result = controller.processEventDebug("Congestion", 0, attributes);
		assertEquals(1, result.ramp);
		assertEquals(1, result.operationMode);
		
		attributes.put("sensorId", "3");
		attributes.put("lowerLimit",(Double) 100.);
		result = controller.processEventDebug("setMeteringRateLimits", 0, attributes);
		assertEquals(1, result.ramp); // should remain from before
		assertEquals(1, result.operationMode);
		assertEquals((Double) 100., result.minFlow);
		
		attributes.put("sensorId", "3");
		result = controller.processEventDebug("ClearCongestion", 0, attributes);
		assertEquals(1, result.ramp);
		assertEquals(0, result.operationMode);
		
		attributes.put("sensorId", "5");
		attributes.put("upperLimit", 2000.);
		result = controller.processEventDebug("setMeteringRateLimits", 0, attributes);
		assertEquals(5, result.ramp);
		assertEquals(0, result.operationMode);
		assertEquals((Double) 2000., result.maxFlow); // FIXME: Disallow values > 1800
		
		attributes.put("upperLimit", -100.); // invalid
		result = controller.processEventDebug("setMeteringRateLimits", 0, attributes);
		assertEquals(5, result.ramp);
		assertEquals(0, result.operationMode);
		assertEquals((Double) 1800., result.maxFlow); // note: 1800 is the upper bound, so no artifical limit
		
		
		// test "double computeDutyCycle(int sensorId, double demand, double dt)"
		Map<Integer,Double> init_cars = new HashMap<Integer,Double>();
		init_cars.put(0, 40.);
		init_cars.put(1, 40.);
		init_cars.put(2, 40.);
		init_cars.put(3, 40.);
		init_cars.put(4, 40.);
		init_cars.put(5, 40.);
		init_cars.put(6, 40.);
		init_cars.put(101, 0.);
		init_cars.put(103, 0.);
		init_cars.put(105, 0.);
		freeway.initDensitites(init_cars); // initialize densities
		double eps = 1e-6;
		attributes.put("sensorId", "2");
		attributes.put("lowerLimit", -1.); // disable lower limit
		controller.processEventDebug("setMeteringRateLimits", 0, attributes);
		assertEquals(-1., controller.computeDutyCycle(101, 0, 120./3600), eps); // controller not active 
		attributes.put("sensorId", "2");
		controller.processEventDebug("Congestion", 0, attributes);
		assertEquals(10/60., controller.computeDutyCycle(101, 0, 120./3600), eps); // active
		init_cars.put(1, 50.);
		freeway.initDensitites(init_cars); // initialize densities
		assertEquals(0., controller.computeDutyCycle(101, 0, 120./3600), eps); // at critical density
		init_cars.put(1, 60.);
		freeway.initDensitites(init_cars); // initialize densities
		assertEquals(0., controller.computeDutyCycle(101, 0, 120./3600), eps); // above critical density
		attributes.put("sensorId", "2");
		attributes.put("lowerLimit", 100.); // disable lower limit
		controller.processEventDebug("setMeteringRateLimits", 0, attributes);
		assertEquals(100./1800, controller.computeDutyCycle(101, 0, 120./3600), eps); // above critical density, but lower limit active
		init_cars.put(101, 0.5*125.);
		freeway.initDensitites(init_cars); // initialize densities
		assertEquals(300./1800., controller.computeDutyCycle(101, 300, 120./3600), eps); // above critical density, but onramp overflow imminent
		
		
		// test simulations of flows and densities
		init_cars.put(0, 30.);
		init_cars.put(1, 30.);
		init_cars.put(2, 90.);
		init_cars.put(3, 90.);
		init_cars.put(4, 90.);
		init_cars.put(5, 30.);
		init_cars.put(6, 30.);
		init_cars.put(101, 0.);
		init_cars.put(103, 0.);
		init_cars.put(105, 0.);
		freeway.initDensitites(init_cars); // initialize densities
		// ... flows
		double dt = 30./3600;
		Map<Integer,Double[]> iTLPmap = new HashMap<Integer,Double[]>();
		Map<Integer,Double> flows = freeway.predictFlows(iTLPmap, dt);
		assertEquals(dt*90*30., flows.get(0), eps);
		assertEquals(dt*90*30., flows.get(1), eps);
		assertEquals(dt*(250-90)*90/(4*0.8), flows.get(-2), eps); // before offramp
		assertEquals(dt*(250-90)*90/4, flows.get(3), eps); // after offramp
		assertEquals(dt*4500., flows.get(-4), eps);
		assertEquals(dt*4500., flows.get(5), eps);
		// ... densities
		Map<Integer,Double> externalDemand = new HashMap<Integer,Double>();
		externalDemand.put(0,10.);
		externalDemand.put(2, 5.);
		externalDemand.put(6,10.);
		Map<Integer,Double> densities = freeway.predictDensity(flows, externalDemand);
		double flow_FF = dt*90*30;
		double flow_CC = dt*(250-90)*90/4;
		double flow_FC = flow_FF;
		double flow_CF = dt*4500.;
		// assertEquals(30.-flow_FF+10., densities.get(0), eps); FIXME: Bug, for road ID = 0, cannot sign to encode in- or outflow
		assertEquals(30.+flow_FF-flow_FC, densities.get(1), eps);
		assertEquals(90.+flow_FC-flow_CC/0.8+5, densities.get(2), eps);
		assertEquals(90.+flow_CC-flow_CC, densities.get(3), eps);
		assertEquals(90.+flow_CC-flow_CF, densities.get(4), eps);
		assertEquals(30.+flow_CF-flow_FF, densities.get(5), eps);
		assertEquals(30.+flow_FF-flow_FF+10, densities.get(6), eps);
		
		
		

	} // end of public Test()


}
