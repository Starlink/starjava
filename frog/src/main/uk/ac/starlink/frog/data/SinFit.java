package uk.ac.starlink.frog.data;

/**
 * The SinFit class is a wrapper object to hold an sin() + cos() fit
 * to a time series of the form 
 *
 *    Y = A + B*sin(2pi/period*X) + C*cos(2pi/period*X)
 *
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries, FoldSeriesDialog
 */
 
public class SinFit
{

    /**
     * a
     */
     protected double a;
 

    /**
     * b
     */
     protected double b;

    /**
     * c
     */
     protected double c;
     

    /**
     * period
     */
     protected double period; 
     
    /**
     * chi squared of this fit
     */
     protected double chiSq; 
  
   /**
     * The TimeSeriesComp object associated with the fit, i.e. the
     * time series of which this is a fit.
     */
    protected TimeSeriesComp timeSeriesComp = null;
   
        
    /**
     * Create an instance of the class.
     */
    public SinFit()
    {
    
     // do nothing 
     
    }
    
    /**
     * Get the chisq value
     */
    public double getChiSq()
    {
        return chiSq;
    }
    
    /**
     * Set the chisq value
     */
    public void setChiSq( double c )
    {
        chiSq = c;
    } 
            
    /**
     * Get the period value
     */
    public double getPeriod()
    {
        return period;
    }
    
    /**
     * Set the period value
     */
    public void setPeriod( double p )
    {
        period = p;
    } 
        
    /**
     * Get the "a" value
     */
    public double getA()
    {
        return a;
    } 
        
    /**
     * Get the "b" value
     */
    public double getB()
    {
        return b;
    }
             
    /**
     * Get the "c" value
     */
    public double getC()
    {
        return c;
    } 
        
    /**
     * Set the "a" value
     */
    public void setA( double aval)
    {
        a = aval;
    }
            
    /**
     * Set the "b" value
     */
    public void setB( double bval)
    {
        b = bval;
    }
            
    /**
     * Set the "c" value
     */
    public void setC( double cval)
    {
        c = cval;
    }
    
    /**
     * override the toString() method 
     *
     *    Y = A + B*sin(2pi/period*X) + C*cos(2pi/period*X)
     */
    public String toString()
    {
        return ( Math.round(a*100000.0)/100000.0 
                 + " + " + 
                 Math.round(b*100000.0)/100000.0 +
                 "sin((2pi/" + Math.round(period*100000.0)/100000.0 
                 + ")X) + " + Math.round(c*100000.0)/100000.0 + "cos((2pi/" + 
                 Math.round(period*100000.0)/100000.0 + ")X)" );
    }
 
   
   /**
     * Get the TimeSeriesComp object associated with Periodogram
     *
     * @return series The TimeSeriesComp
     * @see TimeSeriesComp
     */
    public TimeSeriesComp getTimeSeriesComp()
    {
        return timeSeriesComp;
    }
    
    
   /**
     * Set the TimeSeriesComp object associated with Periodogram
     *
     * @param series The TimeSeriesComp
     * @see TimeSeriesComp
     */
    public void setTimeSeriesComp( TimeSeriesComp t)
    {
        timeSeriesComp = t;
    }    
         
}
