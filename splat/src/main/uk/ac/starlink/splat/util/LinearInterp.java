/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

/**
 *   Interpolate values using a linear scheme.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LinearInterp
    extends Interpolator
{
    /**
     * Create an instance with the given coordinates and values.
     * Interpolation is by coordinate producing a new value using the
     * {@link interpolate} method. The coordinates should be
     * monotonic, either increasing or decreasing. Same value
     * coordinates are not allowed.
     *
     * @param x the coordinates to be interpolated.
     * @param y the values of the coordinates.
     */
    public LinearInterp( double[] x, double[] y )
    {
        super( x, y );
    }

    public void setValues( double[] x, double[] y )
    {
        // See which way the coordinates increase.
        if ( x[1] < x[0] ) {
            decr = true;
        }
        else {
            decr = false;
        }
        this.x = x;
        this.y = y;
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
