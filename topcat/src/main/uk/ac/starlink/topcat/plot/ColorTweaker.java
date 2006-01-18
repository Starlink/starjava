package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * Interface defining how a colour is changed from some input colour to
 * some output colour.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2006
 */
public interface ColorTweaker {

    /**
     * Returns a tweaked version of the submitted colour.
     *
     * @param  orig  input colour
     * @return   tweaked colour
     */
    Color tweakColor( Color orig );

    /**
     * Modifies the alpha, red, green, blue components of a colour
     * (each assumed between 0 and 255).  The input and output array
     * has the elements in the order (blue, green, red, alpha).
     *
     * @param   rgba  array giving ARGB components of a colour, changed
     *          on exit
     */
    void tweakARGB( int[] argb );

    /**
     * Modifies the red, green, blue, alpha components of a colour,
     * as packed into an integer in the format 
     * {@link java.awt.image.BufferedImage.TYPE_INT_ARGB}
     *
     * @param  rgba  packed input colour
     * @return  packed output colour
     */
    int tweakARGB( int argb );
}
