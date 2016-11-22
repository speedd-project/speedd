package org.speedd.dm;

import static org.junit.Assert.*;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Test;

public class TestGP {

	@Test
	public void test() {
		
		// create GP data
		int n = 18;
		double rho_step = 10;
		double v = 100;
		double w = 25;
		double rhobar = 250;
		
		// create trainings data
		double[][] data = new double[n][1];
		double[] targets = new double[n];
		for (int ii=0; ii<n; ii++) {
			double rho = ii*rho_step;
			data[ii] = new double[1];
			data[ii][0] = rho;
			targets[ii] = Math.min(rho*v,(rhobar-rho)*w);
		}
		// put training data in right format
		RealVector trainings_targets = new ArrayRealVector(targets);
		RealVector[] trainings_data = new ArrayRealVector[targets.length];
		for (int ii=0; ii<trainings_data.length; ii++) {
			trainings_data[ii] = new ArrayRealVector( data[ii] );
		}
		
		// create and test GP
		double sigma_v = 500.;
		double sigma_n = 200.;
		double[] scales = {500.};
		GaussianProcess myGP = new GaussianProcess(trainings_data, trainings_targets, scales, sigma_n, sigma_v);

		// make predictions
		RealVector xt;
		double[][] testData = data;
		
		for (int jj=0; jj<testData.length; jj++) {
			xt = new ArrayRealVector(testData[jj]);
			double[] buffer = myGP.predict(xt);
			System.out.println("Mean: " + buffer[0] + ", Variance: " + buffer[1]);
		}
		
		// same result as MATLAB!

		
	}

}
