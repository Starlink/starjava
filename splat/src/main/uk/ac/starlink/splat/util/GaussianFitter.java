/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
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
 * {@link #getChi} method, for this to have significance the supplied weights
 * must be inverse variances (the same applies for the error estimates when
 * weights are supplied).
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
     * The estimated errors in the parameters.
     */
    protected double[] perrors = new double[3];

    /**
     * Whether the parameters are fixed or floating.
     */
    protected boolean[] fixed = new boolean[3];

    // Access to parameters.
    public static final int SCALE = 0;
    public static final int CENTRE = 1;
    public static final int SIGMA = 2;

    /** Conversion factor for sigma to FWHM. */
    public static final double FWHMFAC = 2.0*Math.sqrt( 2.0*Math.log( 2.0 ) );

    /**
     * The chi square of the fit.
     */
    protected double chiSquare = 0.0;

    /**
     *  Whether unit weights were used.
     */
    protected boolean unitweights = false;

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
        unitweights = false;
        perrors[SCALE] = 0.0;
        perrors[CENTRE] = 0.0;
        perrors[SIGMA] = 0.0;

        if ( w == null ) {
            // Default weights are 1.0.
            unitweights = true;
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
                lm.setSigma( i + 1, Math.sqrt( 1.0 / w[i] ) );
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

        //  Estimated errors of the parameters.
        perrors[SCALE] = lm.getError( 1, unitweights );
        perrors[CENTRE] = lm.getError( 2, unitweights );
        perrors[SIGMA] = lm.getError( 3, unitweights );
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
     * Get the error in scale height of fit.
     */
    public double getScaleError()
    {
        return perrors[SCALE];
    }

    /**
     * Get centre of fit.
     */
    public double getCentre()
    {
        return params[CENTRE];
    }

    /**
     * Get error in centre of fit.
     */
    public double getCentreError()
    {
        return perrors[CENTRE];
    }

    /**
     * Get the sigma of the fit.
     */
    public double getSigma()
    {
        return params[SIGMA];
    }

    /**
     * Get the error in the sigma of the fit.
     */
    public double getSigmaError()
    {
        return perrors[SIGMA];
    }

    /**
     * Get the FWHM.
     */
    public double getFWHM()
    {
        return params[SIGMA] * FWHMFAC;
    }

    /**
     * Get the error in the FWHM.
     */
    public double getFWHMError()
    {
        return perrors[SIGMA] * FWHMFAC;
    }

    /**
     * Get if the scale height is held fixed.
     */
    public boolean getScaleFixed()
    {
        return fixed[SCALE];
    }

    /**
     * Get if the sigam is held fixed.
     */
    public boolean getSigmaFixed()
    {
        return fixed[SIGMA];
    }

    /**
     * Get if the centre is being held fixed.
     */
    public boolean getCentreFixed()
    {
        return fixed[CENTRE];
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
     * Get the estimated error in the flux.
     */
    public double getFluxError()
    {
        //  Sum of the fractional errors, as variance is:
        double var = ( perrors[SCALE]*perrors[SCALE] * params[SIGMA]*params[SIGMA] +
                       perrors[SIGMA]*perrors[SIGMA] * params[SCALE]*params[SCALE] );
        if ( var > 0.0 ) {
            return Math.sqrt( var ) * Math.sqrt( 2.0 * Math.PI );
        }
        return 0.0;
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

    // Get the parameters error estimates.
    public double[] getPErrors()
    {
        return perrors;
    }

    // Set all the parameters errors, only used when setting parameters
    // and this is container class.
    public void setPErrors( double[] perrors )
    {
        this.perrors[0] = perrors[0];
        this.perrors[1] = perrors[1];
        this.perrors[2] = perrors[2];
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
            "(+/- " + getFluxError() + "), " +
            "scale = " + getScale() + ", " +
            "(+/- " + getScaleError() + "), " +
            "centre = " + getCentre() + ", " +
            "(+/- " + getCentreError() + "), " +
            "sigma = " + getSigma() +
            "(+/- " + getSigmaError() + "), " +
            "FWHM = " + getFWHM() +
            "(+/- " + getFWHMError() + "), " +
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


    //  Tester, use model function and random noise to test fitting
    //  errors. Uses indices as the coordinates, so scale values
    //  appropriately.
    public static void test( double scale, double centre, double sigma,
                             int npoints )
    {
        System.out.println( "Testing GaussianFitter:" );
        System.out.println( "   model parameters: scale = " + scale +
                            ", centre = " + centre +
                            ", sigma = " + sigma + 
                            ", npoints = " + npoints );

        //  Generate the model data.
        GaussianGenerator gen = new GaussianGenerator( scale, centre, sigma );
        double x[] = new double[npoints];
        double y[] = new double[npoints];
        for ( int i = 0; i < npoints; i++ ) {
            x[i] = (double) i;
            y[i] = gen.evalYData( x[i] );
        }

        //  Fit this.
        GaussianFitter fitter = new GaussianFitter( x, y, scale, centre, sigma );
        System.out.println( "   fit to model: scale = " + fitter.getScale() +
                            ", centre = " + fitter.getCentre() +
                            ", sigma = " + fitter.getSigma() );

        //  Monte-carlo estimation of noise.
        double scales[] = new double[50];
        double centres[] = new double[50];
        double sigmas[] = new double[50];
        double yn[] = new double[npoints];
        double w[] = new double[npoints];

        //  Gaussian sigma, 1/10 of the scale height.
        double gsig = scale * 0.1;
        double wgsig = 1.0 / ( gsig * gsig );

        for ( int i = 0; i < 50; i++ ) {
            for ( int j = 0; j < npoints; j++ ) {
                yn[j] = y[j] + cern.jet.random.Normal.staticNextDouble( 0.0, gsig );
                w[j] = wgsig;
            }
            fitter = new GaussianFitter( x, yn, w, scale, centre, sigma );
            scales[i] = fitter.getScale();
            centres[i] = fitter.getCentre();
            sigmas[i] = fitter.getSigma();
        }

        //  Estimates the errors.
        System.out.println( "Estimated errors: " );
        double sumsq = 0.0;
        for ( int i = 0; i < 50; i++ ) {
            sumsq += ( scale - scales[i] ) * ( scale - scales[i] );
        }
        System.out.println( "   scale sigma = " + Math.sqrt( sumsq / 50.0 ) );

        sumsq = 0.0;
        for ( int i = 0; i < 50; i++ ) {
            sumsq += ( centre - centres[i] ) * ( centre - centres[i] );
        }
        System.out.println( "   centre sigma = " + Math.sqrt( sumsq / 50.0 ) );

        sumsq = 0.0;
        for ( int i = 0; i < 50; i++ ) {
            sumsq += ( sigma - sigmas[i] ) * ( sigma - sigmas[i] );
        }
        System.out.println( "   sigma sigma = " + Math.sqrt( sumsq / 50.0 ) );


        //  And a full fit, with and without error estimates.
        System.out.println( "cf random weighted fit: " );

        fitter = new GaussianFitter( x, yn, w, scale, centre, sigma );
        System.out.println( "scale = " + fitter.getScale() + " +/- " + fitter.getScaleError() );
        System.out.println( "centre = " + fitter.getCentre() + " +/- " + fitter.getCentreError() );
        System.out.println( "sigma = " + fitter.getSigma() + " +/- " + fitter.getSigmaError() );

        System.out.println( "cf same unweighted fit: " );
        fitter = new GaussianFitter( x, yn, scale, centre, sigma );
        System.out.println( "scale = " + fitter.getScale() + " +/- " + fitter.getScaleError() );
        System.out.println( "centre = " + fitter.getCentre() + " +/- " + fitter.getCentreError() );
        System.out.println( "sigma = " + fitter.getSigma() + " +/- " + fitter.getSigmaError() );
    }
}
