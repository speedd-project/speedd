package org.speedd.dm;

public class GrenobleTopology {

	// Convention for sensor labels:-
	// sens_in : sensor on the mainline, flow INto a cell
	// sens_ou : sensor on the mainline, flow OUt of a cell
	// sens_qu : sensor on beginning of a QUeue
	// sens_on : sensor on an ONramp entering the freeway
	// sens_of : sensor on an OFframp
	// road parameters   					  1     2     3     4     5     6     7     8     9    10    11    11o   12    13    14    15    15c   16    17    18    19    20
	private final static int[] sens_in = {   -1,   -1,   -1,   -1,   -1,   -1, 3812,   -1,   -1,   -1, 4061,       4391, 4375,   -1, 4057,       4166, 4055,   -1, 4053      };
	private final static int[] sens_ou = { 4087, 4084, 4244,   -1, 3813,   -1, 3811, 3810, 4355,   -1, 4381,         -1, 4058,   -1, 4056,         -1, 4054,   -1, 4052      };  
	private final static int[] sens_qu = {   -1, 4085,   -1,   -1,  - 1,   -1, 4132,   -1,   -1,   -1, 4134,         -1, 4135,   -1, 4136,         -1, 4138,   -1,   -1      };
	private final static int[] sens_on = { 1708, 1703,   -1,   -1, 1687,   -1, 1679, 1675,   -1,   -1, 1666,         -1, 1658,   -1, 1650,         -1, 1642,   -1, 1634      };
	private final static int[] sens_of = {   -1,   -1,   -1, 1691,   -1, 1683,   -1,   -1,   -1, 1670,   -1,       1662,   -1, 1654,   -1,       1646,   -1, 1638,   -1      };
	private final static int[] actu_id = {   -1, 4489,   -1,   -1,   -1,   -1, 4488,   -1,   -1,   -1, 4487,         -1, 4486,   -1, 4453,         -1, 4490,   -1,   -1      };
	// 												 1			 2			 3    		 4			 5			 6 			 7			 8			 9		 	10
	private final static String[] part_dm = {"section1", "section1", "section1", "section2", "section2", "section2", "section2", "section2", "section2", "section3", 
		// 											11			12			13    		14			15			16 			17			18			19		 	20
											 "section3",  "section4", "section4", "section4", "section4", "section5", "section5", "section5", "section5", "section5"};
	
	private final static double[] ql   = {  0.1,  0.4,  -1.,  -1., 0.12,  -1., 0.15, 0.12,  -1.,  -1.,  0.3,        -1.,  0.2,  -1.,  0.2,        -1.,  0.2,  -1., 0.06      };
	private final static double[] length={  0.5,  0.4,  0.8,  0.4,  0.4,  0.8,  0.5, 0.45, 0.75,  1.3,  0.5,        0.9,  0.5, 0.65, 0.55,       0.65, 0.55, 0.45,  0.4      };
	private final static double[] v    = {  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,        90.,  90.,  90.,  90.,        90.,  90.,  90.,  90.      };
	private final static double[] rhoc = {  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,  50.,        50.,  50.,  50.,  50.,        50.,  50.,  50.,  50.      };
	private final static double[] rhom = { 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250.,       250., 250., 250., 250.,       250., 250., 250., 250.      };
	// private final double[] beta = {   0.,   0.,   0.,  0.2,   0.,  0.2,   0.,   0.,   0.,  0.2,   0.,        0.2,   0.,  0.2,   0.,        0.2,   0.,  0.2,   0.      }; // currently unused
	
	/**
	 * Get ctm structure for given cell index.
	 * @param k	cell index
	 * @return	the corresponding ctm struct
	 */
	public static Ctm getCtm(int k) {
		k--; // array indexing convention
		return new Ctm(v[k], rhoc[k], rhom[k], length[k]);
	}
	
	/**
	 * Get queue length in km for given cell index
	 * @param k	cell index
	 * @return	the queue length in km
	 */
	public static double getQueue(int k) {
		k--;
		if ((k >= 0) && (k <= 18 )) {
			return ql[k];
		} else {
			return -1;
		}

	}
	
	/**
	 * Get the dmPartition attribute
	 * @param k	cell index
	 * @return	the queue length in km
	 */
	public static String get_dm_partition(int k) {
		k--;
		return part_dm[k];
	}
	
	/**
	 * Get ID table for given cell index
	 * 
	 * @param k	cell index
	 * @return	the ID table
	 */
	public static FreewayCell_IdTable get_id_table(int k) {
		k--;
		return new FreewayCell_IdTable(sens_in[k], sens_ou[k], sens_qu[k], sens_on[k], sens_of[k], actu_id[k]);
	}
	
	/**
	 * Get index of next upstream ramp.
	 * @param id
	 * @return
	 */
	public static int get_upstream_ramp(int downstream_index) {
		int k = GrenobleTopology.get_index(downstream_index);
		int upstream_index = -1;
		for (int ii=0; ii<k-1; ii++) {
			if (actu_id[ii] > 0) {
				upstream_index = actu_id[ii];
			}
		}
		return upstream_index;
	}
	
	/**
	 * Find cell index for given sensor or actuator ID.
	 * FIXME: Implement more efficiently e.g. with a Hash-Table
	 * 
	 * @param id	sensor or actuator id
	 * @return		the cell index
	 */
	public static int get_index(int id) {
		
		// index might be any sensor or actuator id
		for (int ii=0; ii<sens_in.length; ii++) {
			if (id == sens_in[ii]) {
				return ii+1; // index shift
			}
		}
		// index might be any sensor or actuator id
		for (int ii=0; ii<sens_ou.length; ii++) {
			if (id == sens_ou[ii]) {
				return ii+1; // index shift
			}
		}
		// index might be any sensor or actuator id
		for (int ii=0; ii<sens_qu.length; ii++) {
			if (id == sens_qu[ii]) {
				return ii+1; // index shift
			}
		}
		// index might be any sensor or actuator id
		for (int ii=0; ii<sens_on.length; ii++) {
			if (id == sens_on[ii]) {
				return ii+1; // index shift
			}
		}
		// index might be any sensor or actuator id
		for (int ii=0; ii<sens_of.length; ii++) {
			if (id == sens_of[ii]) {
				return ii+1; // index shift
			}
		}
		// index might be any sensor or actuator id
		for (int ii=0; ii<actu_id.length; ii++) {
			if (id == actu_id[ii]) {
				return ii+1; // index shift
			}
		}
		
		// not found
		return -1;
	}
						
	
}
