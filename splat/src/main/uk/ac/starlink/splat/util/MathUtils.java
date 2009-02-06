/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     29-APR-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import java.awt.Color;
import java.util.Random;
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

    private static Random generator = null;

    /**
     *  Get a random colour as an RGB integer.
     */
    public static int getRandomRGB()
    {
        if ( generator == null ) {
            generator = new Random();
        }

        //  Hue is an float between 0 and 1, which will be scaled to the range
        //  0 to 360 (so the fractional part determines the actual hue
        //  used). Skip the range of hues for yellows as these look poor 
        //  against a white background.
        float h = generator.nextFloat();
        if ( h > 0.1f && h < 0.25f ) {
            h -= 0.15f; // More reds, no yellows.
        }

        //  Keep brightness down a little so that these are distinguishable
        //  from all the primary colours (which are used for overlay graphics).
        return Color.HSBtoRGB( h, 1.0F, 0.9F );
    }

    /**
     *  Get a random colour from a set of a given size.
     */
    public static Color getRandomColour()
    {
        //  Same ideas as getRandomRBG, just returns a Color.
        if ( generator == null ) {
            generator = new Random();
        }

        float h = generator.nextFloat();
        if ( h > 0.1f && h < 0.25f ) {
            h -= 0.15f; // More reds, no yellows.
        }

        return Color.getHSBColor( h, 1.0F, 0.9F );
    }
}
