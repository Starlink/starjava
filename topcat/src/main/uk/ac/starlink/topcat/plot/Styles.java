package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * Utility class relating to the {@link StyleSet} interface.
 * Provides several factory methods for constructing StyleSets
 * amongst other things.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2005
 */
public class Styles {

    private static final Color[] COLORS = new Color[] {
        Color.red,
        Color.blue.brighter(),
        Color.green.darker(),
        Color.gray,
        Color.magenta,
        Color.cyan.darker(),
        Color.orange,
        Color.blue.darker(),
        Color.pink,
        Color.yellow,
        Color.black,
    };

    /**
     * Returns a colour related to a given index.  The same index always
     * maps to the same colour.
     *
     * @param  index  code
     * @return   colour correspoding to <tt>index</tt>
     */
    public static Color getColor( int index ) {
        return COLORS[ Math.abs( index ) % COLORS.length ];
    }
}
