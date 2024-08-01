package gaia.cu9.tools.parallax.readers;

import java.util.Map;


public abstract class CsvSchema<T> {

	protected final String separators;
	protected final String skipLineMarker;
	protected final Map<String, Integer> columnIndexes;
	protected final int nLinesToDiscard;
	protected final double distanceMultiplier; // 1000 if in kpc, 1 if in pc

	/**
	 * Constructor
	 */
        @SuppressWarnings("this-escape")
	public CsvSchema(){
		columnIndexes=setupColumnIndexes();
		separators=setupSeparators();
		skipLineMarker=setupSkipLineMarker();
		nLinesToDiscard=setupNLinesToDiscard();
		distanceMultiplier = setupDistanceMultiplier();
	}
	
	/* protected methods that can be overridden for different csv-like formats */
	
	protected abstract Map<String, Integer> setupColumnIndexes();
	
	protected String setupSeparators(){
		return " ;,\t";
	}
	
	protected String setupSkipLineMarker(){
		return "#";
	}
	
	protected int setupNLinesToDiscard(){
		return 0;
	}
	
	protected double setupDistanceMultiplier(){
		return 1.0;
	}
	
	
	/* getters */
	
	public Integer getColumnIndex(String key){
		return columnIndexes.get(key);
	}
	
	public int getNLinesToDiscard(){
		return nLinesToDiscard;
	}
	
	public String getSeparators(){
		return separators;
	}
	
	public String getSkipLineMarker(){
		return skipLineMarker;
	}
	
	public double getDistanceMultiplier(){
		return distanceMultiplier;
	}
}
