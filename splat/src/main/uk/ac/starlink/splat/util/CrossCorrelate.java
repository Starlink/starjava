/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     07-JUL-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import JSci.maths.FourierMath;
import JSci.maths.Complex;

/**
 * Cross-correlate two 1D signals, returning the cross-correlation
 * function and the peak correlation bin.
 *
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class CrossCorrelate
{
    /**
     * Fourier transform of reference signal.
     */
    private Complex[] refResult = null;

    /**
     * Correlation result.
     */
    private double[] correlation = null;

    /**
     * Position of peak in correlation result.
     */
    private int peakPosition = -1;

    /**
     * Create an instance.
     *
     * @param ref the reference values.
     */
    public CrossCorrelate( double[] ref )
    {
        refResult = fourierTransform( ref );
    }

    /**
     * Do the cross-correlation of the reference values against a set
     * of real signal values.
     *
     * @param signal the values to cross-correlate.
     */
    public void correlate( double[] signal )
    {
        Complex[] signalResult = fourierTransform( signal );
        correlation = doCalc( refResult, signalResult );

        // Local peak. XXX use a model to decrease noise sensitivity?
        findPeak();
    }

    /**
     * Return the correlation signal.
     */
    public double[] getCorrelation()
    {
        return correlation;
    }

    /**
     * Return the peak position.
     */
    public int getPeak()
    {
        return peakPosition;
    }

    /**
     * Create the Fourier transform of a real 1D signal.
     */
    protected Complex[] fourierTransform( double[] signal )
    {
        //return FourierMath.sort( FourierMath.transform( signal ) );
        return FourierMath.transform( signal );
    }

    /**
     * Perform the cross-correlation of two complex signals.
     */
    protected double[] doCalc( Complex[] ref, Complex[] signal )
    {
        // First perform multiplication of complex conjugate.
        Complex[] multiple = multiplyConjugate( ref, signal );

        // Untransform this to get the correlation function.
        multiple = FourierMath.sort( FourierMath.inverseTransform( multiple ));

        // For a cross-correlation the imaginary parts are all zero,
        // so we just need to extract the real parts.
        double[] result = new double[multiple.length];
        for ( int i = 0; i < multiple.length; i++ ) {
            result[i] = multiple[i].real();
        }
        return result;
    }

    /**
     * Multiply a complex number by the conjugate of another.
     */
    protected Complex[] multiplyConjugate( Complex[] ref, Complex signal[] )
    {
        //  XXX may need to be more efficient, signal.conjugate
        //  creates a Complex that is thrown away immediately.
        Complex result[] = new Complex[ref.length];
        for ( int i = 0; i < ref.length; i++ ) {
            result[i] = ref[i].multiply( signal[i].conjugate() );
        }
        return result;
    }

    /**
     * Find the peak in the correlation array.
     */
    protected void findPeak()
    {
        double max = correlation[0];
        peakPosition = 0;
        for ( int i = 1; i < correlation.length; i++ ) {
            if ( correlation[i] > max ) {
                max = correlation[i];
                peakPosition = i;
            }
        }
    }
}
