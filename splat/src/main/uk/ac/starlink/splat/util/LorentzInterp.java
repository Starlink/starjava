/*
 * Some parts:
 *
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-NOV-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

/**
 * Interpolate values using a polynomial fit of some degree.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LorentzInterp
    extends LinearInterp
{
    /**
     * The LorentzGenerator. Does the real work.
     */
    protected LorentzGenerator generator = null;
    protected double base = 0.0;

    /**
     * Create an instance with no coordinates. A call to 
     * {@link setValues} must be made before any other methods.
     */
    public LorentzInterp()
    {
        //  Do nothing.
    }

    /**
     * Create an instance with the given coordinates. Interpolation 
     * is by X coordinate see the {@link interpolate}  method. 
     * The X coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public LorentzInterp( double[] x, double[] y )
    {
        setValues( x, y );
    }

    /**
     * Set the coordinates that define the Lorentzian.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public void setValues( double[] x, double[] y )
    {
        this.x = x;
        this.y = y;
        decr = false;
        if ( x.length > 2 ) {
            double scale = y[2] - y[1];
            double width = 0.1 * ( x[2] - x[1] );
            double centre = x[2];
            base = y[1];       
            generator = new LorentzGenerator( scale, centre, width );
        }
    }

    public void setCoords( double[] x, double[] y, boolean check )
    {
        setValues( x, y );
    }

    public double interpolate( double xp )
    {
        if ( x.length > 2 ) {
            return generator.evalPoint( xp ) + base;
        }
        return super.interpolate( xp );
    }
}
