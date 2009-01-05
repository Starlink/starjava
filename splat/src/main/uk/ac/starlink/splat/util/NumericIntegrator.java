/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     05-SEP-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Numerically integrate a function of unevenly spaced data. Based on
 * the AVINT routine of INTLIB/ACM.
 *
 * The quadrature method uses overlapping parabolas and smoothing.
 * References:
 * <pre>
 *    Philip Davis, Philip Rabinowitz,
 *    Methods of Numerical Integration,
 *    Blaisdell Publishing, 1967.
 *
 *    Paul Hennion,
 *    Algorithm 77: Interpolation, Differentiation and Integration,
 *    Communications of the Association for Computing Machinery,
 *    Volume 5, page 96, 1962.
 * </pre>
 * The error analysis for AVINT can be found in:
 * <pre>
 *    http://www.ece.unm.edu/summa/notes/Mathematics/0015.pdf
 * </pre>
 * which says that a typical error is O(h^3), where h is the spacing.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class NumericIntegrator
{
    private double[] xtab;
    private double[] ytab;
    private double integral = Double.MIN_VALUE;

    /**
     *  Create an instance. Note use the {@link setData} method to define the
     *  values to integrate and {@link getIntegral} to determine the value.
     */
    public NumericIntegrator()
    {
        //  Nothing to do.
    }

    /**
     * Set the coordinates and data values to be used.
     *
     *  @param coords the array of coordinates, one for each data value.
     *  @param data the array of data values associated with each coordinate.
     */
    public void setData( double[] coords, double[] data )
    {
        if ( coords.length != data.length ) {
            throw new
                RuntimeException( "Data arrays must have equal lengths" );
        }
        if ( coords.length < 2 ) {
            throw new
                RuntimeException( "Data arrays must have at least 2 entries" );
        }

        //  Coordinates must be monotonic for this function.
        //  ?? And increasing ??
        if ( coords[0] < coords[1] ) {
            for ( int i = 0; i < coords.length - 1; i++ ) {
                if ( coords[ i ] >= coords[ i + 1 ] ) {
                    throw new
                        RuntimeException( "Coordinates are not monotonic" );
                }
            }
        }
        else {
            for ( int i = 0; i < coords.length - 1; i++ ) {
                if ( coords[ i ] <= coords[ i + 1 ] ) {
                    throw new
                        RuntimeException( "Coordinates are not monotonic" );
                }
            }
        }
        xtab = coords;
        ytab = data;

        //  Change of data so need a new value for integral.
        integral = Double.MIN_VALUE;
    }

    /**
     *  Get the integral.
     */
    public double getIntegral()
    {
        //  Check that setData has been called.
        if ( xtab == null || ytab == null ) {
            return 0.0;
        }

        //  If already calculated avoid doing this again.
        if ( integral != Double.MIN_VALUE ) {
            return integral;
        }

        double a = 0.0;
        double b = 0.0;
        double ba = 0.0;
        double bb = 0.0;
        double bc = 0.0;
        double ca = 0.0;
        double cb = 0.0;
        double cc = 0.0;
        double syl2 = 0.0;
        double syl3 = 0.0;
        double syl = 0.0;
        double syu2 = 0.0;
        double syu3 = 0.0;
        double syu = 0.0;
        double term1 = 0.0;
        double term2 = 0.0;
        double term3 = 0.0;
        double total = 0.0;
        double x12 = 0.0;
        double x13 = 0.0;
        double x1 = 0.0;
        double x23 = 0.0;
        double x2 = 0.0;
        double x3 = 0.0;
        int i = 0;
        int istart = 0;
        int istop = 0;
        int ntab = 0;

        ntab = xtab.length;

        //  Special case for 2 elements.
        if ( ntab == 2 ) {
            integral = 0.5 * ( ytab[0] + ytab[1] ) * ( xtab[1] - xtab[0] );

            //  Correct for coordinates running backwards.
            if ( xtab[0] > xtab[1] ) {
                integral = -integral;
            }
            return integral;
        }

        integral = 0.0;
        a = xtab[0];
        b = xtab[ntab-1];

        istart = 1;
        istop = ntab - 1;

        total = 0.0;

        syl = a;
        syl2 = syl * syl;
        syl3 = syl2 * syl;

        for ( i = istart; i < istop; i++ ) {
            x1 = xtab[i-1];
            x2 = xtab[i];
            x3 = xtab[i+1];

            x12 = x1 - x2;
            x13 = x1 - x3;
            x23 = x2 - x3;

            term1 =   ( ytab[i-1] ) / ( x12 * x13 );
            term2 = - ( ytab[i]   ) / ( x12 * x23 );
            term3 =   ( ytab[i+1] ) / ( x13 * x23 );

            ba = term1 + term2 + term3;
            bb = - ( x2 + x3 ) * term1 -
                   ( x1 + x3 ) * term2 -
                   ( x1 + x2 ) * term3;
            bc = x2 * x3 * term1 +
                 x1 * x3 * term2 +
                 x1 * x2 * term3;

            if ( i == istart ) {
                ca = ba;
                cb = bb;
                cc = bc;
            }
            else {
                ca = 0.5 * ( ba + ca );
                cb = 0.5 * ( bb + cb );
                cc = 0.5 * ( bc + cc );
            }

            syu = x2;
            syu2 = syu * syu;
            syu3 = syu2 * syu;

            total = total + ca * ( syu3 - syl3 ) / 3.0 +
                            cb * ( syu2 - syl2 ) / 2.0 +
                            cc * ( syu  - syl );
            ca = ba;
            cb = bb;
            cc = bc;

            syl  = syu;
            syl2 = syu2;
            syl3 = syu3;
        }

        syu = b;
        syu2 = syu * syu;
        syu3 = syu2 * syu;

        integral = total + ca * ( syu3 - syl3 ) / 3.0 +
                           cb * ( syu2 - syl2 ) / 2.0 +
                           cc * ( syu  - syl  );

        //  Coordinates running backwards gives negative value.
        if ( xtab[0] > xtab[1] ) {
            integral = -integral;
        }
        return integral;
    }

    public String toString()
    {
        return "Integral = " + getIntegral();
    }

    public static void main( String[] args )
    {
        double[] values = new double[101];
        double[] coords = new double[101];

        GaussianGenerator g = new GaussianGenerator( 100.0, 50.0, 10.0 );

        //  Even spacing.
        System.out.println( "Even spacing" );
        for ( int i = 0; i < coords.length; i++ ) {
            coords[i] = (double) i;
            values[i] = g.evalYData( coords[i] );
        }

        NumericIntegrator integrator = new NumericIntegrator();
        integrator.setData( coords, values );
        double integ = integrator.getIntegral();
        System.out.println( "Integral = " + integ );
        double flux = g.getFlux();
        System.out.println( "Flux = " + flux );
        System.out.println( "Error = " + ( flux - integ ) );

        //  Uneven spacing.
        System.out.println( "Uneven spacing" );
        for ( int i = 0; i < coords.length; i++ ) {
            coords[i] = (double) ( i ) * Math.sqrt( i );
            values[i] = g.evalYData( coords[i] );
        }

        integrator.setData( coords, values );
        integ = integrator.getIntegral();
        System.out.println( "Integral = " + integ );
        flux = g.getFlux();
        System.out.println( "Flux = " + flux );
        System.out.println( "Error = " + ( flux - integ ) );

        //  Very uneven spacing.
        System.out.println( "Very uneven spacing" );
        for ( int i = 0; i < coords.length; i++ ) {
            coords[i] = (double) ( i * i );
            values[i] = g.evalYData( coords[i] );
        }

        integrator.setData( coords, values );
        integ = integrator.getIntegral();
        System.out.println( "Integral = " + integ );
        flux = g.getFlux();
        System.out.println( "Flux = " + flux );
        System.out.println( "Error = " + ( flux - integ ) );

        //  Even spacing, coords running backwards.
        System.out.println( "Even spacing, coords backwards" );
        for ( int i = 0, j = coords.length - 1; i < coords.length; i++, j-- ) {
            coords[i] = (double) j;
            values[i] = g.evalYData( coords[i] );
        }

        integrator = new NumericIntegrator();
        integrator.setData( coords, values );
        integ = integrator.getIntegral();
        System.out.println( "Integral = " + integ );
        flux = g.getFlux();
        System.out.println( "Flux = " + flux );
        System.out.println( "Error = " + ( flux - integ ) );
    }
}
