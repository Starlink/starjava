package gaia.cu9.tools.parallax.readers;

import java.util.List;

import gaia.cu9.tools.parallax.datamodel.StarVariables;

public class StarVariablesCsvReader extends CsvReader<StarVariables> {
	
	public StarVariablesCsvReader(StarVariablesCsvSchema schema){
		super(schema);
	}

	protected StarVariables buildObject(long entryNumber, List<String> tokens) {
		StarVariables star = new StarVariables(
				getLong(StarVariablesCsvSchema.SOURCE_ID, tokens, currentEntryNumber),
				getDouble(StarVariablesCsvSchema.ALPHA, tokens, -1), 
				getDouble(StarVariablesCsvSchema.DELTA, tokens, -1), 
				0, 
				0, 
				getDouble(StarVariablesCsvSchema.VARPI, tokens, -1),
				getDouble(StarVariablesCsvSchema.SIGMA_VARPI, tokens, -1));

		// Only used in tests on the performace of the estimator
		double realR = getDouble(StarVariablesCsvSchema.REAL_R, tokens, -1);
		if (realR >= 0) {
			star.setRealR(realR * schema.getDistanceMultiplier());
		} else {
			double realVarPi = getDouble(StarVariablesCsvSchema.REAL_VARPI, tokens, -1);
			if (realVarPi >= 0) {
				star.setRealR(schema.getDistanceMultiplier() / realVarPi);
			}
		}

		return star;
	}

}
