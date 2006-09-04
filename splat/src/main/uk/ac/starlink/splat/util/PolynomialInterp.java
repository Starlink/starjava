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

import uk.ac.starlink.diva.interp.LinearInterp;

/**
 * Interpolate values using a polynomial fit of some degree.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PolynomialInterp
    extends LinearInterp
{
    /**
     * The degree of the polynomial being used. Quadratic by default.
     */
    protected int degree = 4;

    /**
     * The PolynomialFitter. Does the real work.
     */
    protected PolynomialFitter fitter = null;

    /**
     * Create an instance with no coordinates. A call to
     * {@link #setValues} must be made before any other methods.
     */
    public PolynomialInterp()
    {
        //  Do nothing.
    }

    /**
     * Create an instance with no coordinates, but ready to work
     * with a given degree polynomial.
     * A call to {@link #setValues} must be made before any other
     * methods.
     */
    public PolynomialInterp( int degree )
    {
        this.degree = degree;
    }

    /**
     * Create an instance with the given coordinates for the given
     * degree.  Interpolation is by X coordinate see the {@link #interpolate}
     * method. The X coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param degree the degree of polynomial.
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public PolynomialInterp( int degree, double[] x, double[] y )
    {
        setValues( degree, x, y, true );
    }

    /**
     * Set all the values.
     *
     * @param degree the degree of polynomial.
     * @param x the X coordinates.
     * @param y the Y coordinates.
     * @param check whether to check if X coordinates are increasing
     *              or decreasing, if no check accepts current state.
     */
    public void setValues( int degree, double[] x, double[] y, boolean check )
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
        this.degree = degree;
        if ( x.length >= degree ) {
            fitter = new PolynomialFitter( degree, x, y );
        }
    }

    public void setCoords( double[] x, double[] y, boolean check )
    {
        setValues( degree, x, y, check );
    }

    /**
     * Set the degree of the interpolating polynomial.
     *
     * @param degree the degree.
     */
    public void setDegree( int degree )
    {
        setValues( degree, x, y, false );
    }

    /**
     * Get the degree of polynomial.
     *
     * @return the degree.
     */
    public int getDegree()
    {
        return degree;
    }

    public double interpolate( double xp )
    {
        if ( x.length > degree ) {
            return fitter.evalYData( xp );
        }
        return super.interpolate( xp );
    }

    public double[] evalYDataArray( double[] xps )
    {
        if ( x.length > degree ) {
            return fitter.evalYDataArray( xps );
        }
        return super.evalYDataArray( xps );
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
        PolynomialInterp si = new PolynomialInterp( 5, x, y );

        for ( int i = 0; i < 10; i++ ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( value );
        }
    }
}
