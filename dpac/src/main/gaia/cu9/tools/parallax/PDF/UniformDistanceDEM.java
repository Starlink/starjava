package gaia.cu9.tools.parallax.PDF;

import gaia.cu9.tools.parallax.datamodel.StarVariables;
import gaia.cu9.tools.parallax.util.Constants;
import gaia.cu9.tools.parallax.util.PolinomialSolver;

public class UniformDistanceDEM extends DistanceEstimationMethod {
	private final double varPi;
	private final double sigma;
	private final double rLim;
	
	public UniformDistanceDEM(StarVariables star, double rLim){
		this(star.getVarpi(), star.getErrVarpi(), rLim);
	}
	
	/**
	 * Initialize the uniform distance prior PDF. Inputs should be in
	 * consistent units, i.e. if parallax measurements are in MAS then
	 * distance should be in KPC.
	 * @param varPi
	 * @param sigma
	 * @param rLim
	 */
	public UniformDistanceDEM(double varPi, double sigma, double rLim){
		this.varPi = varPi;
		this.sigma = sigma;
		this.rLim  = rLim;

	}
	
	@Override
	protected PDF createDistancePdf(){
		PDF distancePDF =  new PDF(){
			@Override
			public double getUnnormalizedProbabilityAt(double x) {
				                               // Math.exp(-0.5*(varpi - 1./r)*(varpi - 1./r)/(sigma*sigma) - r/L) * r*r 
				return ((x > 0) && (x <= rLim)) ? Math.exp(-0.5*(varPi - 1./x)*(varPi - 1./x)/sigma/sigma) / sigma*rLim*Constants.SQRT_2PI : 0;
			}
			
			@Override
			public double getBestEstimation(){
				
				// For this distribution the best estimation is the mode,
				// truncated to a maximum value of rLim
				
				double result = 1./varPi;
				if ((result > 0) && (result <= rLim)){
					return result;
				} else {
					return rLim;
				}
			}
		};
		return distancePDF;
	}
	
	protected PDF createModulusPdf(){
		PDF modulusPDF = new PDF(){
			@Override
			public double getUnnormalizedProbabilityAt(double r) {
				PDF distancePDF = UniformDistanceDEM.this.getDistancePDF();
				return r * distancePDF.getUnnormalizedProbabilityAt(r);
			}
			
			@Override
			public double getBestEstimation(){

				double a1 = -varPi/(sigma*sigma);
				double a0 = 1/(sigma*sigma);
				
				return PolinomialSolver.solveSecondDegree(a1, a0);
			}
		};
		return modulusPDF;
	}
	
//	@Override
//	public double getUnnormalizedProbabilityAt(double x) {
//		return ((x > 0) && (x <= this.rLim)) ? Math.exp(-0.5*(this.varPi - 1./x)*(this.varPi - 1./x)/this.sigma/this.sigma) / this.sigma*this.rLim*Constants.SQRT_2PI : 0;
//	}
//	
//	@Override
//	public double getBestEstimation(){
//		
//		// For this distribution the best estimation is the mode,
//		// truncated to a maximum value of rLim
//		
//		double result = 1./this.varPi;
//		if ((result > 0) && (result <= this.rLim)){
//			return result;
//		} else {
//			return this.rLim;
//		}
//	}
	
//	@Override
//	public double getUnnormalizedModulusProbabilityAt(double r) {
//		return r * getUnnormalizedProbabilityAt(r);
//	}
//	
//	@Override
//	public double getModulusEstimation(){
//
//		double a1 = -this.varPi/this.sigma/this.sigma;
//		double a0 = 1/this.sigma/this.sigma;
//		
//		return PolinomialSolver.solveSecondDegree(a1, a0);
//	}

}
