/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
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
 * {@link #getChi} method.
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
     * Whether the parameters are fixed or floating.
     */
    protected boolean[] fixed = new boolean[3];

    // Access to parameters.
    public static final int SCALE = 0;
    public static final int CENTRE = 1;
    public static final int WIDTH = 2;

    /**
     * The chi square of the fit.
     */
    protected double chiSquare = 0.0;

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
                lm.setSigma( i + 1, 1.0 / w[i] );
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
     * Get the scale height of fit.
     */
    public double getScale()
    {
        return params[SCALE];
    }

    /**
     * Get the width of the fit.
     */
    public double getWidth()
    {
        return params[WIDTH];
    }

    /**
     * Get the integrated flux of the fit.
     */
    public double getFlux()
    {
        return Math.PI * 0.5 * params[SCALE] * params[WIDTH] *
               2.0 * Math.sqrt( 2.0 );
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
            "scale = " + getScale() + ", " +
            "centre = " + getCentre() + ", " +
            "width = " + getWidth() +
            "]";
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
}
