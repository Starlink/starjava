/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-JAN-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import uk.ac.starlink.splat.data.SpecData;

/**
 *  Create a new set of data points that are a running average of
 *  another set.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AverageFilter
{
    /**
     * The data to be averaged.
     */
    private double[] data = null;

    /**
     * The number of values used in the averaging window.
     */
    private int window = 3;

    /**
     *  Create an instance, averaging the given data using the given
     *  number of data elements about the current data position.
     *
     *  @param data the array of values to be averaged.
     *  @param window the number of positions that are used for the
     *                average. Will be increased to the nearest odd
     *                integer and has a minimum value of 3.
     */
    public AverageFilter( double[] data, int window )
    {
        setWindow( window );
        setData( data );
    }

    /**
     * Set the window to use. The value is always set to the next
     * highest odd value and has a minimum of 3.
     */
    public void setWindow( int window )
    {
        window = ( window / 2 ) * 2 + 1;
        if ( window == 1 ) window = 3;
        this.window = window;
    }

    /**
     * Get the number of values used in the averaging window.
     */
    public int getWindow()
    {
        return window;
    }

    /**
     * Set the data to be used for input to the averaging
     * calculations.
     */
    public void setData( double[] data )
    {
        this.data = data;
    }

    /**
     * Get the averaged data.
     */
    public double[] eval()
    {
        return doCalc( data, window );
    }

    /**
     * Perform the windowed average calculations. Returns the
     * resultant array.
     */
    protected double[] doCalc( double[] data, int window )
    {
        double[] result = new double[data.length];

        // Initial sum, step half window ahead, but only if half step
        // is shorter than spectrum.
        int count = 0;
        double sum = 0.0;
        int hw = Math.min( window / 2, data.length - 1 );
        for ( int i = 0; i <= hw; i++ ) {
            if ( data[i] != SpecData.BAD ) {
                sum += data[i];
                count++;
            }
        }
        if ( count > 0 ) {
            result[0] = sum / (double) count;
        } 
        else {
            result[0] = SpecData.BAD;
        }

        int low;
        int high;
        for ( int i = 1; i < data.length; i++ ) {
            low = i - hw - 1;
            if ( low >= 0 && data[low] != SpecData.BAD ) {
                count--;
                sum -= data[low];
            }
            high = i + hw;
            if ( high < data.length && data[high] != SpecData.BAD ) {
                count++;
                sum += data[high];
            }
            if ( count > 0 ) {
                result[i] = sum / (double) count;
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }
}

