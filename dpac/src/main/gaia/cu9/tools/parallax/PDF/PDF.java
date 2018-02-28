package gaia.cu9.tools.parallax.PDF;

public interface PDF {
	/**
	 * Calculate the value of the PDF at point x
	 * @param x
	 * @return PDF(x)
	 */
	double getUnnormalizedProbabilityAt(double x);
	
	/**
	 * Calculate the best estimation
	 * @return best estimation value (e.g. mode of the probability distribution)
	 */
	double getBestEstimation();
	
	
}
