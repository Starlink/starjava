package uk.ac.starlink.splat.util;

/**
 * GaussianFitter fits a gaussian to a set of data points using a
 * non-linear weighted least squares fit (non-linear is required for
 * centering).  The relevant formula is:
 *
 *    y(radius) = scale * exp( -0.5 * ((radius-centre)/sigma)**2 )
 *
 * To use this class create an instance with the data to be
 * fitted. Interpolated positions can then be obtained using the
 * evalArray() and evalPoint() methods. A chi squared residual to the
 * fit can be obtained from the getChi() method.
 *
 * @since $Date$
 * @since 12-DEC-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000-2001 Central Laboratory of the Research
 *            Councils
 * @history 03-JAN-2001 modified to fit centre (becomes a non-linear problem).
 */
public class GaussianFitter 
    extends FunctionFitter 
    implements LevMarqFunc
{
    /**
     * The scale factor.
     */
    private double scale = 1.0;

    /**
     * The full width half maximum term.
     */
    private double sigma = 1.0;

    /**
     * The centre.
     */
    private double centre = 0.0;

    /**
     * The chi square of the fit.
     */
    private double chiSquare = 0.0;

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
        // Default weights are 1.0.
        double[] w = new double[x.length];
        for (int i = 0; i < x.length; i++ ) {
            w[i] = 1.0;
        }
        this.scale = scale;
        this.centre = centre;
        this.sigma = sigma;
        doFit( x, y, w );
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
        this.scale = scale;
        this.centre = centre;
        this.sigma = sigma;
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
        lm.setParam( 1, scale );
        lm.setParam( 2, centre );
        lm.setParam( 3, sigma );

        //  And mimimise.
        lm.fitData();

        //  Record estimate of goodness of fit.
        chiSquare = lm.getChisq();

        //  And the fit parameters.
        scale = lm.getParam( 1 );
        centre = lm.getParam( 2 );
        sigma = lm.getParam( 3 );
    }

    /**
     * Get chi square of this fit.
     */
    public double getChi()
    {
        return chiSquare;
    }

    /**
     * Get centre of fit.
     */
    public double getCentre()
    {
        return centre;
    }

    /**
     * Get the scale height of fit.
     */
    public double getScale()
    {
        return scale;
    }

    /**
     * Get the sigma of the fit.
     */
    public double getSigma()
    {
        return sigma;
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
        return scale * sigma * Math.sqrt( 2.0 * Math.PI );
    }

    /**
     * Evaluate the gaussian at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalPoint( double x ) {
        double rbys = Math.abs( x - centre ) / sigma;
        return scale * Math.exp( -0.5 * rbys * rbys );
    }

    //
    // Implementation of the LevMargFunc interface.
    //
    /**
     * Evaluate the gaussian given a set of model parameters. Also
     * evaluates the partial derivates of the current fit.
     *
     */
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

