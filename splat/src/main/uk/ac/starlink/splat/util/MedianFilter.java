/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-JAN-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import java.util.Arrays;
import uk.ac.starlink.splat.data.SpecData;

/**
 * Create a median filtered version of a data array. The median is the
 * middle value from those in a region about the current value
 * (i.e. this is a windowed filter).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class MedianFilter
{
    /**
     * The data to be filtered.
     */
    private double[] data = null;

    /**
     * The number of values used in the median window.
     */
    private int window = 3;

    /**
     *  Create an instance, obtaining the median in a region window
     *  elements wide about each input position.
     *
     *  @param data the array of values to be median filtered.
     *  @param window the number of positions that are used for the
     *                window. Will be increased to the nearest odd
     *                integer and has a minimum value of 3.
     */
    public MedianFilter( double[] data, int window )
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
     * Perform the median filtering of array using a region of size
     * window about each position. The result is the median filtered
     * array.
     */
    protected double[] doCalc( double[] data, int window )
    {
        double[] result = new double[data.length];
        double[] sorted = new double[window];

        int high = Math.min( window / 2, data.length - 1 );
        int low = -high - 1;
        int actlow = low;
        
        int used = 0;
        for ( int i = 0; i < data.length; i++ ) {

            //  Lower part of window must be 0 or greater.
            low++;
            if ( low < 0 ) {
                actlow = 0;
            }
            else {
                actlow = low;
            }

            //  High part of window must lie within array.
            if ( high < data.length ) high++;

            //  Copy window values into array minus any BAD values.
            used = 0;
            for ( int j = actlow; j < high; j++ ) {
                if ( data[j] != SpecData.BAD ) {
                    sorted[used++] = data[j];
                }
            }

            if ( used > 0 ) {
                //  Sort and pick median... TODO: use quicker rank
                //  picker and average middle values when used is even
                //  (rare).
                Arrays.sort( sorted, 0, used );
                result[i] = sorted[used / 2];
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }
}

