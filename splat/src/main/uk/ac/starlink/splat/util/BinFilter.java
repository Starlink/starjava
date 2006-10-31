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
 *  Create a new set of data points that rebin an existing spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class BinFilter
{
    /**
     * The data to be rebinned.
     */
    private double[] data = null;

    /**
     * The data errors to be rebinned.
     */
    private double[] errors = null;

    /**
     * The rebinned data.
     */
    private double[] binnedData = null;

    /**
     * The rebinned data errors, if done.
     */
    private double[] binnedDataErrors = null;

    /**
     * The width of a bin.
     */
    private int width = 3;

    /**
     *  Create an instance, rebinning the given data using the given
     *  number of data elements in each new data element.
     *
     *  @param data the array of values to be rebinned.
     *  @param errors the errors of the data values, if available. Set to null
     *                if not.
     *  @param width the number of positions that are used for each output
     *               value.
     */
    public BinFilter( double[] data, double[] errors, int width )
    {
        setWidth( width );
        setData( data, errors );
    }

    /**
     * Set the width used to rebin the data.
     */
    public void setWidth( int width )
    {
        this.width = Math.max( 1, width );
        binnedData = null;
        binnedDataErrors = null;
    }

    /**
     * Get the width of a bin.
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Set the data to be used for rebinning.
     */
    public void setData( double[] data, double[] errors )
    {
        this.data = data;
        this.errors = errors;
    }

    /**
     * Get the binned data.
     */
    public double[] eval()
    {
        if ( binnedData == null ) {
            doCalc( data, errors, width );
        }
        return binnedData;
    }

    /**
     * Get the binned data errors.
     */
    public double[] errors()
    {
        if ( binnedData == null ) {
            doCalc( data, errors, width );
        }
        return binnedDataErrors;
    }

    /**
     * Perform the binning calculations.
     */
    protected void doCalc( double[] data, double[] errors, int width )
    {
        int nout = data.length / width;

        if ( nout == 0 ) {
            return;
        }
        binnedData = new double[nout];

        if ( errors == null ) {
            binnedDataErrors = null;
            double sum;
            int count;
            for ( int k = 0, i = 0; k < nout; k++, i += width ) {
                sum = 0.0;
                count = 0;
                for ( int j = 0; j < width; j++ ) {
                    if ( data[i+j] != SpecData.BAD ) {
                        sum += data[i+j];
                        count++;
                    }
                }
                if ( count > 0 ) {
                    binnedData[k] = sum / (double) count;
                }
                else {
                    binnedData[k] = SpecData.BAD;
                }
            }
        }
        else {
            binnedDataErrors = new double[nout];
            double sum;
            double vsum;
            int count;
            for ( int k = 0, i = 0; k < nout; k++, i += width ) {
                sum = 0.0;
                vsum = 0.0;
                count = 0;
                for ( int j = 0; j < width; j++ ) {
                    if ( data[i+j] != SpecData.BAD ) {
                        sum += data[i+j];
                        vsum += ( errors[i+j] * errors[i+j] );
                        count++;
                    }
                }
                if ( count > 0 ) {
                    binnedData[k] = sum / (double) count;
                    binnedDataErrors[k] = Math.sqrt( vsum ) / (double) count;
                }
                else {
                    binnedData[k] = SpecData.BAD;
                    binnedDataErrors[k] = SpecData.BAD;
                }
            }
        }
    }


    public static void main( String[] args )
    {
        for ( int m = 0; m < 3; m++ ) {
            double data[] = new double[m+10];
        
            for ( int i = 0; i < m+10; i++ ) {
                data[i] = (double) i + 1;
            }
            System.out.println( "------------------" );
            System.out.println( "Length of array = " + data.length );

            for ( int b = 1; b < m+10; b++ ) {
                BinFilter f = new BinFilter( data, null, b );
                double binnedData[] = f.eval();
                
                System.out.println( "Bin size = " + b );
                System.out.println( "Reduces to: " + binnedData.length );

                for ( int i = 0; i < binnedData.length; i++ ) {
                    System.out.print( binnedData[i] + ", " );
                }
                System.out.println();
            }
        }
    }
}

