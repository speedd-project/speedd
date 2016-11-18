package org.speedd.dm;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;


public class GaussianProcess {
	// data-dependent variables
	private RealVector[] trainings_data;
	private RealVector trainings_targets;
	private RealMatrix K;
	private DecompositionSolver solver_invK;
	private int n;					// NUMBER of data points
	
	// GP-specific hyperparameters, constant
	private final RealVector scales;
	private final double sigma_v2;
	private final double sigma_n2;
	private final int d;			// feature space DIMENSION

	/**
	 * Default constructor. Hyper-parameters need to be given at instantiation time
	 * @param trainings_data
	 * @param trainings_targets
	 * @param scales
	 * @param sigma_n
	 * @param sigma_v
	 */
	public GaussianProcess(RealVector[] trainings_data, RealVector trainings_targets, double[] scales, double sigma_n, double sigma_v) {
		// initialize hyperparameters
		this.sigma_n2 = sigma_n * sigma_n;
		this.sigma_v2 = sigma_v * sigma_v;
		this.scales = new ArrayRealVector(scales);
		this.d = scales.length;
		// set data
		newTrainingData(trainings_data, trainings_targets);
	}
	
	/**
	 * Set new trainings data and trainings targets. An error is thrown, if the
	 * dimensions do not match.
	 * @param trainings_data		the data ("features") as an array of Vectors
	 * @param trainings_targets		the dependent variables ("targets") as a Vector
	 */
	public void newTrainingData(RealVector[] trainings_data, RealVector trainings_targets) {
		// dimension checks
		if ((trainings_data.length != trainings_targets.getDimension()) || (trainings_data[0].getDimension() != this.d)) {
			// FIXME: throw error?
		}
		// set data
		this.n = trainings_targets.getDimension();
		this.trainings_data = trainings_data;
		this.trainings_targets = trainings_targets;
		
		compute_K();
	}
	
    /**
     * Evaluates the GP at test point xt and returns the predicted mean
     * and variance of the target variable.
     * @param  xt		test point (R^n)
     * @return 			double[] containing mean[0], model variance[1] and
     * 					noise[2]
     */ 
	public double[] predict(RealVector xt) {
		if (this.solver_invK == null) {
			compute_K();
		}
		RealVector kstar = compute_kstar(xt);
		
		double[] vals = new double[2];
		vals[0] = this.solver_invK.solve(kstar).dotProduct(this.trainings_targets);
		vals[1] = Math.sqrt( kernel(xt,xt)+this.sigma_n2 - this.solver_invK.solve(kstar).dotProduct(kstar) );
		
		return vals; 																			// Order: [mean, uncertainty, variance]		
	}
	
	/**
	 * Compute and invert the Kernel Matrix for a set of data points. Needs
	 * to be called each time after the data set is updated.
	 */
	private void compute_K() {
		RealMatrix K = new Array2DRowRealMatrix(this.n,this.n);
		for(int i=0; i<this.n; i++) {															// iterate over rows
			for(int j=0; j<this.n; j++) {														// iterate over columns
				double noise = 0;
				if (i==j) {
					noise = this.sigma_n2;
				}
				K.setEntry(i, j, kernel(trainings_data[i],trainings_data[j])+noise );
			}
		}
		this.K = K;

		this.solver_invK = new CholeskyDecomposition(this.K).getSolver();
	}
	
	/** 
	 * Computation of kstar according to Deisenroth et.al. (2015)
	 * @param xt		test point (R^n)
	 * @return			kstar (R^n)
	 */
	private RealVector compute_kstar(RealVector xt) {
		RealVector kstar = new ArrayRealVector(this.n);
		for(int i=0; i<this.n; i++) {															// iterate over vector entries
			kstar.setEntry(i, kernel(xt,trainings_data[i]));
		}
		return kstar;
	}
	
	/**
	 * Evaluation of the kernel function
	 * @param x1		first test point (R^n)
	 * @param x2		second test point (R^n)
	 * @return			kernel of both test points
	 */
	private double kernel(RealVector x1, RealVector x2) {
		double dist = (x1.subtract(x2).ebeDivide(this.scales)).dotProduct(x1.subtract(x2)); 	// dist = (x1-x2)' * diag(1/l) * (x1-x2)
		return this.sigma_v2 * Math.exp(- dist/2);												// k(x1,x2) = sigma_f^2 * exp(-1/2 * dist) + sigma_n^2
	}

}
