package org.speedd.dm;

public class GrenobleTopology {
	
	private final static int N = 21;

	// Convention for sensor labels:
	// sens_in : sensor on the mainline, flow INto a cell
	// sens_ou : sensor on the mainline, flow OUt of a cell
	// sens_qu : sensor on beginning of a QUeue
	// sens_on : sensor on an ONramp entering the freeway
	// sens_of : sensor on an OFframp
	// index             		      1    x2     3     4     5     6    x7     8     9    10   x11    12    13   x14    15   x16     17   18   x19    20    21
	// road parameters   			  1     2     3     4     5     6     7     8     9    10    11   11o    12    13    14    15    15o   16    17    18    19
	final static int[] sens_in = {   -1, 4087,   -1,   -1,   -1,   -1, 3812,   -1,   -1,   -1, 4061,   -1,   -1, 4375,   -1,   -1,   -1,   -1, 4055,   -1,   -1 };
	final static int[] sens_ou = {   -1, 4084,   -1,   -1, 3813,   -1, 3811,   -1, 4355,   -1, 4381,   -1,   -1, 4058,   -1, 4056,   -1,   -1, 4054,   -1, 4052 }; 
	final static int[] sens_me = {   -1, 4244,   -1,   -1,   -1,   -1, 3810,   -1,   -1,   -1, 4391,   -1,   -1, 4057,   -1, 4166,   -1,   -1, 4053,   -1,   -1 };   
	
	final static int[] sens_qu = {   -1, 4085,   -1,   -1,  - 1,   -1, 4132,   -1,   -1,   -1, 4134,   -1,   -1, 4135,   -1, 4136,   -1,   -1, 4138,   -1,   -1 };
	final static int[] sens_on = { 1708, 1703,   -1,   -1, 1687,   -1, 1679, 1675,   -1,   -1, 1666,   -1,   -1, 1658,   -1, 1650,   -1,   -1, 1642,   -1, 1634 };
	final static int[] sens_of = {   -1,   -1,   -1, 1691,   -1, 1683,   -1,   -1,   -1, 1670,   -1,   -1, 1662,   -1, 1654,   -1,   -1, 1646,   -1, 1638,   -1 };
	final static int[] actu_id = {   -1, 4489,   -1,   -1,   -1,   -1, 4488,   -1,   -1,   -1, 4487,   -1,   -1, 4486,   -1, 4453,   -1,   -1, 4490,   -1,   -1 };
	// 												 1			 2			 3    		 4			 5			 6 			 7			 8			 9		 	10
	private final static String[] part_dm = {"section1", "section1", "section1", "section2", "section2", "section2", "section2", "section2", "section2", "section3", 
		// 											11			12			13    		14			15			16 			17			18			19		 	20
											 "section3",  "section4", "section4", "section4", "section4", "section5", "section5", "section5", "section5", "section5"};

	// road parameters   			  1     2     3     4     5     6     7     8     9    10    11   11o    12    13    14    15    15o   16    17    18    19
	// index             		      1    x2     3     4     5     6    x7     8     9    10   x11    12    13   x14    15   x16     17   18   x19    20    21
	final static double[] dF   = {  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,  0.07,   0.07 }; //conservative estimate
	// final static double[] length={  2.0,  0.4,  0.8,  0.4,  0.4,  0.8,  0.5, 0.45, 0.75,  1.3,  0.5,  0.4,  0.5,  0.5, 0.65,  0.4,  0.4,  0.4, 0.55, 0.45,  0.4 }; // google maps
	final static double[] length={  2.0,  0.8,  0.8,  0.4,  0.4,  0.8, 0.25, 0.45, 0.75,  1.3,  0.5,  0.4,  0.5,  0.7, 0.65,  0.4,  0.4,  0.4, 0.8, 0.45,  0.4 }; // google maps, adjusted for AIMSUN
	final static double[] v    = {  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90.,  90. }; // speed limit
	final static double[] rhom = { 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 250., 1.5*250. }; // default value
	// index             		      1    x2     3     4     5     6    x7     8     9    10   x11    12    13   x14    15   x16     17   18   x19    20    21
	final static double[] beta = {   0.,   0.,   0., 0.10,   0., 0.18,   0.,   0.,   0., 0.11,   0.,   0., 0.10,   0., 0.16,   0.,   0., 0.10,   0., 0.08,   0. }; // estimated from data
	final static double[] rhoc = { 49.0, 59.6, 55.0, 55.0, 47.9, 47.9, 52.0, 55.0, 51.1, 54.2, 48.0, 48.0, 51.6, 49.5, 54.7, 51.2, 51.2, 56.1, 56.1, 56.1, 1.5*56.1 }; // estimated from data
	final static double[] ql   = {  0.1,  0.4,   0.,   0., 0.12,   0., 0.15, 0.12,   0.,   0.,  0.3,    0,   0.,  0.2,   0.,  0.2,    0.,   0.,  0.2,   0., 0.06}; // google maps
	
	// final static double[] ql   = {  0.1,  0.4,   0.,   0., 0.12,   0.,  0.4, 0.12,   0.,   0.,  0.4,    0,   0.,  0.4,   0.,  0.4,   0.,   0.,  0.4,   0., 0.06 }; // google maps, extended assumption
	// final static double[] rhom = { 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 2500., 1.5*2500. }; // default value
	// final static double[] rhoc = {  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500.,  500. };
	// final static double[] dF   = {   0.,0.071,   0.,   0.,   0.,   0.,0.153,   0.,   0.,   0.,0.073,   0.,   0.,0.073,   0.,0.065,   0.,   0.,0.065,   0.,   0. }; // NOTE: capacity drop only estimated for metered onramp sections
	// final static double[] dF   = {  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,  0.1,   0.1 }; // ONLY for testing purposes!!
	
	// convenction     ---- CELL X-1 ---> ( LOCATION X ) ---- CELL X ---> ( LOCATION X+1 ) ---- CELL X+1 ---> ....
	
	/**
	 * Get ctm structure for given cell index.
	 * @param k	cell index
	 * @return	the corresponding ctm struct
	 */
	public static Ctm getCtm(int k) {
		return new Ctm(v[k], rhoc[k], rhom[k], length[k], dF[k]);
	}
	
	/**
	 * Get queue length in km for given cell index
	 * @param k	cell index
	 * @return	the queue length in km
	 */
	public static double getQueue(int k) {
		if ((k >= 0) && (k < N )) {
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
		return part_dm[k];
	}
	
	/**
	 * Get ID table for given cell index
	 * 
	 * @param k	cell index
	 * @return	the ID table
	 */
	public static FreewayCell_IdTable get_id_table(int k) {
		return new FreewayCell_IdTable(sens_in[k], sens_ou[k], sens_me[k], sens_qu[k], sens_on[k], sens_of[k], actu_id[k]);
	}
	
	/**
	 * Get index of next upstream ramp.
	 * @param id
	 * @return
	 */
	public static int get_upstream_ramp(int downstream_index) {
		int k = GrenobleTopology.get_index(downstream_index);
		int upstream_index = -1;
		for (int ii=0; ii<k; ii++) {
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
				return ii;
			} else if (id == sens_ou[ii]) {
				return ii;
			} else if (id == sens_qu[ii]) {
				return ii; // index shift
			} else if (id == sens_on[ii]) {
				return ii; // index shift
			} else if (id == sens_me[ii]) {
				return ii; // index shift
			} else if (id == sens_of[ii]) {
				return ii; // index shift
			} else if (id == actu_id[ii]) {
				return ii; // index shift
			}
		}
		
		// not found
		return -1;
	}
						
	
}
