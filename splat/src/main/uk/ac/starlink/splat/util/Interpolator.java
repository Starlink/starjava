/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import uk.ac.starlink.splat.data.AnalyticSpectrum;

/**
 *  Abstract superclass for all interpolators.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public abstract class Interpolator
    implements AnalyticSpectrum
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
    public Interpolator( double[] x, double[] y )
    {
        // Only constructor so sub-classes must implement this.
        setValues( x, y );
    }

    /**
     * The ordinates. These are usually coordinates and must be
     * monotonic.
     */
    protected double[] x;

    /**
     * The data values to be interpolated between the ordinates. There
     * should be at least as many of these values as ordinates.
     */
    protected double[] y;

    /**
     * Whether the ordinates are monotonically decreasing.
     */
    protected boolean decr = false;

    /**
     * Set the ordinates and values used by this interpolator.
     *
     * @param x the coordinates to be interpolated.
     * @param y the values of the coordinates.
     */
    public abstract void setValues( double[] x, double[] y );

    /**
     * Get the ordinates.
     * 
     * @return the ordinates.
     */
    public double[] getOrdinates()
    {
        return x;
    }

    /**
     * Get the data values.
     *
     * @return the data values
     */
    public double[] getDataValues()
    {
        return y;
    }

    /**
     * Return the interpolated value corresponding to some arbitrary
     * ordinate.
     *
     * @param xp the ordinate whose interpolated value is required.
     *
     * @return the interpolated value
     */
    public abstract double interpolate( double xp );

    /**
     * Return the interpolated value corresponding to some arbitrary
     * ordinate.
     *
     * @param xp the ordinate whose interpolated value is required.
     *
     * @return the interpolated value
     */
    public double evalYData( double xp )
    {
        return interpolate( xp );
    }

    /**
     * Return an array of interpolated values corresponding to some
     * array of ordinates.
     *
     * @param xp the ordinate whose interpolated value is required.
     *
     * @return the interpolated value
     */
    public double[] evalYDataArray( double[] xps )
    {
        // Default interpolation scheme. Improve if possible.
        double[] y = new double[xps.length];
        for ( int i = 0; i < xps.length; i++ ) {
            y[i] = interpolate( xps[i] );
        }
        return y;
    }

    /**
     * Return two indices of the values in an array that lie above and
     * below a given value. If the value doesn't lie within the range
     * the two indices are returned as the nearest end point. The
     * array of values must be increasing or decreasing
     * monotonically.
     *
     * @param array the array of values to be searched
     * @param value the value to be located
     */
    protected int[] binarySearch( double[] array, double value )
    {
        return Sort.binarySearch( array, value );
    }
}
