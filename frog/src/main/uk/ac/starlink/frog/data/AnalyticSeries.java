package uk.ac.starlink.frog.data;

/**
 * AnalyticSeries is an interface for objects that
 * themselves using an analytic or semi-analytic function or
 * procedure. Expected uses include interpolation of the time 
 * series and folding around a supplied ephemeris.
 *
 * @since $Date$
 * @since 14-DEC-2000
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 */
public interface AnalyticSeries
{
    /**
     *  Return the value of the time series at a specific time.
     */
    public double evalYData( double x );

    /**
     *  Return an array of values at a number of specified times.
     */
    public double[] evalYDataArray( double[] x );
}

