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
public class BasicInterpolatorFactory
    implements InterpolatorFactory
{
    /**
     *  Create the single class instance.
     */
    private static final InterpolatorFactory instance = 
        new BasicInterpolatorFactory();

    /**
     *  Hide the constructor from use.
     */
    private BasicInterpolatorFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static InterpolatorFactory getInstance()
    {
        return instance;
    }

    public int getInterpolatorCount()
    {
        return defaultShortNames.length;
    }

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

    public String getShortName( int interpolator )
    {
        return defaultShortNames[interpolator];
    }

    public int getTypeFromName( String name )
    {
        for ( int i = 0; i < defaultShortNames.length; i++ ) {
            if ( defaultShortNames[i].equalsIgnoreCase( name ) ) {
                return i;
            }
        }
        return -1;
    }

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
