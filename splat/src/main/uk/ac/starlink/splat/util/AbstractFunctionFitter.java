/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-MAY-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Abstract class of for {@link FunctionFitter}s that perform fitting
 * of a  function to a spectral line using a {@link LevMarq}
 * non-linear minimisation. 
 * <p>
 * Examples of derived classes are GaussianFitter, LorentzFitter and
 * VoigtFitter. Fitters should honour the basic set of interface
 * methods defined here and in {@link FunctionFitter}. Any common 
 * functionality that can be shared should also be implemented here.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see GaussianFitter
 * @see LorentzFitter
 * @see VoigtFitter 
 */
public abstract class AbstractFunctionFitter
    implements FunctionFitter
{
    // Methods not implemented from FunctionFitter.
    public abstract double getChi();
    public abstract double getCentre();
    public abstract double getScale();
    public abstract double getFlux();
    public abstract double evalYData( double x );

    // Evaluate the function at a set of given X positions.
    public double[] evalYDataArray( double[] x )
    {
        double[] y = new double[x.length];
        for ( int i = 0; i < x.length; i++ ) {
            y[i] = evalYData( x[i] );
        }
        return y;
    }

    // Calculate an RMS to the given data for the fit.
    public double calcRms( double[] x, double[] y )
    {
        double rmssum = 0.0;
        double dy = 0.0;
        for ( int i = 0; i < x.length; i++ ) {
            dy = y[i] - evalYData( x[i] );
            rmssum += dy * dy;
        }
        return Math.sqrt( rmssum / (double)( x.length - 1 ) );
    }

    // Calculate the residuals to the given data for the fit.
    public double[] calcResiduals( double[] x, double[] y )
    {
        double[] resids = new double[x.length];
        for ( int i = 0; i < x.length; i++ ) {
            resids[i] = y[i] - evalYData( x[i] );
        }
        return resids;
    }

    // Calculate an RMS to the given data and pre-calculated fit.
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
