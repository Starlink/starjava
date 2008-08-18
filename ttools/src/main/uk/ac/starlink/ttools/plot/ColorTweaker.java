package uk.ac.starlink.ttools.plot;

import java.awt.Color;

/**
 * Interface defining how a colour is changed from some input colour to
 * some output colour.  Two methods are defined which should perform the
 * same transformation, but one uses Color objects and the other an
 * sRGB array.  Although it is possible to define each in terms of the 
 * other, performance can be improved considerably by implementing them
 * both directly.
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
     * Adjusts in place an array representing the sRGB components of a colour.
     * Each element is in the range 0..1 on both input and output.
     *
     * @param  rgba  red, green, blue, alpha array
     */
    void tweakColor( float[] rgba );
}
