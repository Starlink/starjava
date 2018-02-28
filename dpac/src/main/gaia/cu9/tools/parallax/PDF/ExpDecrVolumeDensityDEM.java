package gaia.cu9.tools.parallax.PDF;

import gaia.cu9.tools.parallax.datamodel.StarVariables;
import gaia.cu9.tools.parallax.util.PolinomialSolver;

public class ExpDecrVolumeDensityDEM extends DistanceEstimationMethod {
	private final double varpi;
	private final double sigma;
	private final double L;

	public ExpDecrVolumeDensityDEM(double varPi, double sigma, double scaleLength){
		this.varpi = varPi;
		this.sigma = sigma;
		this.L = scaleLength;
	}
	
	public ExpDecrVolumeDensityDEM(StarVariables star, double scaleLength){
		this(star.getVarpi(), star.getErrVarpi(), scaleLength);
	}
	
	
	@Override
	protected PDF createDistancePdf(){
		PDF distancePDF =  new PDF(){
			@Override
			public double getUnnormalizedProbabilityAt(double r) {
				return (r > 0) ? Math.exp(-0.5*(varpi - 1./r)*(varpi - 1./r)/(sigma*sigma) - r/L) * r*r : 0;
			}
			
			@Override
			public double getBestEstimation(){
				double rMode = Double.NaN;

				double a2 = -2.*L;
				double a1 = varpi*L/(sigma*sigma);
				double a0 = -L/(sigma*sigma);
				

				return PolinomialSolver.solveThirdDegree(a2, a1, a0);
			}
		};
		return distancePDF;
	}
	
	@Override
	protected PDF createModulusPdf(){
		PDF modulusPDF = new PDF(){
			
			@Override
			public double getUnnormalizedProbabilityAt(double r) {
				return r * ExpDecrVolumeDensityDEM.this.getDistancePDF().getUnnormalizedProbabilityAt(r);
			}
			
			@Override
			public double getBestEstimation(){
				double a2 = -3.*L;
				double a1 = varpi*L/(sigma*sigma);
				double a0 = -L/(sigma*sigma);
				
				return PolinomialSolver.solveThirdDegree(a2, a1, a0);
			}
		};
		return modulusPDF;
	}
	
	

//	@Override
//	public double getUnnormalizedProbabilityAt(double r) {
//		return (r > 0) ? Math.exp(-0.5*(this.varpi - 1./r)*(this.varpi - 1./r)/this.sigma/this.sigma - r/this.L) * r*r/this.sigma : 0;
//	}
//	
//	@Override
//	public double getBestEstimation(){
//		double rMode = Double.NaN;
//
//		double a2 = -2.*this.L;
//		double a1 = this.varpi*this.L/this.sigma/this.sigma;
//		double a0 = -this.L/this.sigma/this.sigma;
//		
//
//		return PolinomialSolver.solveThirdDegree(a2, a1, a0);
//	}
	
	
//	@Override
//	public double getUnnormalizedModulusProbabilityAt(double r) {
//		return r * getUnnormalizedProbabilityAt(r);
//	}
//	
//	@Override
//	public double getModulusEstimation(){
//		double a2 = -3.*this.L;
//		double a1 = this.varpi*this.L/this.sigma/this.sigma;
//		double a0 = -this.L/this.sigma/this.sigma;
//		
//		return PolinomialSolver.solveThirdDegree(a2, a1, a0);
//	}
	
}
