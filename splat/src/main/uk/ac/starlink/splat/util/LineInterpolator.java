/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     30-MAR-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.LinearInterp;

/**
 * Base class for {@link Interpolators} that describe a spectral line. 
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public abstract class LineInterpolator
    extends LinearInterp
{
    // Constants for indexing names.
    public static final int SCALE = 0;
    public static final int CENTRE = 1;
    public static final int GWIDTH = 2;
    public static final int LWIDTH = 3;

    /**
     * The FunctionGenerator. Does the real work.
     */
    protected FunctionGenerator generator = null;

    /**
     * Zero point for the line.
     */
    protected double base = 0.0;

    /**
     * Maximum number of values used to define a curve interactively.
     */
    protected int maxValues = 4;

    /**
     * Create an instance with no coordinates. A call to
     * {@link #setValues} must be made before any other methods.
     * 
     * @param maxValues the maximum number of values used by this
     *                  interpolator.
     */
    public LineInterpolator( int maxValues )
    {
        setMaxValues( maxValues );
    }

    /**
     * Set the maximum number of values that are used to define the
     * line characteristics as guide points (rendering extent,
     * maximum, widths).
     */
    public void setMaxValues( int maxValues )
    {
        this.maxValues = maxValues;
    }

    // Lines need a lot of precision.
    public int stepGuess()
    {
        return 101;
    }

    /**
     * Create an instance with the given coordinates. Interpolation
     * is by X coordinate see the {@link #interpolate}  method.
     * The X coordinates should be monotonic, either increasing or
     * decreasing. Same value X coordinates are not allowed.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public LineInterpolator( double[] x, double[] y )
    {
        setValues( x, y );
    }

    public void appendValue( double newx, double newy )
    {
        //  Once we have enough stop appending.
        if ( x.length < maxValues ) {
            super.appendValue( newx, newy );
        }
    }

    public boolean isFull()
    {
        if ( x.length < maxValues ) {
            return false;
        }
        return true;
    }

    /**
     * Set the coordinates that define the line.
     *
     * @param x the X coordinates.
     * @param y the Y coordinates.
     */
    public void setValues( double[] x, double[] y )
    {
        //  Keep old positions, if given too many or they don't match.
        if ( x.length > maxValues || ( x.length != y.length ) ) {
            x = null;
            y = null;
            return;
        }

        // If just two positions, bootstrap a complete line.
        if ( x.length == 2 ) {
            bootstrap( x, y );
            x = this.x;
            y = this.y;
        }
        this.x = x;
        this.y = y;
        decr = false;

        //  Complete set of positions, create the line.
        if ( x.length > maxValues - 1 ) {
            double props[] = getProps( x, y );
            base = y[2] - props[SCALE];
            setProps( props );
        }
    }
    
    /**
     * When given 2 positions, use these to bootstrap a complete
     * curve. The first position is taken as at the FWHM and second at
     * the peak. This covers Gaussian and Lorentzian curves. Voigt
     * will need to re-implement this.
     */
    protected void bootstrap( double[] x, double[] y )
    {
        this.x = new double[4];
        this.y = new double[4];
        this.x[0] = x[1] - 2.0 * ( x[1] - x[0] );
        this.x[1] = x[0];
        this.x[2] = x[1];
        this.x[3] = x[1] + 2.0 * ( x[1] - x[0] );
        
        this.y[0] = y[1] - 2.0 * ( y[1] - y[0] );
        this.y[1] = y[0];
        this.y[2] = y[1];
        this.y[3] = this.y[0];
    }

    /**
     * Given a set of reference X and Y coordinates return an array of
     * properties that describe a related spectral line. Usually these
     * are the line position, scale and width, but Voigt will also
     * require another width (Lorentz). The scale will be negative for
     * an absorption line, if the Y coordinate system run from top to
     * bottom (i.e. graphics coordinates).
     *
     * @param x the x coordinates, must be at least 3.
     * @param y the y coordinates, must be at least 3.
     * @return the scale, centre and width, indexed by SCALE, CENTRE and
     *         GWIDTH and LWIDTH.
     */
    abstract public double[] getProps( double[] x, double[] y );

    public void setCoords( double[] x, double[] y, boolean check )
    {
        setValues( x, y );
    }

    public double interpolate( double xp )
    {
        if ( x.length > maxValues - 1 ) {
            return generator.evalYData( xp ) + base;
        }
        return super.interpolate( xp );
    }

    /** 
     * Return a name for each of the possible properties.
     */
    public static String getName( int index )
    {
        switch (index) {
           case CENTRE: {
               return "Centre";
           }
           case SCALE: {
               return "Scale";
           }
           case GWIDTH: {
               return "GWidth";
           }
           case LWIDTH: {
               return "LWidth";
           }
        }
        return "Unknown";
    }

    /**
     *  Number of possible properties.
     */
    public int propertyCount()
    {
        return LWIDTH;
    }
    
    /**
     * Set the properties of the line. Usually this will be the line
     * centre, scale and width. For Voigt profiles and another width
     * is also required. The props array should be indexed by CENTRE,
     * SCALE, GWIDTH and LWIDTH.
     */
    abstract public void setProps( double[] props );

    /**
     * Get the zero point for the line (a simple background).
     */
    public double getZeroPoint()
    {
        return base;
    }

    /**
     * Return a reference to {@link FunctionGenerator}. This should normally
     * be configured with the current line properties.
     */
    public FunctionGenerator getFunctionGenerator()
    {
        return generator;
    }
}
