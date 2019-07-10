package uk.ac.starlink.splat.util;

import jsky.util.Logger;

public class TimeUtilities {
	
	public static double MJD_Origin = 2400000.5;
	public static double JD_Origin = 0.0;
	
	private TimeUtilities() {
		
	}
	
	
	  /**
     * Check if the reference position and timescale combinnation are supported by AST, if not, issue a warning and 
     * use a default system. In AST, the reference position is implied by the timescale, being geocentre for TCG,
	 * barycentre for TDB, TCB, and topocentre for all others.
     * 
     * @return the chosen time system, or a default supported time system 
     */
    public static String getSupportedTimeScale(String refpos, String timescale) {
		
    	String message="";
    	
		if ("TCG".equalsIgnoreCase(timescale) && "GEOCENTER".equalsIgnoreCase(refpos)) {
			return timescale;
		}
		if ("TCB".equalsIgnoreCase(timescale) && "BARYCENTER".equalsIgnoreCase(refpos)) {
			return timescale;
		}
		if ("TDB".equalsIgnoreCase(timescale) && "BARYCENTER".equalsIgnoreCase(refpos)) {
			return timescale;
		}
		if (! "TOPOCENTER".equalsIgnoreCase(refpos)) {
			message = " Reference Position "+refpos+" not supported by AST. Using TOPOCENTER";
			Logger.warn( "TimeUtilities:",  message);
			refpos="TOPOCENTER";
		} 
		if ("GPS".equalsIgnoreCase(timescale)) { // GPS not supported by AST.
			message = " Timescale "+timescale+" not supported by AST. Using TAI";
			Logger.warn( "TimeUtilities:",  message);
			return "TAI";
		}
		if ("LOCAL".equalsIgnoreCase(timescale) ) { // local not supported by AST -->>> What to do? 
			message+=" Timescale "+timescale+" not supported by AST. Using LT";
			Logger.warn( "TimeUtilities:",  message);
			return "LT";
		}
		
		return timescale;		
		
	};
    

}
