/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 *  Interface that all interpolators should implement. 
 *  Interpolation assumes a monotonic set of X coordinates (the
 *  ordinates) and an arbitrary set of Y coordinates (the data
 *  values). Interpolation of the Y coordinates is provided by the 
 *  specification of any possible X coordinate or array of X coordinates.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AbstractInterpolator
 */
public interface Interpolator
{
    /**
     * A guess at the number of steps needed between the actual X
     * coordinates that may be used to draw a reasonable representation
     * of the curve being interpolated. 
     */
    public int stepGuess();

    /**
     * Set or reset the coordinates used by this interpolator.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     * @param check whether to check the monotonic direction (compares
     *              0 and 1 values of x). Use this when you need to
     *              preserve the direction temporarily even though the
     *              order may currently be switched, but take care to
     *              reorder before interpolating.
     */
    public void setCoords( double[] x, double[] y, boolean check );

    /**
     * Return the direction being used by this interpolator.
     */
    public boolean isIncreasing();

    /**
     * Append a new position to the existing coordinates.
     * 
     * @param x the X coordinate.
     * @param y the Y coordinate.
     */
    public void appendValue( double newx, double newy );

    /**
     * Get the number of coordinate positions that are being used by
     * this interpolator.
     *
     * @return the number of positions that will be used.
     */
    public int getCount();

    /**
     * Return if the Interpolator is full. This may mean that the
     * instance will ignore or refuse any further vectices.
     */
    public boolean isFull();

    /**
     * Get the X coordinates.
     *
     * @return the X coordinate array. Note this is not a copy, if you
     *         modify it you need to re-apply {@link setCoords}.
     */
    public double[] getXCoords();

    /**
     * Get an X coordinate by index. If the index is invalid an 
     * out of bound exception will be thrown.
     *
     * @return the X coordinate.
     */
    public double getXCoord( int index );

    /**
     * Get the Y coordinates.
     *
     * @return the Y coordinate array. Note this is not a copy, if you
     *         modify it you need to re-apply {@link setCoords}.
     */
    public double[] getYCoords();

    /**
     * Get a Y coordinate by index. If the index is invalid an 
     * out of bound exception will be thrown.
     *
     * @return the Y coordinate.
     */
    public double getYCoord( int index );

    /**
     * Return the interpolated value corresponding to some arbitrary
     * X coordinate.
     *
     * @param xp the X coordinate at which an interpolated Y
     *           coordinate is required.
     *
     * @return the interpolated value.
     */
    public double interpolate( double xp );

    /**
     * Return the interpolated value corresponding to some arbitrary
     * X coordinate.
     *
     * @param xp the X coordinate at which an interpolated Y
     *           coordinate is required.
     *
     * @return the interpolated value.
     */
    public double evalYData( double xp );

    /**
     * Return an array of interpolated value corresponding to some
     * array of arbitrary X coordinates.
     *
     * @param xps the X coordinates at which interpolated Y
     *            coordinates are required.
     *
     * @return the interpolated values.
     */
    public double[] evalYDataArray( double[] xps );
}
