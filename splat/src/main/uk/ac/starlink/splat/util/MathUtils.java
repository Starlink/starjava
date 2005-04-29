/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     29-APR-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import java.awt.Color;
import uk.ac.starlink.util.PhysicalConstants;

/**
 * Static class of useful pseudo math functions.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class MathUtils
{
    /** Constructor, private just a class of static methods. */
    private MathUtils()
    {
        //  Does nothing.
    }

    //
    // Velocity and redshift routines.
    //

    /**
     * Convert a redshift into a relativistic velocity.
     *
     * @param redshift the redshift
     * @return the velocity in metres-per-second.
     */
    public static double redshiftToVelocity( double redshift )
    {
        double tmp = ( 1.0 + redshift ) * ( 1.0 + redshift );
        return PhysicalConstants.SPEED_OF_LIGHT * ( tmp - 1.0 )/( tmp + 1.0 );
    }

    /**
     * Convert a relativistic velocity into a redshift.
     *
     * @param velocity the velocity in metres-per-second.
     * @return the redshift.
     */
    public static double velocityToRedshift( double velocity )
    {
        double tmp1 = PhysicalConstants.SPEED_OF_LIGHT + velocity;
        double tmp2 = PhysicalConstants.SPEED_OF_LIGHT - velocity;
        return 1.0 + Math.sqrt( tmp1 / tmp2 );
    }

    //
    // Spectral colouring routines.
    //

    private static java.util.Random generator = null;
    /**
     *  Get a random colour from a set of a given size.
     *
     *  @param res number of expected colours in rainbow (i.e. number
     *             you want to select from).
     */
    public static Color getRandomColour( float res )
    {
        if ( generator == null ) {
            generator = new java.util.Random();
        }
        float h = generator.nextFloat() * res;
        return Color.getHSBColor( h, 1.0F, 1.0F );
    }

    /**
     *  Get a rainbow colour as an RGB integer from a set of a given
     *  size.
     *
     *  @param res number of expected colours in rainbow (i.e. number
     *             you want to select from).
     */
    public static int getRandomRGB( float res )
    {
        if ( generator == null ) {
            generator = new java.util.Random();
        }
        float h = generator.nextFloat() * res;
        return Color.HSBtoRGB( h, 1.0F, 1.0F );
    }

}
