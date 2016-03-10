package org.speedd.dm;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestIntersection {

	@Test
	public void test() {

		// run tests for 4way network
		maketest4way();
		
		
	}

	
	// initialize map
	public static <T> Map<Integer,T> initMap(Integer[] key, T[] value) {
		Map<Integer,T> mymap = new HashMap<Integer,T>();
		for (int ii=0;ii<value.length;ii++) {
			mymap.put(key[ii], value[ii]);
		}
		return mymap;
	}
	// saveback data from map to array
	private static Double[] readMap(Map<Integer,Double> data, Integer[] index) {
		Double[] values = new Double[index.length];
		for (int ii=0;ii<index.length;ii++) {
			values[ii] = data.get(index[ii]);
		}
		return values;
	}
	
	// check results: compare to maps
	private static void assertEqualsMapEpsilon(Map<Integer,Double> expected, Map<Integer,Double> actual, double epsilon) {
	    assertEquals(expected.size(), actual.size());
	    for(Map.Entry<Integer,Double> value:expected.entrySet()){
	        Double actualValue = actual.get(value.getKey());
	        assertNotNull(actualValue);
	        assertEquals(value.getValue(), actualValue, epsilon);
	    }
	}
	// print map: for debugging
	private void printMap(Map<Integer,Double> mymap) {
		for (Map.Entry<Integer,Double> entry : mymap.entrySet()) {
			System.out.println("Key " + entry.getKey() + " Value " + entry.getValue());
		}
	}
	
	
	// create "4way network"
	private network create_4way() {
			// create test network: 4way intersection
			HashMap<Integer,Road> Roads = new HashMap<Integer,Road>();
			HashMap<Integer,Intersection> Intersections = new HashMap<Integer,Intersection>();
			
			// define roads
			int[] intersection_begin = {0,0,0,0,-1,-1,-1,-1};
			int[] intersection_end = {1,2,3,4,0,0,0,0};
			int[] sensor_begin = {-1,-1,-1,-1,-1,-1,-1,-1};
			int[] sensor_end = {-1,-1,-1,-1,-1,-1,-1,-1};
			String type = "city"; // same for all roads
			Ctm ctmparams = new Ctm(50.,50.,150.,1.); // same for all roads: v=50; w=25; rhoc=50; rhom=150; F=2500; l=1;
			for (int ii=0;ii<=7;ii++) {
				Road road = new RoadCtm(intersection_begin[ii], intersection_end[ii], sensor_begin[ii], sensor_end[ii], type, ctmparams);
				Roads.put(ii+1, road);
				}
			
			// define intersections
			int[][] roads_in_ID = {{5,6,7,8},{1},{2},{3},{4}};
			int[][] roads_out_ID = {{1,2,3,4},{-1},{-1},{-1},{-1}};
			int[] actuator_ID = {-1,-1,-1,-1,-1};
			double[] priorities4way = {1.,1.,1.,1.};	// equal priority
			double l = 0.2; // percentage of cars turning left
			double[][][] TLP4way = { {{0.,0.,1-l,0.},{0.,0.,l,0.},{1-l,0.,0.,0.},{l,0.,0.,0.}},		// phase 1
									 {{0.,l,0.,0.},{0.,0.,0.,1-l},{0.,0.,0.,l},{0.,1-l,0.,0.}} };	// phase 2
			double[][] TPM4way = {{0.,l,1-l,0.},{0.,0.,l,1-l},{1-l,0.,0.,l},{l,1-l,0.,0.}};			// equivalently: phase 1 + phase 2
			double[] priorities1end = {1.};
			double[][][] TLP1end = {{{1.}}};
			double[][] TPM1end = {{1.}};
			Fifo params4way = new Fifo(priorities4way, TLP4way, TPM4way);
			Fifo params1end = new Fifo(priorities1end, TLP1end, TPM1end);
			Intersection new_intersection4 = new IntersectionFifo(0, roads_in_ID[0], roads_out_ID[0], actuator_ID[0], params4way);
			Intersections.put(0, new_intersection4);
			for (int ii=1;ii<=4;ii++) {
				Intersection new_intersection1 = new IntersectionFifo(ii, roads_in_ID[ii], roads_out_ID[ii], actuator_ID[ii], params1end);
				Intersections.put(ii,  new_intersection1);
			}
			return new network(Intersections,Roads);
		}
	// create tests for "4way network"
	private void maketest4way() {
		// test FIFO rules on 4way intersection
				Double[][] ncars_init = {
						{20.,20.,20.,20.,20.,20.,20.,20.},
						{20.,20.,20.,20.,20.,20.,20.,20.},
						{110.,110.,110.,110.,110.,110.,110.,110.}
						};
				double[][] inflows = {
						{0.,0.,0.,0.,0.,0.,0.,0.,},
						{0.,0.,0.,0.,0.,0.,0.,0.,},
						{0.,0.,0.,0.,0.,0.,0.,0.,}
						};
				Double[][][] iTLF = {
						{{1.,0.},{1.},{1.},{1.},{1.}},
						{{0.,1.},{1.},{1.},{1.},{1.}},
						{{1.,0.},{1.},{1.},{1.},{1.}}
						};
				Double[][] flows_in = {
						// 1    2   3   4
						{ 8. , 2. , 8. , 2. },
						{ 2. , 8. , 2. , 8. },
						{10. , 2.5,10. , 2.5}
						};
				Double[][] flows_out = {
						// 1    2    3    4    5    6    7    8
						{10. ,10. ,10. ,10. ,10. , 0. ,10. , 0. },
						{10. ,10. ,10. ,10. , 0. ,10. , 0. ,10. },
						{25. ,25. ,25. ,25. ,12.5, 0. ,12.5, 0. }
						};
				double[][] ncars_new = {
						{0.,0.,0.,0.,0.,0.,0.,0.},
						{0.,0.,0.,0.,0.,0.,0.,0.},
						{0.,0.,0.,0.,0.,0.,0.,0.}
						};
				double[] dt = {0.01, 0.01, 0.01};
				
				
				for (int ii=0; ii<3; ii++) {
					test4way(ncars_init[ii], inflows[ii], iTLF[ii], dt[ii], flows_in[ii], flows_out[ii], ncars_new[ii]);
				}
	}
	// test "4way network"
	private void test4way(Double[] ncars_init, double[] inflows, Double[][] iTLF, double dt, Double[] flows_in, Double[] flows_out, double[] ncars_new) {
		network test4way = create_4way();
		Integer[] road_ids_in = {1,2,3,4};
		Integer[] road_ids = {1,2,3,4,5,6,7,8};
		Integer[] road_ids_out = {-1,-2,-3,-4,-5,-6,-7,-8};
		Map<Integer,Double> flows_new = initMap(road_ids_in,flows_in);
		flows_new.putAll(initMap(road_ids_out,flows_out));
		Integer[] intersection_ids = {0,1,2,3,4};
		Map<Integer,Double[]> iTLFmap = initMap(intersection_ids,iTLF);
		
		test4way.initDensitites(initMap(road_ids, ncars_init));			
		Map<Integer,Double> flows = test4way.predictFlows(iTLFmap, dt);
		
		assertEqualsMapEpsilon(flows,flows_new,0.001);
		

	}

	
	
	
	
}
