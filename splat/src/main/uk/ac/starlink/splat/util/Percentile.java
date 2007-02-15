/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.util.Arrays;
import uk.ac.starlink.splat.data.SpecData;

/**
 * Calculate the percentile limits for a given array of values.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class Percentile
{
    /**
     * The number of bins used when forming the histogram for
     * percentile cutting.
     */
    protected int sampleSize = 1024;

    /**
     * The histogram of binned values.
     */
    protected int[] histogram = null;

    /**
     * The width of the histogram bins.
     */
    protected double width = 1.0;

    /**
     * The zero point of the histogram bins.
     */
    protected double zero = 0.0;

    /**
     * Total number of counts in the histogram.
     */
    protected double sum = 0.0;

    /**
     * Minimum value.
     */
    protected double min = 0.0;

    /**
     * Maximum value
     */
    protected double max = 0.0;

    /**
     * Create an instance, with default sampleSize.
     *
     * @param array the values.
     */
    public Percentile( double[] array )
    {
        setSampleSize( array, sampleSize );
    }

    /**
     * Create an instance.
     *
     * @param array the values.
     * @param sampleSize the number of bins used to sample the array.
     */
    public Percentile( double[] array, int sampleSize )
    {
        setSampleSize( array, sampleSize );
    }

    /**
     * Set the number of bins used when forming the histogram.
     */
    public void setSampleSize( double[] array, int sampleSize )
    {
        this.sampleSize = sampleSize;
        histogram = new int[sampleSize];
        sample( array );
    }

    /**
     * Return a percentile limit.
     *
     * @param percentile the percentile to calculate.
     * @return the percentile data value.
     */
    public double get( double percentile )
    {
        if ( percentile == 100.0 ) {
            return max;
        } 
        else if ( percentile == 0.0 ) {
            return min;
        }

        // If the percentile is outside the range 0 to 100, then we
        // need to calculate the difference and add or subtract this
        // from the min/max, times the number of intervals needed.
        double realPercentile = percentile;
        int nfact = 0;
        if ( realPercentile > 100.0 ) {
            nfact = (int) ( percentile * 0.01 );
            percentile = percentile - ( nfact * 100.0 );
        }
        else if ( realPercentile < 0.0 ) {
            nfact = (int) ( percentile * -0.01 );
            percentile = Math.abs( percentile ) - ( nfact * 100.0 );
        }
        double value = 0.0;

        // Calculate how many counts the percentile position
        // corresponds to.
        double need = sum * percentile * 0.01;

        // Loop until the count sum exceeds the sum which is
        // required. The previous bin and this bin are used to
        // interpolate the actual bin fraction which corresponds to
        // the required percentile.
        double count = 0.0;
        double last = 0.0;
        for ( int i = 0; i < sampleSize; i++ ) {
            count += (double) histogram[i];
            if ( count > need ) {
                // Located the position. Interpolate value from the
                // last position to this position, to get fractional
                // bin offset.
                value = ( need - last ) / ( count - last );

                // Determine the actual data value this position
                // corresponds to.
                value = ( i + value ) * width + zero;
                break;
            }

            // Remember this count for use in interpolation.
            last = count;
        }

        // Return is clipped to min/max unless the percentile
        // requested was outside the range 0 to 100.
        value = Math.max( min, Math.min( max, value ) );

        if ( realPercentile != percentile ) {
            if ( realPercentile > 100.0 ) {
                value = max + ( value - min ) + ( (nfact-1) * ( max - min ) );
            }
            else if ( realPercentile < 0.0 ) {
                value = min - ( value - min ) - ( nfact * ( max - min ) );
            }
        }
        return value;
    }

    /**
     * Create the histogram of array values.
     */
    protected void sample( double[] array )
    {
        // Find the minimum and maximum values in the array.
        min = Double.MAX_VALUE;
        max = -Double.MAX_VALUE;
        for ( int i = 0; i < array.length; i++ ) {
            if ( array[i] != SpecData.BAD ) {
                if ( array[i] > max ) max = array[i];
                if ( array[i] < min ) min = array[i];
            }
        }

        // Clear the histogram of any previous values.
        Arrays.fill( histogram, 0 );

        // Set the bin width.
        double scale = ((double) sampleSize - 1 ) / ( max - min );
        width = 1.0 / scale;

        // Set the zero point.
        zero = min - ( 0.5 * width );

        // Form the histogram.
        int index;
        sum = 0.0;
        for ( int i = 0; i < array.length; i++ ) {
            if ( array[i] != SpecData.BAD ) {
                index = (int) Math.rint( scale * ( array[i]  - min ) );
                try {
                    histogram[index]++;
                    sum++;
                }
                catch (Exception e) {
                    System.err.println( "Internal error (" + 
                                        index + "," + histogram.length
                                        + ")" );
                    e.printStackTrace();
                }
            }
        }
        zero = zero - ( 0.5 * width );
    }

    //  Test routine.
    public static void main( String[] args ) 
    {
        double[] array = new double[101];
        for ( int i = 0; i < array.length; i++ ) {
            array[i] = 50.0 + i;
        }

        System.out.println( "Positive array" );
        Percentile perc = new Percentile( array, array.length );
        System.out.println( "-525 -> " + perc.get( -525.0 ) );
        System.out.println( "-200 -> " + perc.get( -200.0 ) );
        System.out.println( "  -5 -> " + perc.get( -5.0 ) );
        System.out.println( "   0 -> " + perc.get( 0.0 ) );
        System.out.println( "   5 -> " + perc.get( 5.0 ) );
        System.out.println( "  50 -> " + perc.get( 50.0 ) );
        System.out.println( "  95 -> " + perc.get( 95.0 ) );
        System.out.println( " 100 -> " + perc.get( 100.0 ) );
        System.out.println( " 105 -> " + perc.get( 105.0 ) );
        System.out.println( " 200 -> " + perc.get( 200.0 ) );
        System.out.println( " 525 -> " + perc.get( 525.0 ) );

        for ( int i = 0; i < array.length; i++ ) {
            array[i] *= -1.0;
        }

        System.out.println( "Negative array" );
        perc = new Percentile( array, array.length );
        System.out.println( "-525 -> " + perc.get( -525.0 ) );
        System.out.println( "-200 -> " + perc.get( -200.0 ) );
        System.out.println( "  -5 -> " + perc.get( -5.0 ) );
        System.out.println( "   0 -> " + perc.get( 0.0 ) );
        System.out.println( "   5 -> " + perc.get( 5.0 ) );
        System.out.println( "  50 -> " + perc.get( 50.0 ) );
        System.out.println( "  95 -> " + perc.get( 95.0 ) );
        System.out.println( " 100 -> " + perc.get( 100.0 ) );
        System.out.println( " 105 -> " + perc.get( 105.0 ) );
        System.out.println( " 200 -> " + perc.get( 200.0 ) );
        System.out.println( " 525 -> " + perc.get( 525.0 ) );


        for ( int i = 0; i < array.length; i++ ) {
            array[i] = -50.0 + i;
        }

        System.out.println( "Negative->Positive array" );
        perc = new Percentile( array, array.length );
        System.out.println( "-525 -> " + perc.get( -525.0 ) );
        System.out.println( "-200 -> " + perc.get( -200.0 ) );
        System.out.println( "  -5 -> " + perc.get( -5.0 ) );
        System.out.println( "   0 -> " + perc.get( 0.0 ) );
        System.out.println( "   5 -> " + perc.get( 5.0 ) );
        System.out.println( "  50 -> " + perc.get( 50.0 ) );
        System.out.println( "  95 -> " + perc.get( 95.0 ) );
        System.out.println( " 100 -> " + perc.get( 100.0 ) );
        System.out.println( " 105 -> " + perc.get( 105.0 ) );
        System.out.println( " 200 -> " + perc.get( 200.0 ) );
        System.out.println( " 525 -> " + perc.get( 525.0 ) );
    }
}
