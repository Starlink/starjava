/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 *   Interpolate values using a linear scheme.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LinearInterp
    extends AbstractInterpolator
{
    /**
     * Create an instance with no coordinates. A call to 
     * {@link setValues} must be made before any other methods.
     */
    public LinearInterp()
    {
        //  Do nothing.
    }

    /**
     * Create an instance with the given coordinates.  Interpolation
     * is by X coordinate see the {@link interpolate} method. The X
     * coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public LinearInterp( double[] x, double[] y )
    {
        super( x, y );
    }

    public double interpolate( double xp )
    {
        //  Locate the position of xp.
        int[] bounds = binarySearch( x, xp );
        int klo = bounds[0];
        int khi = bounds[1];
        if ( khi == klo ) {
            //  Off ends.
            return y[klo];
        }

	double dx = xp - x[klo];
        double value = 0.0;
        if ( decr ) {
            value = ( y[klo] - y[khi]) * dx / ( x[klo] - x[khi] ) + y[khi];
        } 
        else {
            value = ( y[khi] - y[klo]) * dx / ( x[khi] - x[klo] ) + y[klo];
        }
        return value;
    }

    public int guessStep()
    {
        return 1;
    }

    /** Simple test entry point */
    public static void main( String[] args )
    {
        double[] x = new double[10];
        double[] y = new double[10];

        for ( int i = 0; i < 10; i++ ) {
            x[i] = i + 1;
            y[i] = Math.sin( i + 1 );
        }

        LinearInterp si = new LinearInterp( x, y );
        for ( int i = 0; i < 10; i++ ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( i + ", " + value );
        }

        for ( int i = 9; i >= 0; i-- ) {
            x[i] = i + 1;
            y[i] = Math.sin( i + 1 );
        }

        si = new LinearInterp( x, y );
        for ( int i = 9; i >= 0; i-- ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( i + ", " + value );
        }
    }
}
