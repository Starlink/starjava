package uk.ac.starlink.frog.data;

/**
 * The Ephemeris class is a wrapper object to hold an ephemeris sournd which
 * a time series has or can be fitted. Ephemeris is of the form:
 *
 *    BJD = ZERO + PERIOD x E
 *
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeries, FoldSeriesDialog
 */
 
public class Ephemeris
{

    /**
     * period
     */
     protected double period;
     
    /**
     * zero point
     */
     protected double zeroPoint;  


    /**
     * Create an instance of the class.
     */
    public Ephemeris()
    {
    
     // do nothing 
     
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
     * Get the zero point value
     */
    public double getZeroPoint()
    {
        return zeroPoint;
    } 
    
    /**
     * Set the zero point value
     */
    public void setZeroPoint( double z)
    {
        zeroPoint = z;
    }
    
    /**
     * override the toString() method
     */
    public String toString()
    {
        return ( zeroPoint + " + " + period + " x E " );
    }
     
}
