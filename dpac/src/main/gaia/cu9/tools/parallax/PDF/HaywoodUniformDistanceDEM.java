package gaia.cu9.tools.parallax.PDF;

import gaia.cu9.tools.parallax.datamodel.StarVariables;

public class HaywoodUniformDistanceDEM extends UniformDistanceDEM {


	public HaywoodUniformDistanceDEM(StarVariables star, double rLim) {
		this(star.getVarpi(), star.getErrVarpi(), rLim);
	}

	public HaywoodUniformDistanceDEM(double varPi, double sigma, double rLim) {
		super(HaywoodSmithTransformation.getCorrectedParallax(varPi, sigma), 
				HaywoodSmithTransformation.getCorrectedSigma(varPi, sigma), 
			  rLim);
	}


	
}
