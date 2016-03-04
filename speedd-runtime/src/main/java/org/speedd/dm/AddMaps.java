package org.speedd.dm;

import java.util.Map;

public class AddMaps {

	public static Map<Integer,Double> add(Map<Integer,Double> map1, Map<Integer,Double> map2) {
		Map<Integer,Double> map = map2;
		for (Map.Entry<Integer,Double> entry : map1.entrySet()) {
			Integer key = entry.getKey();
			Double value1 = entry.getValue();

			Double value2 = map2.get(key);
			
			if (value2 != null) {
				map.put(key,value1+value2);
			} else {
				map.put(key, value1);
			}
		}
		return map;
	}

	public static void print(Map<Integer,Double> map) {
		System.out.println(" ");
		for (Map.Entry<Integer,Double> entry : map.entrySet()) {
			System.out.println("Key: " + entry.getKey() + " Value: " + entry.getValue());
		}
	}
}
