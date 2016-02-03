package org.speedd.dm;

class onrampStruct {
	protected Double maxFlow;
	protected Double minFlow;
	protected int operationMode;
	protected double dutycycle;
	protected int upstreamRamp;
	protected int ramp;
	protected int downstreamRamp;
	protected int actuatorId;

	public onrampStruct(Integer intersectionId, Integer actuatorId) {
		this.operationMode = 0;
		this.dutycycle = 1.;
		this.maxFlow = Double.POSITIVE_INFINITY;
		this.minFlow = .0;
		this.upstreamRamp = -1;
		this.ramp = intersectionId;
		this.downstreamRamp = -1;
		this.actuatorId = actuatorId;
	}


}
