package gaia.cu9.tools.parallax.PDF;

import gaia.cu9.tools.parallax.datamodel.StarVariables;

public class LosExpDecrVolumeDensityDEM extends ExpDecrVolumeDensityDEM{

	protected static double[][] scaleMatrix = null;
	
	protected static void loadScaleMatrix(String filename){
		scaleMatrix = new double[180][360];
		// TODO: load from file
	}
	
	
	public LosExpDecrVolumeDensityDEM(StarVariables star) {
		super(star, getScale(star));
	}

	protected static double getScale(StarVariables star){
		return getScale(star.getBdeg(), star.getLdeg());
	}
	
	protected static double getScale(double b, double l){
		// Returns closest match in the table
		int bmod = ((int)Math.round(b + 90))%180;
		int lmod = ((int)Math.round(l))%360;

		return scaleMatrix[bmod][lmod];
	}
	

}
