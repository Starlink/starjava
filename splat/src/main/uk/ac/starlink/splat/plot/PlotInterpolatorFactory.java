/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import uk.ac.starlink.diva.interp.BasicInterpolatorFactory;
import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.InterpolatorFactory;
import uk.ac.starlink.splat.util.GaussianInterp;
import uk.ac.starlink.splat.util.LorentzInterp;
import uk.ac.starlink.splat.util.VoigtInterp;
import uk.ac.starlink.splat.util.Utilities;

/**
 * This class extends the basic set of {@link Interpolator}s provided
 * by {@link InterpolatorFactory} to include any additional interpolators used
 * by SPLAT.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotInterpolatorFactory
    implements InterpolatorFactory
{
    //  Implementation note. This effectively proxies the instance of
    //  BasicInterpolatorFactory, as it's not possible to extend it.

    //
    // Enumeration of the "types" of Interpolator that can be created.
    //
    
    /** 
     * Lorentz profile shape.
     */
    public static final int GAUSSIAN = InterpolatorFactory.LINEAR + 1;
    public static final int LORENTZ = InterpolatorFactory.LINEAR + 2;
    public static final int VOIGT = InterpolatorFactory.LINEAR + 3;

    private static final String[] localNames = 
    {
        "gaussian",
        "lorentz",
        "voigt"
    };

    /** Display names for Curves */
    public static final String[] shortNames = Utilities.joinStringArrays
        ( InterpolatorFactory.defaultShortNames, localNames );

    /**
     * The InterpolatorFactory.
     */
    private InterpolatorFactory mainFactory = 
        BasicInterpolatorFactory.getInstance();

    /**
     *  Create the single class instance.
     */
    protected static PlotInterpolatorFactory instance = 
        new PlotInterpolatorFactory();

    /**
     * Get the single instance of this class.
     */
    public static PlotInterpolatorFactory getInstance()
    {
        return instance;
    }

    /**
     *  Hide the constructor from use.
     */
    protected PlotInterpolatorFactory() 
    {
        super();
    }

    /**
     *  Create an Interpolator of the given type.
     */
    public Interpolator makeInterpolator( int interpolator )
    {
        switch( interpolator )
        {
           case GAUSSIAN: {
               return new GaussianInterp();
           }
           case LORENTZ: {
               return new LorentzInterp();
           }
           case VOIGT: {
               return new VoigtInterp();
           }
        }
        return mainFactory.makeInterpolator( interpolator );
    }

    /**
     * Get the number of interpolators.
     */
    public int getInterpolatorCount()
    {
        return shortNames.length;
    }

    /**
     * Return the numeric type of a given Interpolator. Returns -1 if
     * the type is unknown.
     */
    public int getInterpolatorType( Interpolator interpolator )
    {
        if ( interpolator instanceof GaussianInterp ) {
            return GAUSSIAN;
        }
        if ( interpolator instanceof LorentzInterp ) {
            return LORENTZ;
        }
        if ( interpolator instanceof VoigtInterp ) {
            return LORENTZ;
        }
        return mainFactory.getInterpolatorType( interpolator );
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
        for ( int i = 0; i < shortNames.length; i++ ) {
            if ( shortNames[i].equalsIgnoreCase( name ) ) {
                return i;
            }
        }
        return -1;
    }
}
