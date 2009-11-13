/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     17-JUN-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import hep.aida.bin.DynamicBin1D;
import cern.colt.list.DoubleArrayList;

/**
 * Derive a useful set of statistics about an array of data values.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class Statistics
{
    /** The DynamicBin1D instance that does all the work */
    private DynamicBin1D bin1D = new DynamicBin1D();

    /**
     *  Create an instance.
     *
     *  @param data the array of values to processed.
     */
    public Statistics( double[] data )
    {
        setData( data );
    }

    /**
     * Set the data to be used.
     */
    public void setData( double[] data )
    {
        bin1D.clear();
        bin1D.addAllOfFromTo( new DoubleArrayList( data ), 0, data.length-1 );
    }

    /**
     * Get the number of values.
     */
    public int getNumberValues()
    {
        return bin1D.size();
    }

    /**
     * Get the maximum value.
     */
    public double getMaximum()
    {
        return bin1D.max();
    }

    /**
     * Get the minimum value.
     */
    public double getMinimum()
    {
        return bin1D.min();
    }

    /**
     * Get the sum.
     */
    public double getSum()
    {
        return bin1D.sum();
    }

    /**
     * Get the median.
     */
    public double getMedian()
    {
        return bin1D.median();
    }

    /**
     * Get the mode.
     */
    public double getMode()
    {
        return ( 3.0 * getMedian() ) - ( 2.0 * getMean() );
    }

    /**
     * Get the mean.
     */
    public double getMean()
    {
        return bin1D.mean();
    }

    /**
     * Get an alpha trimmed mean.
     *
     * @param fraction of values to remove from distribution, 0 to 0.5.
     */
    public double getAlphaTrimmedMean( double frac )
    {
        double size = (double) bin1D.size();
        int nremove = (int)( size * frac );
        return bin1D.trimmedMean( nremove, nremove );
    }

    /**
     * Get the sample standard deviation.
     */
    public double getStandardDeviation()
    {
        return bin1D.standardDeviation();
    }

    public String toString()
    {
        return super.toString() + ":\n" +
            "Maximum = " + getMaximum() + ", " +
            "Minimum = " + getMinimum() + ", " +
            "Sum = "     + getSum()     + ", " +
            "Median = "  + getMedian()  + ", " +
            "Mode = "    + getMode()    + ", " +
            "Mean = "    + getMean()    + ", " +
            "Std = "     + getStandardDeviation();
    }

    /**
     * Get the a single String result with the statistics. Can include a lot
     * of extra measurements if extra is set to true.
     * See {@link DynamicBin1D}.
     */
    public String getStats( boolean extra )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( "  Mean = " + getMean() + "\n" );
        buf.append( "  Standard deviation = " + getStandardDeviation() +"\n");
        buf.append( "  Median = " + getMedian() + "\n" );
        buf.append( "  Mode (quick) = " + getMode() + "\n" );
        buf.append( "  Sum = " + getSum() + "\n" );
        buf.append( "  Minimum = " + getMinimum() + "\n" );
        buf.append( "  Maximum = " + getMaximum() + "\n" );
        buf.append( "  Number of data points = " + bin1D.size() + "\n" );
        if ( extra ) {
            buf.append( "  Sum of squares = " + bin1D.sumOfSquares() + "\n" );
            buf.append( "  RMS = " + bin1D.rms() + "\n" );
            buf.append( "  Variance = " + bin1D.variance() + "\n" );
            buf.append( "  Standard error = " + bin1D.standardError() + "\n" );
            buf.append( "  25%, 75% quantiles = " +
                        bin1D.quantile( 0.25 ) + ", " +
                        bin1D.quantile( 0.75 ) + "\n");
            int maxOrder = bin1D.getMaxOrderForSumOfPowers();
            if ( maxOrder > 2 ) {
                if ( maxOrder >= 3 ) {
                    buf.append( "  Skew = " + bin1D.skew() + "\n" );
                }
                if ( maxOrder >= 4 ) {
                    buf.append( "  Kurtosis = " + bin1D.kurtosis() + "\n" );
                }
            }
            buf.append( "  Alpha trimmed means:\n" );
            buf.append( "     0.01 = " + getAlphaTrimmedMean( 0.01 ) + "\n" );
            buf.append( "     0.05 = " + getAlphaTrimmedMean( 0.05 ) + "\n" );
            buf.append( "     0.1 = " + getAlphaTrimmedMean( 0.1 ) + "\n" );
            buf.append( "     0.2 = " + getAlphaTrimmedMean( 0.2 ) + "\n" );
            buf.append( "     0.3 = " + getAlphaTrimmedMean( 0.3 ) + "\n" );
            buf.append( "     0.4 = " + getAlphaTrimmedMean( 0.4 ) + "\n" );
        }
        return buf.toString();
    }

    public static void main( String[] args )
    {
        double[] values = new double[101];
        for ( int i = 0; i < 101; i++ ) {
            values[i] = (double) i + 1;
        }

        Statistics stats = new Statistics( values );

        System.out.println( "Statistics of values from 1 to 101" );
        System.out.println( stats.toString() );
    }
}
