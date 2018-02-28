package gaia.cu9.tools.parallax.readers;

import java.util.HashMap;
import java.util.Map;

public class Test2CsvSchema extends StarVariablesCsvSchema{

	@Override
	public Map<String, Integer> setupColumnIndexes(){
		Map<String, Integer> columnIndexes = new HashMap<String, Integer>();
		columnIndexes.put(SOURCE_ID, 0);
		columnIndexes.put(ALPHA, 1);
		columnIndexes.put(DELTA, 2);
		columnIndexes.put(VARPI, 6);
		columnIndexes.put(SIGMA_VARPI, 7);
		
		columnIndexes.put(REAL_R, 18);
		
		return columnIndexes;
	}
	
}
