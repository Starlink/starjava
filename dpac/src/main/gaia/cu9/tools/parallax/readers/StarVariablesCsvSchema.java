package gaia.cu9.tools.parallax.readers;

import gaia.cu9.tools.parallax.datamodel.StarVariables;

public abstract class StarVariablesCsvSchema extends CsvSchema<StarVariables>{
	
	protected static final String SOURCE_ID = "sourceId";
	protected static final String ALPHA = "alpha";
	protected static final String DELTA = "delta";
	protected static final String B = "BDeg";
	protected static final String L = "LDeg";
	protected static final String VARPI = "varPi";
	protected static final String SIGMA_ALPHA = "alphaErr";
	protected static final String SIGMA_DELTA = "deltaErr";
	protected static final String SIGMA_VARPI = "varPiErr";
	
	// used for tests with simulated data
	protected static final String REAL_R = "realR";
	protected static final String REAL_VARPI = "realVarPi";
}
