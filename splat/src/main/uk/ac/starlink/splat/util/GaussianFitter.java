/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-DEC-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * GaussianFitter fits a gaussian to a set of data points using a
 * non-linear weighted least squares fit (non-linear is required for
 * centering).  The relevant formula is:
 * <pre>
 *    y(radius) = scale * exp( -0.5 * ((radius-centre)/sigma)**2 )
 * </pre>
 * To use this class create an instance with the data to be
 * fitted. Interpolated positions can then be obtained using the
 * {@link #evalYDataArray} and {@link #evalYData} methods.
 * A chi squared residual to the fit can be obtained from the
 * {@link #getChi} method.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class GaussianFitter
    extends AbstractFunctionFitter
{
    /**
     * The parameters of this function.
     */
    protected double[] params = new double[3];

    /**
     * Whether the parameters are fixed or floating.
     */
    protected boolean[] fixed = new boolean[3];

    // Access to parameters.
    public static final int SCALE = 0;
    public static final int CENTRE = 1;
    public static final int SIGMA = 2;

    /**
     * The chi square of the fit.
     */
    protected double chiSquare = 0.0;

    /**
     * For sub-classes.
     */
    protected GaussianFitter() {}

    /**
     * Fit a gaussian to unweighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y. These are assumed precise.
     * @param scale initial estimate of the gaussian scale.
     * @param centre initial estimate of the gaussian centre.
     * @param sigma initial estimate of the gaussian sigma.
     */
    public GaussianFitter( double[] x, double[] y, double scale,
                           double centre, double sigma )
    {
        this( x, y, null, scale, centre, sigma );
    }

    /**
     * Fit a gaussian to weighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances).
     * @param scale initial estimate of the gaussian scale.
     * @param centre initial estimate of the gaussian centre.
     * @param sigma initial estimate of the gaussian sigma.
     */
    public GaussianFitter( double[] x, double[] y, double[] w,
                           double scale, double centre, double sigma )
    {
        this( x, y, w, scale, false, centre, false, sigma, false );
    }

    /**
     * Fit a gaussian to weighted data points, possible fixing some parameters
     * to the initial estimate.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances).
     * @param scale initial estimate of the gaussian scale.
     * @param scaleFixed whether the scale value should be fixed.
     * @param centre initial estimate of the gaussian centre.
     * @param centreFixed whether the centre value should be fixed.
     * @param sigma initial estimate of the gaussian sigma.
     * @param sigmaFixed whether the sigma value should be fixed.
     */
    public GaussianFitter( double[] x, double[] y, double[] w,
                           double scale, boolean scaleFixed,
                           double centre, boolean centreFixed,
                           double sigma, boolean sigmaFixed )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[SIGMA] = sigma;
        fixed[SCALE] = scaleFixed;
        fixed[CENTRE] = centreFixed;
        fixed[SIGMA] = sigmaFixed;

        if ( w == null ) {
            // Default weights are 1.0.
            w = new double[x.length];
            for (int i = 0; i < x.length; i++ ) {
                w[i] = 1.0;
            }   
        }
        doFit( x, y, w );
    }

    /**
     * Perform the fit of the data by a gaussian.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     * @param w array of weights for each position.
     */
    protected void doFit( double[] x, double[] y, double[] w )
    {
        //  Create the LevMarq minimisation object.
        LevMarq lm = new LevMarq( this, x.length, 3 );

        //  Pass it the data.
        for ( int i = 0; i < x.length; i++ ) {
            lm.setX( i + 1, x[i] );
            lm.setY( i + 1, y[i] );
            if ( w[i] == 0.0 ) {
                lm.setSigma( i + 1, 1.0 );
            } else {
                lm.setSigma( i + 1, 1.0 / w[i] );
            }
        }

        //  Set the initial guesses.
        lm.setParam( 1, params[SCALE], fixed[SCALE] );
        lm.setParam( 2, params[CENTRE], fixed[CENTRE] );
        lm.setParam( 3, params[SIGMA], fixed[SIGMA] );

        //  And mimimise.
        lm.fitData();

        //  Record estimate of goodness of fit.
        chiSquare = lm.getChisq();

        //  And the fit parameters.
        params[SCALE] = lm.getParam( 1 );
        params[CENTRE] = lm.getParam( 2 );
        params[SIGMA] = lm.getParam( 3 );
    }

    /**
     * Get chi square of this fit.
     */
    public double getChi()
    {
        return chiSquare;
    }

    /**
     * Get the scale height of fit.
     */
    public double getScale()
    {
        return params[SCALE];
    }

    /**
     * Get centre of fit.
     */
    public double getCentre()
    {
        return params[CENTRE];
    }

    /**
     * Get the sigma of the fit.
     */
    public double getSigma()
    {
        return params[SIGMA];
    }

    /**
     * Get the integrated intensity of the gaussian.
     *
     * This is scale * sigma * sqrt( 2 * pi ).
     */
    public double getFlux()
    {
        //  Note "1.0/sigma*sqrt(2*pi)" is the area of a normalised
        //  (i.e. flux of 1) gaussian, which our scale includes, so we
        //  need to remove this term from scale to get the flux.
        return params[SCALE] * params[SIGMA] * Math.sqrt( 2.0 * Math.PI );
    }

    /**
     * Evaluate the gaussian at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalYData( double x )
    {
        double rbys = Math.abs( x - params[CENTRE] ) / params[SIGMA];
        return params[SCALE] * Math.exp( -0.5 * rbys * rbys );
    }

    // Return the number of parameters used to describe this function.
    public int getNumParams()
    {
        return params.length;
    }

    // Get the parameters.
    public double[] getParams()
    {
        return params;
    }

    // Set the parameters.
    public void setParams( double[] params )
    {
        this.params[0] = params[0];
        this.params[1] = params[1];
        this.params[2] = params[2];
    }

    //  Get the fixed/floating state of parameters.
    public boolean[] getFixed()
    {
        return fixed;
    }

    // Set the fixed state of the various parameters.
    public void setFixed( boolean[] fixed )
    {
        this.fixed[0] = fixed[0];
        this.fixed[1] = fixed[1];
        this.fixed[2] = fixed[2];
    }

    public String toString()
    {
        return "GaussianFitter[" + 
            "flux = " + getFlux() + ", " +
            "scale = " + getScale() + ", " +
            "centre = " + getCentre() + ", " +
            "sigma = " + getSigma() +
            "]";
    }

    //
    // Implementation of the LevMarqFunc interface.
    //

    // Evaluate the gaussian given a set of model parameters. Also
    // evaluates the partial derivates of the current fit.
    //
    public double eval( double x, double[] a, int na, double[] dyda )
    {
        //  Calculate the value of the exponential term used in the
        //  partial derivatives.
        double rbys = ( x - a[2] ) / a[3];
        double expFunc = Math.exp( -0.5 * rbys * rbys );

        //  Value of gaussian.
        double y = a[1] * expFunc;

        //  dy/d(scale)
        dyda[1] = expFunc;

        //  dy/d(centre)
        dyda[2] = y * rbys / a[3];

        //  dy/d(sigma)
        dyda[3] = y * rbys * rbys / a[3];

        return y;
    }
}
