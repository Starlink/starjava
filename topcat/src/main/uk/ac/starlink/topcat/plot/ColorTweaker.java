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
}
