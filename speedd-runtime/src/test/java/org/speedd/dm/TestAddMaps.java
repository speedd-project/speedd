package org.speedd.dm;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestAddMaps {

	@Test
	public void test() {
		
		Map<Integer,Double> map1 = new HashMap<Integer,Double>();
		Map<Integer,Double> map2 = new HashMap<Integer,Double>();
		
		Double val1 = 0.3;
		Double val2 = 0.5;
		
		map1.put(1,val1);
		map1.put(3,val1);
		map1.put(4,val1);
		
		map2.put(1,val2);
		map2.put(7,val2);
		map2.put(9,val2);
		
		Map<Integer,Double> map = AddMaps.add(map1, map2);
		
		assertEquals(map.get(0), null);
		assertEquals(map.get(1), val1+val2, 1e-6);
		assertEquals(map.get(2), null);
		assertEquals(map.get(3), val1, 1e-6);
		assertEquals(map.get(4), val1, 1e-6);
		assertEquals(map.get(5), null);
		assertEquals(map.get(6), null);
		assertEquals(map.get(7), val2, 1e-6);
		assertEquals(map.get(8), null);
		assertEquals(map.get(9), val2, 1e-6);
		assertEquals(map.get(10), null);
		
		
		
		

	} // end of public Test()


}
