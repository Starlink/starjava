/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * VoigtFitter fits a Voigt (real part of the complex error function)
 * function to a set of data points using a non-linear weighted least
 * squares fit.
 *
 * To use this class create an instance with the data to be
 * fitted. Interpolated positions can then be obtained using the
 * evalYDataArray() and evalYData() methods. A chi squared residual to
 * the fit can be obtained from the getChi() method.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class VoigtFitter 
    extends AbstractFunctionFitter
{
    /**
     * The function parameters.
     */
    protected double[] params = new double[4];

    /**
     * Whether the parameters are fixed or floating.
     */
    protected boolean[] fixed = new boolean[4];

    // Access to parameters.
    public static final int SCALE = 0;
    public static final int CENTRE = 1;
    public static final int GWIDTH = 2;
    public static final int LWIDTH = 3;

    /**
     * The peak height of error function (scale factor for scale).
     */
    protected double peak = 1.0;

    /**
     * The chi square of the fit.
     */
    protected double chiSquare = 0.0;

    /**
     * For sub-classes.
     */
    protected VoigtFitter() {}

    /**
     * Fit a Voigt function to unweighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y. These are assumed precise.
     * @param scale initial estimate of the scale height.
     * @param centre initial estimate of the centre.
     * @param gwidth initial estimate of the Gaussian width.
     * @param lwidth initial estimate of the Lorentzian width.
     */
    public VoigtFitter( double[] x, double[] y, double scale,
                        double centre, double gwidth, double lwidth )
    {
        this( x, y, null, scale, centre, gwidth, lwidth );
    }

    /**
     * Fit a Voigt to weighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances).
     * @param scale initial estimate of the scale height.
     * @param centre initial estimate of the centre.
     * @param gwidth initial estimate of the Gaussian width.
     * @param lwidth initial estimate of the Lorentzian width.
     */
    public VoigtFitter( double[] x, double[] y, double[] w,
                        double scale, double centre, double gwidth,
                        double lwidth )
    {
        this( x, y, null, scale, false, centre, false, gwidth, false, 
              lwidth, false );
    }

    /**
     * Fit a Voigt to weighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances, null for defaults).
     * @param scale initial estimate of the scale height.
     * @param scaleFixed is scale value fixed.
     * @param centre initial estimate of the centre.
     * @param centreFixed is centre value fixed.
     * @param gwidth initial estimate of the Gaussian width.
     * @param gwidthFixed is gwidth value fixed.
     * @param lwidth initial estimate of the Lorentzian width.
     * @param lwidthFixed is lwidth value fixed.
     */
    public VoigtFitter( double[] x, double[] y, double[] w,
                        double scale, boolean scaleFixed, 
                        double centre, boolean centreFixed, 
                        double gwidth, boolean gwidthFixed, 
                        double lwidth, boolean lwidthFixed )
    {
        params[SCALE] = scale;
        params[CENTRE] = centre;
        params[GWIDTH] = gwidth;
        params[LWIDTH] = lwidth;
        peak = 1.0;

        fixed[SCALE] = scaleFixed;
        fixed[CENTRE] = centreFixed;
        fixed[GWIDTH] = gwidthFixed;
        fixed[LWIDTH] = lwidthFixed;

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
        LevMarq lm = new LevMarq( this, x.length, 4 );

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
        lm.setParam( 3, params[GWIDTH], fixed[GWIDTH] );
        lm.setParam( 4, params[LWIDTH], fixed[LWIDTH] );

        //  Each solution is scaled by a re-normalisation factor
        //  (i.e. so that error function peak is 1).
        setPeak();

        //  And mimimise.
        lm.fitData();

        //  Record estimate of goodness of fit.
        chiSquare = lm.getChisq();
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
     * Get the Gaussian width of the fit.
     */
    public double getGWidth()
    {
        return params[GWIDTH];
    }

    /**
     * Get the Lorentzian width of the fit.
     */
    public double getLWidth()
    {
        return params[LWIDTH];
    }

    /**
     * Get if the scale height is held fixed.
     */
    public boolean getScaleFixed()
    {
        return fixed[SCALE];
    }

    /**
     * Get if the Gaussian width is held fixed.
     */
    public boolean getGWidthFixed()
    {
        return fixed[GWIDTH];
    }

    /**
     * Get if the Lorentzian width is held fixed.
     */
    public boolean getLWidthFixed()
    {
        return fixed[LWIDTH];
    }

    /**
     * Get if the centre is being held fixed.
     */
    public boolean getCentreFixed()
    {
        return fixed[CENTRE];
    }

    /**
     * Get the total flux of the fit.
     */
    public double getFlux()
    {
        return peak;
    }

    /**
     * Define the scaling factor that normalises the error function to
     * have a peak of 1, given the current parameters.
     */
    protected void setPeak()
    {
        peak = 1.0; // Value used in evalYData();
        peak = params[SCALE] / evalYData( params[CENTRE] );
    }

    /**
     * Get chi square of this fit.
     */
    public double getChi()
    {
        return chiSquare;
    }

    /**
     * Evaluate the Voigt function at a set of given X positions.
     *
     * @param x array of X positions at which to evaluate.
     * @return array of values at given X's.
     */
    public double[] evalYDataArray( double[] x )
    {
        double[] y = new double[x.length];
        double[] dyda = new double[5];
        for ( int i = 0; i < x.length; i++ ) {
            y[i] = fullEvalPoint( x[i], dyda );
        }
        return y;
    }

    /**
     * Evaluate the Voigt function at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalYData( double x )
    {
        double[] dyda = new double[5];
        return fullEvalPoint( x, dyda );
    }

    // Return the number of parameters used by this function.
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
        this.params[SCALE] = params[SCALE];
        this.params[CENTRE] = params[CENTRE];
        this.params[GWIDTH] = params[GWIDTH];
        this.params[LWIDTH] = params[LWIDTH];
        setPeak();
    }

    //  Get the fixed/floating state of parameters.
    public boolean[] getFixed()
    {
        return fixed;
    }

    // Set the fixed state of the various parameters.
    public void setFixed( boolean[] fixed )
    {
        this.fixed[SCALE] = fixed[SCALE];
        this.fixed[CENTRE] = fixed[CENTRE];
        this.fixed[GWIDTH] = fixed[GWIDTH];
        this.fixed[LWIDTH] = fixed[LWIDTH];
    }

    public String toString()
    {
        return "VoigtFitter[" + 
            "flux = " + getFlux() + ", " +
            "scale = " + getScale() + ", " +
            "centre = " + getCentre() + ", " +
            "gwidth = " + getGWidth() + ", " +
            "lwidth = " + getLWidth() +
            "]";
    }

    /**
     * Evaluate the Voigt function at a single position.  Also
     * calculates the partial derivatives of the Voigt function, with
     * respect to scale, position, and the two sigmas.
     *
     * The Voigt function is a convolution of a normalised Gaussian
     * and Lorentzian (Cauchy) function. The function is calculated by
     * noting that the Voigt function is, to within a scale factor
     * (which we characterise as a scale on the peak) equal to the
     * real part of the complex error function.
     *
     *  Original Fortran version by:
     *     W.I.F.David            6-JUN-84
     *     Neutron Division
     *     RAL ext. 5179
     *
     *
     * @param wavex input position (wavelength)
     * @param dyda partial derivates (as used for minimization).
     * @return the value of the function at the given position.
     *
     * Also returns an array of 5 doubles, dyda. These are the
     * normalised Voigt function value followed the partial derivates
     * scale, gaussian sigma, lorentzian sigma and position.
     */
    public double fullEvalPoint( double wavex, double[] dyda )
    {
        final double ovrtpi = 0.564189584;  // 1/sqrt(PI)
        final double ovrt2 = 0.707106781;   // 1/sqrt(2)

        double btem = ovrt2 / params[GWIDTH];
        double atem = ovrtpi * btem;
        double xx = ( wavex - params[CENTRE] ) * btem;
        double yy = 0.5 * params[LWIDTH] * btem;

        //  Evaluate voigt function, w[0] = real part, w[1] = imaginary.
        double w[] = voigt( xx, yy );
        double ctem = atem * btem;

        //  Value of normalised Voigt function.
        dyda[0] = atem * w[0];

        // Partials wrt to "x" and "y". See R.J.Wells, "Rapid
        // approximation to the Voigt/Faddeeva function and its
        // derivatives", which shows the formulae for these
        // parameterised forms.
        double dwrdx = 2.0 * ( yy * w[1] - xx * w[0] );
        double dwrdy = 2.0 * ( xx * w[1] + yy * w[0] - ovrtpi );
        if ( params[SCALE] < 0.0 ) {
            dwrdx *= -1.0;
            dwrdy *= -1.0;
        }

        //  Partials wrt to scale, position, gaussian width and
        //  lorentzian width.
        dyda[1] = dyda[0];
        dyda[2] = peak * -dwrdx * ctem;
        dyda[3] = peak * -atem * ( w[0] + dwrdx * xx + dwrdy * yy ) / 
                  params[GWIDTH];
        dyda[4] = peak * 0.5 * ctem * dwrdy;

        //  Return value correct to a voigt peak of 1.
        return peak * dyda[0];
    }

    /**
     * Evaluate the Voigt given a set of model parameters. Also
     * evaluates the partial derivates of the current fit.
     *
     * Implementation of the LevMarqFunc interface.
     */
    public double eval( double x, double[] a, int na, double[] dyda )
    {
        params[SCALE] = a[1];
        params[CENTRE] = a[2];
        params[GWIDTH] = a[3];
        params[LWIDTH] = a[4];

        //  Each solution is uniquely scaled by a re-normalisation factor
        //  (i.e. so that peak is 1).
        setPeak();
        double y = fullEvalPoint( x, dyda );
        if ( a[3] < 0.0 || a[4] < 0.0 ) {
            //  Try to discourage non-physical solutions by increasing
            //  chi square calcs.
            return 0.0;
        }
        return y;
    }

    /**
     * Compute the real (Voigt function) and imaginary parts of the
     * complex function w(z)=exp(-z**2)*erfc(-i*z) in the upper
     * half-plane z=x+iy.  The maximum relative error of the real part
     * is 2E-6 and the imaginary part is 5E-6.
     *
     * From: Humlicek, J. Quant. Spectrosc. Radiat. Transfer, V21,
     *       p309, 1979.  
     */
    protected double[] voigt( double xarg, double yarg )
    {
        double wr, wi;
        double[] result = new double[2];
        int	i;
        double x, y, y1, y2, y3, d, d1, d2, d3, d4, r, r2;

        double [] t = {0.314240376, 0.947788391, 1.59768264, 2.27950708,
                       3.02063703, 3.8897249};
        double[] c = {1.01172805, -0.75197147, 1.2557727e-2, 1.00220082e-2,
                      -2.42068135e-4, 5.00848061e-7};
        double[] s = {1.393237, 0.231152406, -0.155351466, 6.21836624e-3,
                      9.19082986e-5, -6.27525958e-7};

	x = xarg;
	y = Math.abs( yarg );
	wr = 0.0;
	wi = 0.0;
	y1 = y + 1.5;
	y2 = y1 * y1;

	if ( y < 0.85 && Math.abs( x ) > 18.1 * y + 1.65 ) {

            // Region II
	    if ( Math.abs( x ) < 12 ) {
		wr = Math.exp (-x * x);
            }
	    y3 = y + 3.0;
            for ( i = 0; i < 6; i++ ) {
		r = x - t[i];
		r2 = r * r;
		d = 1.0 / (r2 + y2);
		d1 = y1 * d;
		d2 = r * d;
		wr = wr + y * (c[i] * (r * d2 - 1.5 * d1) + s[i] * y3 * d2) /
		    (r2 + 2.25);
		r = x + t[i];
		r2 = r * r;
		d = 1 / (r2 + y2);
		d3 = y1 * d;
		d4 = r * d;
		wr = wr + y * (c[i] * (r * d4 - 1.5 * d3) - s[i] * y3 * d4) /
		    (r2 + 2.25);
		wi = wi + c[i] * (d2 + d4) + s[i] * (d1 - d3);
            }

	} else {

            // Region I
            for ( i = 0; i < 6; i++ ) {
		r = x - t[i];
		d = 1 / (r * r + y2);
		d1 = y1 * d;
		d2 = r * d;
		r = x + t[i];
		d = 1 / (r * r + y2);
		d3 = y1 * d;
		d4 = r * d;
		wr = wr + c[i] * (d1 + d3) - s[i] * (d2 - d4);
		wi = wi + c[i] * (d2 + d4) + s[i] * (d1 - d3);
	    }
	}
        result[0] = wr;
        result[1] = wi;
        return result;
    }
}
