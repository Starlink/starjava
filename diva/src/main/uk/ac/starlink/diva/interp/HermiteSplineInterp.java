/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 *   Spline interpolate a series of points using a scheme based on
 *   Hermite polynomials.
 *   <p>
 *   Based on the the code from G. Hill, Publ. DAO, vol 16, no. 6 (1982)
 *   which uses US Airforce Surveys in Geophysics no. 272 as its source.
 *   <p>
 *   The effect is supposed to construct reasonable analytic curves
 *   through discrete data points (i.e. like those a human would
 *   produce).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class HermiteSplineInterp
    extends LinearInterp
{
    /**
     * Create an instance with no coordinates. A call to 
     * {@link setCoords} must be made before any other methods.
     */
    public HermiteSplineInterp()
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
    public HermiteSplineInterp( double[] x, double[] y )
    {
        super( x, y );
    }

    public double interpolate( double xp )
    {
        // When we have too few points we use linear interpolation.
        if ( x.length > 2 ) {
            double p = 0.0;
            double lp1 = 0.0;
            double lp2 = 0.0;
            double l1 = 0.0;
            double l2 = 0.0;
            double yp1 = 0.0;
            double yp2 = 0.0;
            double xpi1 = 0.0;
            double xpi = 0.0;
            
            int n = Math.min( x.length, y.length );
            
            //  Off the "top" or "bottom" returns an end value.
            if ( ( xp >= x[n-1] && ! decr ) || ( xp <= x[n-1] && decr ) ) {
                return y[n-1];
            }
            else if ( ( xp <= x[0] && ! decr ) || ( xp >= x[0] && decr ) ) {
                return y[0];
            }
            
            //  Locate xp in x.
            int[] bounds = binarySearch( x, xp );
            int i = bounds[0];
            
            //  Interpolate.
            lp1 = 1.0 / ( x[i] - x[i+1] );
            lp2 = 1.0 / ( x[i+1] - x[i] );
            
            if ( i == 0 ) {
                yp1 = ( y[1] - y[0] ) / ( x[1] - x[0] );
            }
            else {
                yp1 = ( y[i+1] - y[i-1] ) / ( x[i+1] - x[i-1] );
            }
            
            if ( i >= n - 2 ) {
                yp2 = ( y[n-1] - y[n-2] ) / ( x[n-1] - x[n-2] );
            }
            else {
                yp2 = ( y[i+2] - y[i] ) / ( x[i+2] - x[i] );
            }
            xpi1 = xp - x[i+1];
            xpi = xp - x[i];
            l1 = xpi1 * lp1;
            l2 = xpi * lp2;
            p = y[i] * ( 1.0 - 2.0 * lp1 * xpi ) * l1 * l1 +
                y[i+1]*( 1.0 - 2.0 * lp2 * xpi1 ) * l2 * l2 +
                yp2 * xpi1 * l2 * l2 +
                yp1 * xpi * l1 * l1;
            
            return p;
        }
        return super.interpolate( xp );
    }

    /** Simple test entry point. */
    public static void main( String[] args )
    {
        double[] x = new double[10];
        double[] y = new double[10];

        for ( int i = 0; i < 10; i++ ) {
            x[i] = i + 1;
            y[i] = Math.sin( i + 1 );
        }
        HermiteSplineInterp si = new HermiteSplineInterp( x, y );

        for ( int i = 0; i < 10; i++ ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( value );
        }
    }
}
