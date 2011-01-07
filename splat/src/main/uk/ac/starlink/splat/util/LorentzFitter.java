/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     03-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * LorentzFitter fits a lorentzian to a set of data points using a
 * non-linear weighted least squares fit (non-linear is required for
 * centering).  The relevant formula is:
 * <pre>
 *    y(radius) = scale / ( 1.0 + 0.5 * ( radius / width )**2 )
 * </pre>
 * with <code>FWHM = width * 2.0 * sqrt(2.0)</code>,
 * and <code>radius = wavelength - centre</code> of peak.
 * <p>
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
public class LorentzFitter
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
    public static final int WIDTH = 2;

    // Useful conversion factor, width to FWHM.
    public static final double FWHMFAC = 2.0 * Math.sqrt( 2.0 );

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
    protected LorentzFitter() {}

    /**
     * Fit a lorentzian to unweighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y. These are assumed precise.
     * @param scale initial estimate of the scale height.
     * @param centre initial estimate of the centre.
     * @param width initial estimate of the width.
     */
    public LorentzFitter( double[] x, double[] y, double scale,
                          double centre, double width )
    {
        this( x, y, null, scale, centre, width );
    }

    /**
     * Fit a lorentz to weighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances).
     * @param scale initial estimate of the scale height.
     * @param centre initial estimate of the centre.
     * @param width initial estimate of the width.
     */
    public LorentzFitter( double[] x, double[] y, double[] w,
                          double scale, double centre, double width )
    {
        this( x, y, w, scale, false, centre, false, width, false );
    }

    /**
     * Fit a Lorentz to weighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances).
     * @param scale initial estimate of the scale height.
     * @param scaleFixed is scale value fixed.
     * @param centre initial estimate of the centre.
     * @param centreFixed is centre value fixed.
     * @param width initial estimate of the width.
     * @param widthFixed is width value fixed.
     */
    public LorentzFitter( double[] x, double[] y, double[] w,
                          double scale, boolean scaleFixed,
                          double centre, boolean centreFixed,
                          double width, boolean widthFixed )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[WIDTH] = width;
        fixed[SCALE] = scaleFixed;
        fixed[CENTRE] = centreFixed;
        fixed[WIDTH] = widthFixed;
        unitweights = false;
        perrors[SCALE] = 0.0;
        perrors[CENTRE] = 0.0;
        perrors[WIDTH] = 0.0;

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
     * Perform the fit of the data.
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
        lm.setParam( 3, params[WIDTH], fixed[WIDTH] );

        //  And minimise.
        lm.fitData();

        //  Record estimate of goodness of fit.
        chiSquare = lm.getChisq();

        //  And the fit parameters.
        params[SCALE] = lm.getParam( 1 );
        params[CENTRE] = lm.getParam( 2 );
        params[WIDTH] = lm.getParam( 3 );

        //  Estimated errors of the parameters.
        perrors[SCALE] = lm.getError( 1, unitweights );
        perrors[CENTRE] = lm.getError( 2, unitweights );
        perrors[WIDTH] = lm.getError( 3, unitweights );
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
     * Get the width of the fit.
     */
    public double getWidth()
    {
        return params[WIDTH];
    }

    /**
     * Get the error in width of fit.
     */
    public double getWidthError()
    {
        return perrors[WIDTH];
    }

    /**
     * Get if the scale height is held fixed.
     */
    public boolean getScaleFixed()
    {
        return fixed[SCALE];
    }

    /**
     * Get if the width is held fixed.
     */
    public boolean getWidthFixed()
    {
        return fixed[WIDTH];
    }

    /**
     * Get if the centre is being held fixed.
     */
    public boolean getCentreFixed()
    {
        return fixed[CENTRE];
    }

    /**
     * Get the integrated flux of the fit.
     */
    public double getFlux()
    {
        return Math.PI * 0.5 * params[SCALE] * params[WIDTH] * FWHMFAC;
    }

    /**
     * Get the estimated error in the integrated flux.
     */
    public double getFluxError()
    {
        //  Sum of the fractional errors, as variance is:
        double var = ( perrors[SCALE]*perrors[SCALE] * params[WIDTH]*params[WIDTH] +
                       perrors[WIDTH]*perrors[WIDTH] * params[SCALE]*params[SCALE] );
        if ( var > 0.0 ) {
            return Math.sqrt( var ) * Math.PI * 0.5 * FWHMFAC;
        }
        return 0.0;
    }

    /**
     * Evaluate the lorentzian at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalYData( double x )
    {
        double radius = Math.abs( x - params[CENTRE] );
        return params[SCALE] /
            ( 1.0 + 0.5 * (radius*radius) / (params[WIDTH]*params[WIDTH]) );
    }

    // Return the number of parameters that are used.
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
        return "LorentzFitter[" +
            "flux = " + getFlux() + ", " +
            "(+/- " + getFluxError() + ") " +
            "scale = " + getScale() + ", " +
            "(+/- " + getScaleError() + ") " +
            "centre = " + getCentre() + ", " +
            "(+/- " + getCentreError() + ") " +
            "width = " + getWidth() +
            "(+/- " + getWidthError() + ") " +
            "]";
    }

    /**
     * Get the width as a FWHM.
     */
    public double getFWHM()
    {
        return getWidth() * FWHMFAC;
    }

    /**
     * Get the error in the width as a FWHM.
     */
    public double getFWHMError()
    {
        return getWidthError() * FWHMFAC;
    }

//
// Implementation of the LevMarqFunc interface.
//
    /**
     * Evaluate the lorentzian given a set of model parameters. Also
     * evaluates the partial derivates of the current fit.
     */
    public double eval( double x, double[] a, int na, double[] dyda )
    {
        //  Radius.
        double radius = x - a[2];
        double radsq = radius * radius;

        //  Width squared.
        double widsq = a[3] * a[3];

        //  General quotient term for all partial derivatives.
        double invquo = 1.0 / ( 1.0 + 0.5 * radsq / widsq );
        double invquosq = invquo * invquo;

        //  d/d(scale)
        dyda[1] = invquo;

        //  d/d(centre)
        dyda[2] = a[1] * radius / widsq * invquosq;

        //  d/d(width)
        dyda[3] = a[1] * radsq / ( widsq *a[3] ) * invquosq;

        return a[1] * invquo;
    }



//  Tester, use model function and random noise to test fitting
//  errors. Uses indices as the coordinates, so scale values
//  appropriately.
    public static void test( double scale, double centre, double width,
                             int npoints )
    {
        System.out.println( "Testing LorentzFitter:" );
        System.out.println( "   model parameters: scale = " + scale +
                            ", centre = " + centre +
                            ", width = " + width + 
                            ", npoints = " + npoints );

        //  Generate the model data.
        LorentzGenerator gen = new LorentzGenerator( scale, centre, width );
        double x[] = new double[npoints];
        double y[] = new double[npoints];
        for ( int i = 0; i < npoints; i++ ) {
            x[i] = (double) i;
            y[i] = gen.evalYData( x[i] );
        }

        //  Fit this.
        LorentzFitter fitter = new LorentzFitter( x, y, scale, centre, width );
        System.out.println( "   fit to model: scale = " + fitter.getScale() +
                            ", centre = " + fitter.getCentre() +
                            ", width = " + fitter.getWidth() );

        //  Monte-carlo estimation of noise.
        double scales[] = new double[50];
        double centres[] = new double[50];
        double widths[] = new double[50];
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
            fitter = new LorentzFitter( x, yn, w, scale, centre, width );
            scales[i] = fitter.getScale();
            centres[i] = fitter.getCentre();
            widths[i] = fitter.getWidth();
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
            sumsq += ( width - widths[i] ) * ( width - widths[i] );
        }
        System.out.println( "   width sigma = " + Math.sqrt( sumsq / 50.0 ) );


        //  And a full fit, with and without error estimates.
        System.out.println( "cf random weighted fit: " );

        fitter = new LorentzFitter( x, yn, w, scale, centre, width );
        System.out.println( "scale = " + fitter.getScale() + " +/- " + fitter.getScaleError() );
        System.out.println( "centre = " + fitter.getCentre() + " +/- " + fitter.getCentreError() );
        System.out.println( "width = " + fitter.getWidth() + " +/- " + fitter.getWidthError() );

        System.out.println( "cf same unweighted fit: " );
        fitter = new LorentzFitter( x, yn, scale, centre, width );
        System.out.println( "scale = " + fitter.getScale() + " +/- " + fitter.getScaleError() );
        System.out.println( "centre = " + fitter.getCentre() + " +/- " + fitter.getCentreError() );
        System.out.println( "width = " + fitter.getWidth() + " +/- " + fitter.getWidthError() );
    }

}
