/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     15-JAN-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.diva.interp;

/**
 * Interpolate values using a simple polynomial fit.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PolynomialInterp
    extends LinearInterp
{
    /**
     * Create an instance with no coordinates. A call to
     * {@link setValues} must be made before any other methods.
     */
    public PolynomialInterp()
    {
        //  Do nothing.
    }

    /**
     * Create an instance with the given coordinates.
     * Interpolation is by X coordinate see the {@link interpolate}
     * method. The X coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public PolynomialInterp( double[] x, double[] y )
    {
        setValues( x, y, true );
    }

    /**
     * Set all the values.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     * @param check whether to check if X coordinates are increasing
     *              or decreasing, if no check accepts current state.
     */
    public void setValues( double[] x, double[] y, boolean check )
    {
        this.x = x;
        this.y = y;
        if ( check ) {
            if ( x[1] < x[0] ) {
                decr = true;
            }
            else {
                decr = false;
            }
        }
        c = new double[x.length+1];
        if ( x.length >= 2 ) {
            evalCoeffs();
        }
    }

    public void setCoords( double[] x, double[] y, boolean check )
    {
        setValues( x, y, check );
    }

    
    public double interpolate( double xp )
    {
        if ( x.length >= 2 ) {
            return evalPolynomial( xp );
        }
        return y[0];
    }


    /**
     * Eval a polynomial by Horner's schema
     */
    public double evalPolynomial( double xp ) 
    {
        int n  = c.length - 1;
        double r = c[n];
        for ( int i = n - 1; i >= 0; i-- ) {
            r = c[i] + ( r * xp );
        }
        return r;
    }

    /**
     *  Compute coefficients of interpolating polynomial.
     */
    protected void evalCoeffs() 
    {
        int n = x.length - 1;
        double[] s = new double [n+1];
        for ( int i = 0; i < n; i++ ) {
            c[i] = 0.0;
            s[i] = 0.0;
        }
        s[n] = -x[0];
        for ( int i = 1; i <= n; i++ ) {
            for ( int j = n-i; j <= n-1; j++ ) {
                s[j] -= x[i] * s[j+1];
            }
            s[n] -= x[i];
        }
        for ( int j = 0; j <= n; j++ ) {
            double p = n + 1;
            for ( int k = n; k >= 1; k-- ) {
                p = k * s[k] + x[j] * p;
            }
            double ff = y[j] / p;
            double b = 1.0;
            for ( int k = n; k >= 0; k-- ) {
                c[k] += b * ff;
                b = s[k] + x[j] * b;
            }
        }
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
        PolynomialInterp si = new PolynomialInterp( x, y );

        for ( int i = 0; i < 10; i++ ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( value );
        }
    }
}
