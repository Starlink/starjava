package skyview.data;

/** Find an appropriate delta to use for getting grid intervals.
 */
public class AngScale {
   
    private final double deciBigs[] = {
	120, 60, 30, 15, 5, 3, 1 
    };
    
    private final double deciSmalls[] = {
	0.5, 0.2, 0.1
    };
    
    private final double sexaBigs[]   = {
	120, 60, 30, 15, 5, 2, 1, 
	30/60., 20/60., 10/60., 5/60., 2/60., 1/60.,
	30/3600., 20/3600., 10/3600., 5/3600., 2/3600., 1/3600.
    };
    
    private final double timeBigs[]   = {
	6, 3, 2, 1, 
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
     *  @param  sexagesimal  Do we want sexagesimal coordinates.
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
       
    public static void main(String[] args) {
	
	double min = Double.parseDouble(args[0]);
	double max = Double.parseDouble(args[1]);
	AngScale ang = new AngScale();
	ang.setSexagesimal(args.length == 2);
	double[] s = ang.scaling(min,max);
	System.out.println("Starting value: "+s[0]+", Delta: "+s[1]);
	
    }
		      
	
}
