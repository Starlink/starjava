package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * Utility class relating to the {@link StyleSet} interface.
 *
 * @author   Mark Taylor
 * @since    4 Nov 2005
 */
public class Styles {

    private static final Color[] TC1_COLORS = new Color[] {
        new Color( 0xf00000 ),
        new Color( 0x0000f0 ),
        Color.green.darker(),
        Color.gray,
        Color.magenta,
        Color.cyan.darker(),
        Color.orange,
        Color.pink,
        Color.yellow,
        Color.black,
    };

    private static final Color[] COLORS = false ? PlotBox._colors : TC1_COLORS;

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
