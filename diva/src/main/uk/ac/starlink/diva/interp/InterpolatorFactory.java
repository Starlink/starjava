/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

/**
 * This class creates and enumerates the types of {@link Interpolator}
 * that are available.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class InterpolatorFactory
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

    /** Display names for Curves */
    public static final String[] shortNames =
        {
            "hermite",
            "akima",
            "cubic",
            "polynomial",
            "linear",
        };

    /** The number of interpolator types */
    public static final int NUM_INTERPOLATORS = shortNames.length;

    /**
     *  Create the single class instance.
     */
    private static final InterpolatorFactory instance = new InterpolatorFactory();

    /**
     *  Hide the constructor from use.
     */
    private InterpolatorFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static InterpolatorFactory getReference()
    {
        return instance;
    }

    /**
     *  Create an Interpolator of the given type.
     */
    public Interpolator makeInterpolator( int interpolator )
    {
        switch( interpolator )
        {
           case HERMITE: {
               return new HermiteSplineInterp();
           }
           case AKIMA: {
               return new AkimaSplineInterp();
           }
           case CUBIC: {
               return new CubicSplineInterp();
           }
           case POLYNOMIAL: {
               return new PolynomialInterp();
           }
           case LINEAR: {
               return new LinearInterp();
           }
        }
        return null;
    }

    /**
     *  Get the short name of an interpolator type.
     */
    public String getShortName( int interpolator )
    {
        return shortNames[interpolator];
    }

    /**
     *  Get the interpolator type, given a short name. Returns -1 if
     *  the name isn't known.
     */
    public int getTypeFromName( String name )
    {
        for ( int i = 0; i < NUM_INTERPOLATORS; i++ ) {
            if ( shortNames[i].equalsIgnoreCase( name ) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the numeric type of a given Interpolator. Returns -1 if
     * the type is unknown.
     */
    public int getInterpolatorType( Interpolator interpolator )
    {
        if ( interpolator instanceof HermiteSplineInterp ) {
            return HERMITE;
        }
        if ( interpolator instanceof AkimaSplineInterp ) {
            return AKIMA;
        }
        if ( interpolator instanceof CubicSplineInterp ) {
            return CUBIC;
        }
        if ( interpolator instanceof PolynomialInterp ) {
            return POLYNOMIAL;
        }
        if ( interpolator instanceof LinearInterp ) {
            return LINEAR;
        }
        return -1;
    }
}
