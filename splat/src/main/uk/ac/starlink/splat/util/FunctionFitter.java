/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-MAY-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Abstract class of for functions that perform fitting of a function
 * to a spectral line. Examples of derived classes are GaussianFitter,
 * LorentzFitter and VoigtFitter. Fitters should honour the basic set
 * of interface methods defined here. Any common functionality that
 * can be shared should also be implemented here.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see GaussianFitter
 * @see LorentzFitter
 * @see VoigtFitter 
 */
public abstract class FunctionFitter
{
    /**
     * Get Chi square for the fit.
     */
    public abstract double getChi();

    /**
     * Get the centre of fit.
     */
    public abstract double getCentre();

    /**
     * Get the scale height of fit.
     */
    public abstract double getScale();

    /**
     * Get the integrated intensity of the fit.
     */
    public abstract double getFlux();

    /**
     * Evaluate the function at a set of given X positions.
     *
     * @param x array of X positions at which to evaluate.
     * @return array of values at given X's.
     */
    public double[] evalArray( double[] x )
    {
        double[] y = new double[x.length];
        for ( int i = 0; i < x.length; i++ ) {
            y[i] = evalPoint( x[i] );
        }
        return y;
    }

    /**
     * Evaluate the function at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public abstract double evalPoint( double x );

    /**
     *  Calculate an RMS to the given data for the fit.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     */
    public double calcRms( double[] x, double[] y )
    {
        double rmssum = 0.0;
        double dy = 0.0;
        for ( int i = 0; i < x.length; i++ ) {
            dy = y[i] - evalPoint( x[i] );
            rmssum += dy * dy;
        }
        return Math.sqrt( rmssum / (double)( x.length - 1 ) );
    }

    /**
     *  Calculate an RMS to the given data and pre-calculated fit.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     * @param fity array of model values for each X coordinate.
     */
    public double calcRms( double[] x, double[] y, double[] fity )
    {
        double rmssum = 0.0;
        double dy = 0.0;
        for ( int i = 0; i < x.length; i++ ) {
            dy = y[i] - fity[i];
            rmssum += dy * dy;
        }
        return Math.sqrt( rmssum / (double)( x.length - 1 ) );
    }
}
