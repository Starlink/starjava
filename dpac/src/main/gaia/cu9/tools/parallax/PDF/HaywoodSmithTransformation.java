package gaia.cu9.tools.parallax.PDF;

public class HaywoodSmithTransformation {
	
	// Parameter of the estimation, selected to this value in the original paper
	protected static final double BETA = 1.01;
	
	
	public static double getCorrectedParallax(double varPi, double sigma){
		double v_s = varPi/sigma;
		
		double g = (varPi>0 ? 1 : Math.exp(-0.605 * v_s * v_s ));
		double phi = Math.log(1 + Math.exp(0.8 * v_s))/0.8;
		
		double pseudoVarPi = BETA * sigma * phi  * g;
		
		return pseudoVarPi;
	}
	
	public static double getCorrectedSigma(double varPi, double sigma){
		double a = getCorrectedParallax(varPi-sigma, sigma);
		double b = getCorrectedParallax(varPi+sigma, sigma);
		
		double newSigma = (b-a)/2;
		
		return newSigma;
	}
}
