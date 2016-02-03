package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class IntersectionFifo extends Intersection {
    public Fifo params;

    /**
     * Default constructor: All internal variables of the superclass
     * "Intersection" are provided as parameters. In addition, the "FIFO"
     * parameters are provided.
     *
     * @param  id
     * @param  roads_in_ID	list of IDs of incoming roads
     * @param  roads_out_ID	list of IDs of outgoing roads
     * @param  actuator_ID	ID of AIMSUN actuator associated with this
     * 						intersection (traffic light). May be null.
     * @param  params		parameters describing the dynamic behavior
     */ 
    public IntersectionFifo(int id, int[] roads_in_ID, int[] roads_out_ID, int actuator_ID, Fifo params) {
        this.id = id;
        this.roads_in_ID = roads_in_ID;
        this.roads_out_ID = roads_out_ID;
        this.ActuatorId = actuator_ID;
        this.params = params;
        this.nTLP = this.params.TLP.length;
        this.defaultAction = new Double[this.nTLP];
        for (int ii=0;ii<this.nTLP;ii++) {
        	this.defaultAction[ii] = 1./this.nTLP;
        }
        this.activeAction = this.defaultAction;
    }
    
    /**
     * Alternative constructor, default action is chosen as well.
     *
     * @param  id
     * @param  roads_in_ID	list of IDs of incoming roads
     * @param  roads_out_ID	list of IDs of outgoing roads
     * @param  actuator_ID	ID of AIMSUN actuator associated with this
     * 						intersection (traffic light). May be null.
     * @param  params		parameters describing the dynamic behavior
     */ 
    public IntersectionFifo(int id, int[] roads_in_ID, int[] roads_out_ID, int actuator_ID, Fifo params, Double[] defaultAction) {
        this.id = id;
        this.roads_in_ID = roads_in_ID;
        this.roads_out_ID = roads_out_ID;
        this.ActuatorId = actuator_ID;
        this.params = params;
        this.nTLP = this.params.TLP.length;
        this.defaultAction = defaultAction;
        this.activeAction = this.defaultAction;
    }
	
    /**
     * Computes the flows across intersection for one traffic light cycle of 
     * length 'dt'. Relies on the traffic light phases provided as a
     * parameter and on the internal state of the adjacent roads.
     *
     * @param  iTLP		traffic light phases for this intersection
     * @param  dt		simulation time
     * @return 			map relating IDs of adjacent roads to the corresponding
     * 					flows
     */ 
    public Map<Integer,Double> computeFlows(Double[] iTLP, double dt) {
    	
    	// get some dimensions
    	int n_in = roads_in.length;
    	int n_out = roads_out.length;
    	
    	// load traffic demand 
    	double[] demand = new double[n_in];
    	for(int ii=0;ii<n_in;ii++) {
    		demand[ii] = this.roads_in[ii].getDemand();
    	}
    	// load traffic supply
    	double[] supply = new double[n_out];
    	for(int ii=0;ii<n_out;ii++) {
    		if (this.roads_out[ii] == null) {
    			// flow leaves the network
    			supply[ii] = 100000; // FIXME: this is really infinity...
    		}
    		else {
    			supply[ii] = this.roads_out[ii].getSupply();
    		}
    	}
    	
    	// compute "average" traffic signal
    	// aTLP = "average traffic light signal" := sum_{phases} iTLF{ii} * TLP[ii]
    	// assumption: sum_{ii} iTLF := 1
    	RealMatrix aTLP = MatrixUtils.createRealMatrix(this.params.TLP[0]);
    	aTLP = aTLP.scalarMultiply(iTLP[0]); // initialize with first traffic light phase
    	for(int ii=1;ii<this.params.TLP.length;ii++) {
    		RealMatrix buffer = MatrixUtils.createRealMatrix(this.params.TLP[ii]);
    		aTLP = aTLP.add(buffer.scalarMultiply(iTLP[ii])); // add all other traffic light phases
    	}
    	
    	// compute demand satisfaction according to FIFO rule
    	RealVector demand2 = MatrixUtils.createRealVector(demand);
    	RealVector d = aTLP.transpose().preMultiply(demand2);									// Matlab: d_s = aTLP*demand; FIXME: use "operate" instead
    	RealVector kappa_supply = MatrixUtils.createRealVector(supply).ebeDivide(d);			// Matlab: kappa_supply = s./d_s;
    	
    	double[] kappa_demand = new double[n_in]; // allocate
    	for(int ii=0;ii<n_in;ii++) {
    		kappa_demand[ii] = 1;
    		for(int jj=0;jj<n_out;jj++) {
    			if(aTLP.getData()[jj][ii] > 0) {
    				kappa_demand[ii] = Math.min(kappa_demand[ii], kappa_supply.toArray()[jj]); 	// FIFO: smallest downstream demand satisfaction determines flow
    			}
    		}
    	}
    	
    	RealVector kappa_demand2 = MatrixUtils.createRealVector(kappa_demand);

    	RealVector allones = MatrixUtils.createRealVector(new double[n_out]); allones.set(1.); 	// Matlab: allones = ones(n_out,1);
    	RealVector scaling = aTLP.preMultiply(allones); 										// Matlab: scaling = ones(1,n_out)*aTLP;
    	
    	RealVector fin = kappa_demand2.ebeMultiply(demand2).ebeMultiply(scaling); 				// Matlab: fin2 = kappa_demand2 .* demand2 .* scaling;
    	
    	RealMatrix TPM = MatrixUtils.createRealMatrix(this.params.TPM);
    	RealVector fout = TPM.transpose().preMultiply(fin); 									// Matlab: fout = TPM*fin;
    	
    	Map<Integer,Double> flows = new HashMap<Integer,Double>();
    	
    	// set flows on the individual roads
    	for(int ii=0;ii<n_in;ii++) {
    		double ncars_out = dt * fin.getEntry(ii); // NOTE: 'fin' = flow into intersection ~ 'n_out' = cars leaving the road
    		flows.put(-this.roads_in_ID[ii], ncars_out); // negative --> cars leaving the road
    	}
    	for(int ii=0;ii<n_out;ii++) {
    		if (this.roads_out[ii] == null) {
    			// flow leaves the network - do nothing?
    		}
    		else {
    			double ncars_in = dt * fout.getEntry(ii); // NOTE: 'fout' = flow out of the intersection ~ 'n_in' = cars entering a road
        		flows.put(this.roads_out_ID[ii], ncars_in); // positive --> cars entering the road
    		}
    	}
    	
    	return flows;
    }
    
}
