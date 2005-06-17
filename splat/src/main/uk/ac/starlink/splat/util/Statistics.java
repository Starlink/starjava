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
        bin1D.addAllOfFromTo( new DoubleArrayList( data ), 0, data.length-1 );
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
     * Get the sample standard deviation.
     */
    public double getStandardDeviation()
    {
        return bin1D.standardDeviation();
    }


    public static void main( String[] args ) 
    {
        double[] values = new double[101];
        for ( int i = 0; i < 101; i++ ) {
            values[i] = (double) i + 1;
        }

        Statistics stats = new Statistics( values );
        
        System.out.println( "Statistics of values from 1 to 101" );
        System.out.println( 
                           "Maximum = " + stats.getMaximum() + "\n" +
                           "Minimum = " + stats.getMinimum() + "\n" +
                           "Sum = " + stats.getSum() + "\n" +
                           "Median = " + stats.getMedian() + "\n" +
                           "Mode = " + stats.getMode() + "\n" +
                           "Mean = " + stats.getMean() + "\n" +
                           "Std = " + stats.getStandardDeviation()
                           );
    }
}
