package uk.ac.starlink.splat.util;

/**
 * LorentzFitter fits a lorentzian to a set of data points using a
 * non-linear weighted least squares fit (non-linear is required for
 * centering).  The relevant formula is:
 *
 *    y(radius) = scale / ( 1.0 + 0.5 * ( radius / width )**2 )
*
 * with FWHM = width * 2.0 * sqrt(2.0),
 * and radius = wavelength - centre of peak.
 *
 * To use this class create an instance with the data to be
 * fitted. Interpolated positions can then be obtained using the
 * evalArray() and evalPoint() methods. A chi squared residual to the
 * fit can be obtained from the getChi() method.
 *
 * @since $Date$
 * @since 03-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000-2001 Central Laboratory of the Research
 *            Councils
 */
public class LorentzFitter
    extends FunctionFitter
    implements LevMarqFunc
{
    /**
     * The scale factor.
     */
    private double scale = 1.0;

    /**
     * The width.
     */
    private double width = 1.0;

    /**
     * The centre.
     */
    private double centre = 0.0;

    /**
     * The chi square of the fit.
     */
    private double chiSquare = 0.0;

    /**
     * Fit a lorentzian to unweighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y. These are assumed precise.
     * @param scale initial estimate of the scale height.
     * @param centre initial estimate of the centre.
     * @param sigma initial estimate of the width.
     */
    public LorentzFitter( double[] x, double[] y, double scale,
                          double centre, double width )
    {
        this.scale = scale;
        this.centre = centre;
        this.width = width;

        // Default weights are 1.0.
        double[] w = new double[x.length];
        for (int i = 0; i < x.length; i++ ) {
            w[i] = 1.0;
        }
        doFit( x, y, w );
    }

    /**
     * Fit a lorentz to weighted data points.
     *
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights (i.e. inverse variances).
     * @param scale initial estimate of the scale height.
     * @param centre initial estimate of the centre.
     * @param sigma initial estimate of the width.
     */
    public LorentzFitter( double[] x, double[] y, double[] w,
                          double scale, double centre, double width )
    {
        this.scale = scale;
        this.centre = centre;
        this.width = width;
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
        lm.setParam( 1, scale );
        lm.setParam( 2, centre );
        lm.setParam( 3, width );

        //  And mimimise.
        lm.fitData();

        //  Record estimate of goodness of fit.
        chiSquare = lm.getChisq();

        //  And the fit parameters.
        scale = lm.getParam( 1 );
        centre = lm.getParam( 2 );
        width = lm.getParam( 3 );
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
     * Get the width of the fit.
     */
    public double getWidth()
    {
        return width;
    }

    /**
     * Get the integrated flux of the fit.
     */
    public double getFlux()
    {
        return Math.PI * 0.5 * scale * width * 2.0 * Math.sqrt( 2.0 );
    }

    /**
     * Evaluate the lorentzian at a single position.
     *
     * @param x X position at which to evaluate.
     * @return value at X
     */
    public double evalPoint( double x )
    {
        double radius = Math.abs( x - centre );
        return scale / ( 1.0 + 0.5 * ( radius * radius ) / ( width * width ) );
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
