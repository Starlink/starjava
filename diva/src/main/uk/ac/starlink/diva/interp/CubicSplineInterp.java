/*
 * Some parts:
 *
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 *   Interpolate values using a natural cubic spline.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class CubicSplineInterp
    extends LinearInterp
{
    /**
     * Create an instance with no coordinates. A call to 
     * {@link setCoords} must be made before any other methods.
     */
    public CubicSplineInterp()
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
    public CubicSplineInterp( double[] x, double[] y )
    {
        super( x, y );
    }

    public void setCoords( double[] x, double[] y, boolean check )
    {
        // See which way the coordinates increase. If not increasing
        // we need to create an inverted list.
        if ( check  && x.length >= 2 ) {
            if ( x[1] < x[0] ) {
                decr = true;
            }
            else {
                decr = false;
            }
        }
        if ( decr ) {
            this.x = new double[x.length];
            for ( int i = 0; i < x.length; i++ ) {
                this.x[i] = -x[i];
            }
        }
        else {
            this.x = x;
        }
        this.y = y;

        c = new double[x.length+1];
        if ( x.length > 2 ) {
            evalCoeffs();
        }
    }

    /**
     * Evaluate the coefficients for each position.
     */
    private void evalCoeffs()
    {
	int i, k;
	double ip, s, dx1, i_dx2;

	/* Assumes that n >= 4 and x is monotonically increasing */
        int n = x.length;
        double[] u = new double[n];

	c[1] = c[n] = u[1] = 0.0;
	for (i = 1; i < n-1; i++) {
            i_dx2 = 1.0 / (x[i+1] - x[i-1]);
            dx1 = x[i] - x[i-1];
            s = dx1 * i_dx2;
            ip = 1.0 / (s * c[i-1] + 2.0);
            c[i] = (s - 1.0) * ip;
            u[i] = (y[i+1] - y[i]) / (x[i+1] - x[i]) - (y[i] - y[i-1]) / dx1;
            u[i] = (6.0 * u[i] * i_dx2 - s * u[i-1]) * ip;
	}
	for (k = n-2; k >= 0; k--) {
            c[k] = c[k] * c[k+1] + u[k];
        }
	return;
    }

    public double interpolate( double xp )
    {
        if ( x.length > 2 ) {
            //  Locate the position of xp.
            if ( decr ) xp = -xp;
            int[] bounds = binarySearch( x, xp );
            int klo = bounds[0];
            int khi = bounds[1];
            if ( khi == klo ) {
                if ( khi == 0 ) {
                    return y[0];
                }
                klo = khi - 1;
            }
            double h, ih, b, a, yp;
            
            h = x[khi] - x[klo];
            ih = 1.0 / h;
            a = (x[khi] - xp) * ih;
            b = (xp - x[klo]) * ih;
            yp = a * y[klo] + b * y[khi] + 
                ((a*a*a - a) * c[klo] + (b*b*b - b) * c[khi]) * (h*h) / 6.0;
            return yp;
        }
        return super.interpolate( xp );
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
        CubicSplineInterp si = new CubicSplineInterp( x, y );

        for ( int i = 0; i < 10; i++ ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( value );
        }
    }
}
