/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-MAR-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.LinearInterp;

/**
 * Creates {@link Interpolator}s for drawing interactive Voigt profiles.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class VoigtInterp
    extends LinearInterp
{
    /**
     * The VoigtGenerator. Does the real work.
     */
    protected VoigtGenerator generator = null;
    protected double base = 0.0;

    /**
     * Create an instance with no coordinates. A call to
     * {@link #setValues} must be made before any other methods.
     */
    public VoigtInterp()
    {
        //  Do nothing.
    }

    public int stepGuess()
    {
        return 101;
    }

    /**
     * Create an instance with the given coordinates. Interpolation
     * is by X coordinate see the {@link #interpolate}  method.
     * The X coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public VoigtInterp( double[] x, double[] y )
    {
        setValues( x, y );
    }

    public void appendValue( double newx, double newy )
    {
        //  Once we have enough stop appending.
        if ( x.length < 5 ) {
            super.appendValue( newx, newy );
        }
    }

    public boolean isFull()
    {
        if ( x.length < 5 ) {
            return false;
        }
        return true;
    }

    /**
     * Set the coordinates that define the Voigt profile.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public void setValues( double[] x, double[] y )
    {
        //  Keep old positions, if given too many or they don't match.
        if ( x.length > 5 || ( x.length != y.length ) ) {
            x = null;
            y = null;
            return;
        }

        //  When given 2 positions, use these to bootstrap a complete
        //  curve. The first position is taken as at the "FWHM" and second at
        //  the peak.
        if ( x.length == 2 ) {

            this.x = new double[5];
            this.y = new double[5];

            // Anchor.
            this.x[0] = x[1] - 2.0 * ( x[1] - x[0] );

            // Gaussian FWHM
            this.x[1] = x[0];

            //  Center.
            this.x[2] = x[1];

            //  Lorentz FWHM.
            this.x[3] = x[1] + ( x[1] - x[0] );

            // Anchor.
            this.x[4] = x[1] + 2.0 * ( x[1] - x[0] );

            this.y[0] = y[1] - 2.0 * ( y[1] - y[0] );
            this.y[1] = y[0];
            this.y[2] = y[1];
            this.y[3] = y[0];
            this.y[4] = this.y[0];

            x = this.x;
            y = this.y;
        }

        this.x = x;
        this.y = y;
        decr = false;

        //  Complete set of positions, derive the Voigt profile.
        //  Center is middle position, gaussian width, before lorentzian
        //  after.
        if ( x.length > 4 ) {

            //  Width is FWHM. Scale goes +/- according to absorption/emission.
            double scale = 2.0 * ( y[2] - y[1] );
            double gwidth = Math.abs( 2.0 * ( x[1] - x[2] ) /
                                      GaussianGenerator.FWHMFAC );
            double lwidth = Math.abs( 2.0 * ( x[3] - x[2] ) /
                                      LorentzGenerator.FWHMFAC );
            double centre = x[2];

            base = y[2] - scale;
            generator = new VoigtGenerator( scale, centre, gwidth, lwidth );
        }
    }

    public void setCoords( double[] x, double[] y, boolean check )
    {
        setValues( x, y );
    }

    public double interpolate( double xp )
    {
        if ( x.length > 4 ) {
            return generator.evalYData( xp ) + base;
        }
        return super.interpolate( xp );
    }
}
