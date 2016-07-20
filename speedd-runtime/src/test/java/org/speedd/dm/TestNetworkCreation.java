package org.speedd.dm;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestNetworkCreation{

	@Test
	public void test() {
		
		// prepare data structures
		Map<Integer,Intersection> intersections = new HashMap<Integer,Intersection>();
		Map<Integer,Road> roads = new HashMap<Integer,Road>();
		Fifo paramsFifo = Fifo.makeSplit();
		Ctm paramsCtm = new Ctm("small", 1.);
		Road inRoad;
		Road outRoad;
		
		// invalid ID, incoming
		IntersectionFifo intersection = new IntersectionFifo(0, new int[] {-2}, new int[] {1}, 0, paramsFifo); 
		intersections.put(0, intersection);
		Boolean test = false;
		try {
			new network(intersections,roads);
		} catch(IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
			test = true;
		}
		assertTrue(test);
		
		// no road found, incoming
		intersection = new IntersectionFifo(0, new int[] {0}, new int[] {1}, 0, paramsFifo); 
		intersections.put(0, intersection);
		test = false;
		try {
			new network(intersections,roads);
		} catch(IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
			test = true;
		}
		assertTrue(test);

		// ID mismatch
		intersection = new IntersectionFifo(0, new int[] {0}, new int[] {1}, 0, paramsFifo); 
		intersections.put(0, intersection);
		inRoad = new RoadCtm(-1,1,-1,-1,"small",paramsCtm);
		roads.put(0, inRoad);
		test = false;
		try {
			new network(intersections,roads);
		} catch(IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
			test = true;
		}
		assertTrue(test);
		
		// invalid ID, outgoing
		intersection = new IntersectionFifo(0, new int[] {0}, new int[] {-2}, 0, paramsFifo); 
		intersections.put(0, intersection);
		inRoad = new RoadCtm(-1,0,-1,-1,"small",paramsCtm);
		roads.put(0, inRoad);
		test = false;
		try {
			new network(intersections,roads);
		} catch(IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
			test = true;
		}
		assertTrue(test);
		
		// ID not found, outgoing
		intersection = new IntersectionFifo(0, new int[] {0}, new int[] {1}, 0, paramsFifo); 
		intersections.put(0, intersection);
		test = false;
		try {
			new network(intersections,roads);
		} catch(IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
			test = true;
		}
		assertTrue(test);
		
		// ID mismatch, outgoing
		intersection = new IntersectionFifo(0, new int[] {0}, new int[] {1}, 0, paramsFifo); 
		intersections.put(0, intersection);
		outRoad = new RoadCtm(1,-1,-1,-1,"small",paramsCtm);
		roads.put(1, outRoad);
		test = false;
		try {
			new network(intersections,roads);
		} catch(IllegalArgumentException exception) {
			System.out.println(exception.getMessage());
			test = true;
		}
		assertTrue(test);
		
		
		// test rocade
		// sec 1
		network section1 = new network("section1");
		System.out.println("\n");
		section1.printNetwork();
		// sec 2
		network section2 = new network("section2");
		System.out.println("\n");
		section2.printNetwork();
		// sec 3
		network section3 = new network("section3");
		System.out.println("\n");
		section3.printNetwork();
		// sec 4
		network section4 = new network("section4");
		System.out.println("\n");
		section4.printNetwork();
		// sec 5
		network section5 = new network("section5");
		System.out.println("\n");
		section5.printNetwork();
		
		// test lookup tables
		// use "section4", since it is sufficiently "intersesting"
		System.out.println(Arrays.toString(section4.sensor2road.entrySet().toArray()));
		// check sensors-roads
		assertEquals((Integer) 15, section4.sensor2road.get(4057));
		assertEquals((Integer) 16, section4.sensor2road.get(4166));
		assertEquals((Integer) 115, section4.sensor2road.get(1650));
		assertEquals((Integer) 113, section4.sensor2road.get(1658));
		// check actuators-intersections
		assertEquals((Integer) 13, section4.actuator2intersection.get(4486));
		assertEquals((Integer) 15, section4.actuator2intersection.get(4453));
		
		
	}


}
