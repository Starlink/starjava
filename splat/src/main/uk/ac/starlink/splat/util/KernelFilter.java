/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import uk.ac.starlink.splat.data.SpecData;

/**
 * Smooth a given data array using a kernel of weights.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class KernelFilter
{
    /**
     * The data to be smoothed.
     */
    private double[] data = null;

    /**
     * The kernel.
     */
    private double[] kernel = null;

    /**
     *  Create an instance.
     *
     *  @param data the array of values to be smoothed.
     *  @param kernel the array of weights to be used as the kernel
     */
    public KernelFilter( double[] data, double kernel[] )
    {
        setData( data );
        setKernel( kernel );
    }

    /**
     * Set the data to be used.
     */
    public void setData( double[] data )
    {
        this.data = data;
    }

    /**
     * Set the kernel to be used.
     */
    public void setKernel( double[] kernel )
    {
        this.kernel = kernel;
    }

    /**
     * Get the smoothed data.
     */
    public double[] eval()
    {
        return doCalc( data, kernel );
    }

    /**
     * Perform the convolution of the data with the given
     * kernel. Returns the resultant array.
     */
    protected double[] doCalc( double[] data, double[] kernel )
    {
        double sum;
        double wsum;
        double[] result = new double[data.length];
        int low;
        int high;
        int k;
        int halfLength = kernel.length / 2;

        //  Kernel must have odd length, if not subtract one throwing
        //  away end point. Alternative would be to add a BAD data
        //  point at end, but that would mean making a local array.
        if ( halfLength * 2 == kernel.length ) halfLength--;

        for ( int i = 0; i < data.length; i++ ) {
            if ( data[i] == SpecData.BAD ) {
                result[i] = SpecData.BAD;
            }
            else {

                //  Sum weighted data and weights around current pixel.
                sum = 0.0;
                wsum = 0.0;
                low = Math.max( 0, i - halfLength );
                high = Math.min( data.length - 1, i + halfLength );
                k = low - ( i - halfLength );

                for ( int j = low; j <= high; j++ ) {
                    if ( data[j] != SpecData.BAD && 
                         kernel[k] != SpecData.BAD ) {
                        sum += kernel[k] * data[j];
                        wsum += kernel[k];
                    }
                    k++;
                }

                if ( wsum != 0.0 ) {
                    result[i] = sum / wsum;
                }
                else {
                    result[i] = SpecData.BAD;
                }
            }
        }
        return result;
    }
}
