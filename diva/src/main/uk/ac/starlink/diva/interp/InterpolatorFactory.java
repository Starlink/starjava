/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 * Interface for a class that defines how to create {@link Interpolator}
 * instances for an application. Usually there will be just one class
 * that implements this interface.
 * 
 * @author Peter W. Draper
 * @version $Id$
 */
public interface InterpolatorFactory
{
    //
    // Enumeration of the "types" of Interpolator that can be created.
    //

    /** Hermite splines */
    public static final int HERMITE = 0;

    /** Akima splines */
    public static final int AKIMA = 1;

    /** Cubic splines */
    public static final int CUBIC = 2;

    /** Single polynomial though all points. */
    public static final int POLYNOMIAL = 3;

    /** Straight lines between points (almost same as polyline except
     *  X coordinates are always monotonic) */
    public static final int LINEAR = 4;

    /** Display names for Curves defined in the interface. Implementors should
     *  base any extensions on this list */
    public static final String[] defaultShortNames =
    {
        "hermite",
        "akima",
        "cubic",
        "polynomial",
        "linear",
    };

    /**
     * Get the number of interpolators supported.
     */
    public int getInterpolatorCount();

    /**
     *  Create an Interpolator of the given type.
     */
    public Interpolator makeInterpolator( int interpolator );

    /**
     *  Get the short name of an interpolator type.
     */
    public String getShortName( int interpolator );

    /**
     *  Get the interpolator type, given a short name. Returns -1 if
     *  the name isn't known.
     */
    public int getTypeFromName( String name );

    /**
     * Return the numeric type of a given Interpolator. Returns -1 if
     * the type is unknown.
     */
    public int getInterpolatorType( Interpolator interpolator );
}
