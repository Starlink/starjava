/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 *  Abstract base class for all {@link Interpolator}s. 
 *  Interpolation assumes a monotonic set of X coordinates 
 *  (the ordinates) and an arbitrary set of Y coordinates 
 *  (the data values). Interpolation of the Y coordinates 
 *  is provided by the specification of any possible X
 *  coordinate or array of X coordinates.
 *  <p>
 *  Extenders of this class need to at implement the 
 *  {@link #interpolate}, and probably a few others.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public abstract class AbstractInterpolator
    implements Interpolator
{
    /**
     * Create an instance with no coordinates. A call to 
     * {@link setCoords} must be made before any other methods.
     */
    public AbstractInterpolator()
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
    public AbstractInterpolator( double[] x, double[] y )
    {
        setCoords( x, y, true );
    }

    /**
     * The X coordinates. Must be monotonic. These form the basis for
     * interpolation of the Y coordinates.
     */
    protected double[] x;

    /**
     * The Y coordinates. There should be at least as many of these
     * values as X coordinates. 
     */
    protected double[] y;

    /**
     * Some coefficients, if any associated with the fit.
     */
    protected double[] c;

    /**
     * Whether the X coordinates are monotonically decreasing.
     */
    protected boolean decr = false;

    public int stepGuess()
    {
        return 11;
    }

    public void setCoords( double[] x, double[] y, boolean check )
    {
        // Keep references to the coordinates.
        this.x = x;
        this.y = y;

        // See which way the X coordinates increase, if wanted.
        if ( check && x.length >= 2 ) {
            if ( x[1] < x[0] ) {
                decr = true;
            }
            else {
                decr = false;
            }
        }
    }

    public boolean isIncreasing()
    {
        return ( decr == false );
    }

    public void appendValue( double newx, double newy )
    {
        // Default implementation just appends new position and
        // re-evaluates the whole system.
        int newlength = 0;
        if ( x == null ) {
            newlength = 1;
        }
        else {
            newlength = x.length + 1;
        }
        double tempx[] = new double[newlength];
        double tempy[] = new double[newlength];
        if ( x != null ) {
            System.arraycopy( x, 0, tempx, 0, newlength - 1 );
            System.arraycopy( y, 0, tempy, 0, newlength - 1 );
        }
        tempx[newlength-1] = newx;
        tempy[newlength-1] = newy;
        x = tempx;
        y = tempy;
        setCoords( x, y, false );
    }

    public int getCount()
    {
        if ( x != null ) {
            return x.length;
        }
        return 0;
    }

    public boolean isFull()
    {
        //  Most Interpolators should just grow.
        return false;
    }

    public double[] getXCoords()
    {
        return x;
    }

    public double getXCoord( int index )
    {
        if ( x != null ) {
            return x[index];
        }
        return 0;
    }

    public double[] getYCoords()
    {
        return y;
    }

    public double getYCoord( int index )
    {
        if ( y != null ) {
            return y[index];
        }
        return 0;
    }

    //  Implementation specific, you need to implement this one.
    public abstract double interpolate( double xp );

    public double evalYData( double xp )
    {
        return interpolate( xp );
    }

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
        int bounds[] = new int[2];
        int low = 0;
        int high = array.length - 1;
        boolean increases = ( array[low] < array[high] );

        // Check off scale.
        if ( ( increases && value < array[low] ) ||
             ( ! increases && value > array[low] ) ) {
            high = low;
        }
        else if ( ( increases && value > array[high] ) ||
                  ( ! increases && value < array[high] ) ) {
            low = high;
        }
        else {
            //  Use a binary search as values should be sorted to increase
            //  in either direction (wavelength, pixel coordinates etc.).
            int mid = 0;
            if ( increases ) {
                while ( low < high - 1 ) {
                    mid = ( low + high ) / 2;
                    if ( value < array[mid] ) {
                        high = mid;
                    }
                    else if ( value > array[mid] ) {
                        low = mid;
                    }
                    else {
                        // Exact match.
                        low = high = mid;
                        break;
                    }
                }
            }
            else {
                while ( low < high - 1 ) {
                    mid = ( low + high ) / 2;
                    if ( value > array[mid] ) {
                        high = mid;
                    }
                    else if ( value < array[mid] ) {
                        low = mid;
                    }
                    else {
                        // Exact match.
                        low = high = mid;
                        break;
                    }
                }
            }
        }
        bounds[0] = low;
        bounds[1] = high;
        return bounds;
    }
}
