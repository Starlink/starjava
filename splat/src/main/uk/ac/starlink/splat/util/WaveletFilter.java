/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     2-JUN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import JSci.maths.wavelet.Multiresolution;
import JSci.maths.wavelet.FWTCoef;
import JSci.maths.wavelet.Filter;
import JSci.maths.wavelet.Signal;
import JSci.maths.wavelet.daubechies2.Daubechies2;
import JSci.maths.wavelet.daubechies3.Daubechies3;
import JSci.maths.wavelet.daubechies4.Daubechies4;
import JSci.maths.wavelet.daubechies5.Daubechies5;
import JSci.maths.wavelet.daubechies6.Daubechies6;
import JSci.maths.wavelet.daubechies7.Daubechies7;
import JSci.maths.wavelet.daubechies8.Daubechies8;
import JSci.maths.wavelet.cdf2_4.CDF2_4;
import JSci.maths.wavelet.cdf3_5.CDF3_5;
import JSci.maths.wavelet.haar.MultiSplineHaar;

/**
 * Denoise a set of data points by applying a wavelet filter of some
 * kind. XXX make sure that the coordinates are equally spaced for a
 * proper representation of the frequency domain.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class WaveletFilter
{
    /**
     * The data to be filtered.
     */
    private double[] data = null;

    /**
     * The threshold of data sum to zero.
     */
    private double threshold = 0.5;

    /**
     * The wavelet in use.
     */
    private int wavelet = DAUBECHIES2;

    // Enumerations of the filters available.
    public static final int DAUBECHIES2 = 0;
    public static final int DAUBECHIES3 = 1;
    public static final int DAUBECHIES4 = 2;
    public static final int DAUBECHIES5 = 3;
    public static final int DAUBECHIES6 = 4;
    public static final int DAUBECHIES7 = 5;
    public static final int DAUBECHIES8 = 6;
    public static final int HAAR = 7;
    //public static final int CDF2_4 = 8;
    //public static final int CDF3_4 = 9;

    /**
     * Symbolic names of known wavelets (can be indexed by enumerations).
     */
    public final static String[] WAVELETS = {
        "Daubechies2",
        "Daubechies3",
        "Daubechies4",
        "Daubechies5",
        "Daubechies6",
        "Daubechies7",
        "Daubechies8",
        "Haar"
        //"CDF2_4",
        //"CDF3_5"
    };

    /**
     * Create an instance, filtering the given data.
     *
     * @param data the array of values to be filtered.
     * @param wavelet the name of the wavelet filter 
     *                (from WaveletFilter.WAVELETS)
     * @param percent the percentage of wavelet coefficients to remove.
     */
    public WaveletFilter( double[] data, String wavelet, double percent )
    {
        setFilter( wavelet );
        setData( data );
        setPercentage( percent );
    }

    /**
     * Set the wavelet filter to use by symbolic name. The default on
     * failure is DAUBECHIES2.
     */
    public void setFilter( String name )
    {
        wavelet = DAUBECHIES2;
        if ( "Daubechies3".equals( name ) ) {
            wavelet = DAUBECHIES3;
        }
        else if ( "Daubechies4".equals( name ) ) {
            wavelet = DAUBECHIES4;
        }
        else if ( "Daubechies5".equals( name ) ) {
            wavelet = DAUBECHIES5;
        }
        else if ( "Daubechies6".equals( name ) ) {
            wavelet = DAUBECHIES6;
        }
        else if ( "Daubechies7".equals( name ) ) {
            wavelet = DAUBECHIES7;
        }
        else if ( "Daubechies8".equals( name ) ) {
            wavelet = DAUBECHIES8;
        }
        //else if ( "CDF2_4".equals( name ) ) {
        //    wavelet = CDF2_4;
        //}
        //else if ( "CDF3_5".equals( name ) ) {
        //    wavelet = CDF3_4;
        //}
        else if ( "Haar".equals( name ) ) {
            wavelet = HAAR;
        }
    }

    /**
     * Get the filter being used.
     */
    public String getFilter()
    {
        return WAVELETS[wavelet];
    }
    
    /**
     * Set the data to be used.
     */
    public void setData( double[] data )
    {
        this.data = data;
    }
    
    /**
     * Set the fraction (0-1) of coefficients to be zeroed.
     */
    public void setFraction( double value )
    {
        threshold = value;
        if ( threshold < 0.0 ) {
            threshold = 0.0;
        }
        else if ( threshold > 1.0 ) {
            threshold = 1.0;
        }
    }

    /**
     * Set the percentage (0-100) of coefficients to be zeroed.
     */
    public void setPercentage( double value )
    {
        setFraction( value * 0.01 );
    }
    
    /**
     * Get the fraction (0-1) of coefficients to be zeroed.
     */
    public double getFraction()
    {
        return threshold;
    }

    /**
     * Get the filtered data.
     */
    public double[] eval()
    {
        return doCalc();
    }

    /**
     * Get an instance of the type of filter to apply.
     */
    protected Filter getFilterInstance()
    {
        switch ( wavelet ) {
            case DAUBECHIES2: 
                return new Daubechies2();
            case DAUBECHIES3: 
                return new Daubechies3();
            case DAUBECHIES4: 
                return new Daubechies4();
            case DAUBECHIES5: 
                return new Daubechies5();
            case DAUBECHIES6: 
                return new Daubechies6();
            case DAUBECHIES7: 
                return new Daubechies7();
            case DAUBECHIES8: 
                return new Daubechies8();
            //case CDF2_4:
                //return new CDF2_4();
            //case CDF3_4:
                //return new CDF3_5();
            case HAAR:
                return new MultiSplineHaar();
        }
        return null;
    }

    /**
     * Perform the calculation.
     */
    protected double[] doCalc()
    {
        Filter filter = getFilterInstance();
        int filtertype = ((Multiresolution) filter).getFilterType();

        //  Need a power of 2 for data size, plus padding for
        //  dyadic multiresolution scaling functions.
        int maxlevel = 1;
        while ( data.length > Math.pow( 2.0, (double) maxlevel ) ) {
            maxlevel++;
        }
        int count = (int) Math.pow( 2.0, (double) maxlevel ) + filtertype;

        //  Copy data to padded array, placing near centre and setting
        //  edges to the end value.
        double[] noisy = new double[count];
        int offset = count = ( count - data.length ) / 2;

        //  Trim padding away from all data.
        System.arraycopy( data, 0, noisy, offset, data.length );

        // Fill any buffered regions with end values.
        double value = noisy[offset];
        for ( int i = 0; i < offset; i++ ) {
            noisy[i] = value;
        }
        value = noisy[offset + data.length - 1];
        for ( int i = offset + data.length; i < noisy.length; i++ ) {
            noisy[i] = value;
        }

        // Choose a maximum level and use that. Note 20 is max
        // possible, and we need to leave space for filtertype 
        // padding (-4). XXX how slow does this make it!!!
        int level = Math.min( maxlevel - 4, 20 );
        //int level = 5;

        // Make the Signal and filter it.
        Signal signal = new Signal( noisy );
        signal.setFilter( filter );
        FWTCoef signalCoeffs = signal.fwt( level );

        //  Zero any coefficients that are less than some fraction of
        //  the total sum. Note compress is inverse function.
        signalCoeffs.denoise( threshold );

        //  Rebuild the signal with the new set of coefficients.
        double[] rebuild = 
            signalCoeffs.rebuildSignal( filter ).evaluate( 0 );

        //  Trim padding away from all data.
        double[] result = new double[data.length];
        System.arraycopy( rebuild, offset, result, 0, data.length );
        return result;
    }
}
