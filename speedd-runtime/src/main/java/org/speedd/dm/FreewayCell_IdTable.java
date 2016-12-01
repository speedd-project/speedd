package org.speedd.dm;

public class FreewayCell_IdTable {	
	// Data
	public final int sens_in;
	public final int sens_ou;
	public final int sens_me;
	public final int sens_qu;
	public final int sens_on;
	public final int sens_of;
	public final int actu_id;
	
	/**
	 * Default constructor
	 * 
	 * @param sens_in	ID of sensor at the beginning of the mainline of the cell
	 * @param sens_ou	ID of sensor at the end of the mainline of the cell
	 * @param sens_qu	ID of sensor at the beginning of the onramp adjacent to the cell
	 * @param sens_on	ID of sensor at the end of the onramp adjacent to the cell
	 * @param sens_of	ID of sensor at the offramp adjacent to the cell
	 * @param actu_id	ID of ramp metering actuator at the cell
	 */
	public FreewayCell_IdTable(int sens_in, int sens_ou, int sens_me, int sens_qu, int sens_on, int sens_of, int actu_id) {
		this.sens_in = sens_in;
		this.sens_ou = sens_ou;
		this.sens_me = sens_me;
		this.sens_qu = sens_qu;
		this.sens_on = sens_on;
		this.sens_of = sens_of;
		this.actu_id = actu_id;
	}

}
