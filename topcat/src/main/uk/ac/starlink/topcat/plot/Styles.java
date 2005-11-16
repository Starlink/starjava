package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

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
     * Returns an icon which represents a given style set.  It consists of
     * a row of example legends corresponding to the set.
     *
     * @param   styles   style set
     * @return  icon for <code>styles</code>
     */
    public static Icon getIcon( final StyleSet styleSet ) {
        return new Icon() {
            final int NMARK = 5;
            final int SEPARATION = 16;
            final int HEIGHT = SEPARATION;

            public int getIconHeight() {
                return HEIGHT;
            }

            public int getIconWidth() {
                return ( NMARK + 1 ) * SEPARATION;
            }

            public void paintIcon( Component c, Graphics g,
                                   int xoff, int yoff ) {
                int y = yoff + HEIGHT / 2;
                int x = xoff + SEPARATION / 2;
                for ( int i = 0; i < NMARK; i++ ) {
                    styleSet.getStyle( i ).drawLegend( g, x += SEPARATION, y );
                }
            }
        };
    }

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
