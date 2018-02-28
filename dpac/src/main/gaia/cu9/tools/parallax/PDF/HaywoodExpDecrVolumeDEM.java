package gaia.cu9.tools.parallax.PDF;

import gaia.cu9.tools.parallax.datamodel.StarVariables;

public class HaywoodExpDecrVolumeDEM extends ExpDecrVolumeDensityDEM {

	public HaywoodExpDecrVolumeDEM(StarVariables star, double scaleFactor) {
		this(star.getVarpi(), star.getErrVarpi(), scaleFactor);
	}

	public HaywoodExpDecrVolumeDEM(double varPi, double sigma, double scaleFactor) {
		super(HaywoodSmithTransformation.getCorrectedParallax(varPi, sigma), 
				HaywoodSmithTransformation.getCorrectedSigma(varPi, sigma), 
			  scaleFactor);
	}

	
	
	
	

	
}
