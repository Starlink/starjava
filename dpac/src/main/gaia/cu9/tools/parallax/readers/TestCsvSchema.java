package gaia.cu9.tools.parallax.readers;

import java.util.HashMap;
import java.util.Map;

public class TestCsvSchema extends StarVariablesCsvSchema{

	@Override
	public Map<String, Integer> setupColumnIndexes(){
		Map<String, Integer> columnIndexes = new HashMap<String, Integer>();
		columnIndexes.put(ALPHA, 4);
		columnIndexes.put(DELTA, 5);
		columnIndexes.put(VARPI, 6);
		columnIndexes.put(SIGMA_VARPI, 10);
		
		columnIndexes.put(REAL_VARPI, 2);
		
		return columnIndexes;
	}
	
	@Override
	protected double setupDistanceMultiplier(){
		return 1000.0;
	}
	
}
