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
 *   Interpolate a series of points using a scheme based Akima's splines.
 *   <p>
 *   The effect is supposed to construct reasonable analytic curves
 *   through discrete data points (i.e. like those a human would
 *   produce).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AkimaSplineInterp
    extends LinearInterp
{
    /**
     * Create an instance with no coordinates. A call to 
     * {@link setCoords} must be made before any other methods.
     */
    public AkimaSplineInterp()
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
    public AkimaSplineInterp( double[] x, double[] y )
    {
        super( x, y );
    }

    public void setCoords( double[] x, double[] y, boolean check ) 
    {
        // See which way the X coordinates increase. If not increasing
        // we need to create an inverted list.
        if ( check && x.length >= 2 ) {
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

        c = new double[3*x.length];
        if ( x.length > 3 ) {
            evalCoeffs();
        }
    }

    /**
     * Evaluate the coefficients for each position.
     */
    private void evalCoeffs()
    {
        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         * Translation of original c version from Grace:
         *
         * AKIMA computes the coefficients for a quasi-cubic hermite spline.
         * Same algorithm as in the IMSL library.
         * Programmer:	Paul Wessel
         * Date:	16-JAN-1987
         * Ver:		v.1-pc
         * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         */
	int i, no;
	double t1, t2, b, rm1, rm2, rm3, rm4;

        int nx = Math.min( x.length, y.length );

	/* Assumes that n >= 4 and x is monotonically increasing */
	rm3 = (y[1] - y[0])/(x[1] - x[0]);
	t1 = rm3 - (y[1] - y[2])/(x[1] - x[2]);
	rm2 = rm3 + t1;
	rm1 = rm2 + t1;

	/* get slopes */
	no = nx - 2;
	for (i = 0; i < nx; i++) {
            if (i >= no) {
                rm4 = rm3 - rm2 + rm3;
            }
            else {
                rm4 = (y[i+2] - y[i+1])/(x[i+2] - x[i+1]);
            }
            t1 = Math.abs(rm4 - rm3);
            t2 = Math.abs(rm2 - rm1);
            b = t1 + t2;
            if ( b != 0.0 ) {
                c[3*i] = (t1*rm2 + t2*rm3) / b;
            }
            else {
                c[3*i] = 0.5*(rm2 + rm3);

            }
            rm1 = rm2;
            rm2 = rm3;
            rm3 = rm4;
	}
	no = nx - 1;

	/* compute the coefficients for the nx-1 intervals */
	for (i = 0; i < no; i++) {
            t1 = 1.0 / (x[i+1] - x[i]);
            t2 = (y[i+1] - y[i])*t1;
            b = (c[3*i] + c[3*i+3] - t2 - t2)*t1;
            c[3*i+2] = b*t1;
            c[3*i+1] = -b + (t2 - c[3*i])*t1;
	}
	return;
    }

    public double interpolate( double xp )
    {
        if ( x.length > 3 ) {
            //  Locate the position of xp.
            if ( decr ) xp = -xp;
            int[] bounds = binarySearch( x, xp );
            int i = bounds[0];
        
            double dx = xp - x[i];
            double value = ( (c[3*i+2]*dx + c[3*i+1])*dx + c[3*i])*dx + y[i];
            if ( decr ) value = -value;
            
            return value;
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
        AkimaSplineInterp si = new AkimaSplineInterp( x, y );

        for ( int i = 0; i < 10; i++ ) {
            double value = si.interpolate( i + 1.25 );
            System.out.println( value );
        }
    }
}
