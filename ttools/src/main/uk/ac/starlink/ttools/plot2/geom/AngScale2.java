package uk.ac.starlink.ttools.plot2.geom;

import skyview.data.AngScale;

/** Find an appropriate delta to use for getting grid intervals.
 *
 * <p>This subclasses the SkyView AngScale class in order to modify
 * its behaviour slightly.  The original implementation is copied from the
 * original skyview source.
 *
 * <p>The changes consist of addding some larger values to the
 * <code>*Bigs[]</code> arrays, so that lower crowding values
 * actually yield more sparse grids.  That includes values that
 * correspond to no grid lines at all (180/360 degrees for lat/lon).
 *
 * @author   Tom McGlynn
 * @author   Mark Taylor
 */
public class AngScale2 extends AngScale {
   
    private final double deciBigs[] = {
	360, 180, 120, 60, 30, 15, 5, 3, 1 
    };
    
    private final double deciSmalls[] = {
	0.5, 0.2, 0.1
    };
    
    private final double sexaBigs[]   = {
	360, 180, 120, 60, 30, 15, 5, 2, 1, 
	30/60., 20/60., 10/60., 5/60., 2/60., 1/60.,
	30/3600., 20/3600., 10/3600., 5/3600., 2/3600., 1/3600.
    };
    
    private final double timeBigs[]   = {
	24, 12, 6, 3, 2, 1, 
	30/60., 20/60., 10/60., 5/60., 2/60., 1/60.,
	30/3600., 20/3600., 10/3600., 5/3600., 2/3600., 1/3600.
    };
    
    private final double sexaSmalls[] = { 
	0.5/3600., 0.2/3600., 0.1/3600.
    };
    
    private final double minDivs = 3;
    
    private boolean sexagesimal = true;
    private boolean time        = false;
	  
    
    /** Get an appropriate scaling for this coordinate. 
     *  @param delta       The range of the coordinate.
     */
    public double scale(double delta) {
	
	double[] bigs, smalls;
	
	if (delta == 0) {
	    return 0;
	}
	
	delta = Math.abs(delta);
	
	if (time) {
	    bigs   = timeBigs;
	    smalls = sexaSmalls;
	} else if (sexagesimal) {
	    bigs   = sexaBigs;
	    smalls = sexaSmalls;
	} else {
	    bigs   = deciBigs;
	    smalls = deciSmalls;
	}
	
	for (int i=0; i<bigs.length; i += 1) {
	    if (delta/bigs[i] > minDivs) {
		return bigs[i];
	    }
	}
	
	double tens = 1;
	while (tens<1.e10) {
	    for (int i=0; i<smalls.length; i += 1) {
		double div = smalls[i]/tens;
		if (delta/div > minDivs) {
		    return div;
		}
	    }
	    tens *= 10;
	}
	return 0;
    }
    
    /** Get the desired starting values and scaling interval.
     *  @param  min  The minimum coordinate value.
     *  @param  max  The maximum coordinate value
     *  @return a two element vector giving the starting value and delta
     *           to be used.  .
     */
    
    public double[] scaling(double min, double max) {
	
	if (min == max) {
	    return null;
	}
	
	double delt = max-min;
	
	if (min > max) {
	    if (!time) {
	        delt += 360;
	    } else {
		delt += 24;
	    }
	}
	
	delt = scale(delt);
	if (min > 0) {
	    min = min - min%delt + delt;
	} else {
	    min = min - (min%delt) ;
	}
	  
	return new double[]{min,delt};
    }
    
    
    /** Do we want sexagesimal coordinates? */
    public void setSexagesimal(boolean flag) {
	sexagesimal = flag;
    }
    
    /** Do we want coordinates in time? */
    public void setTime(boolean flag) {
	time = flag;
    }
}
