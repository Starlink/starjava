package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Stroke;
import javax.swing.Icon;

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

    private static final float[][] DASHES = new float[][] {
        { 1f },
        { 3f, 3f },
        { 9f, 5f },
        { 12f, 3f, 3f, 3f },
    };

    public static final Color[] COLORS = false ? PlotBox._colors : TC1_COLORS;

    private static final Stroke[] STROKES;
    static {
        STROKES = new Stroke[ DASHES.length ];
        for ( int i = 0; i < DASHES.length; i++ ) {
            STROKES[ i ] = new BasicStroke( 1f, BasicStroke.CAP_SQUARE,
                                            BasicStroke.JOIN_MITER,
                                            10f, DASHES[ i ], 0f );
        }
    }

    /** Colour to use in monochrome colour scheme (black). */
    public static final Color PLAIN_COLOR = Color.BLACK;

    /* Stroke style to use if no stroke variation is taking place (unbroken). */
    public static final Stroke PLAIN_STROKE = new BasicStroke();

    /**
     * Returns a colour labelled by a given index.  The same index always
     * maps to the same colour.
     *
     * @param  index  code
     * @return   colour correspoding to <tt>index</tt>
     */
    public static Color getColor( int index ) {
        return COLORS[ Math.abs( index ) % COLORS.length ];
    }

    /**
     * Returns a stroke labelled by a given index.  The same index always
     * maps to the same colour.
     *
     * @param   index  code
     * @return  stroke corresponding to <tt>index</tt>
     */
    public static Stroke getStroke( int index ) {
        return STROKES[ Math.abs( index ) % STROKES.length ];
    }

    /**
     * Returns an icon which displays the legend for a given style.
     * If <code>style</code> is null, an empty icon of the right size is
     * returned.
     *
     * @param   style  style
     * @param   width  icon width
     * @param   height icon height
     * @return  icon for style legend
     */
    public static Icon getLegendIcon( final Style style, final int width,
                                      final int height ) {
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return width;
            }
            public void paintIcon( Component c, Graphics g,
                                   int xoff, int yoff ) {
                int x = xoff + width / 2;
                int y = yoff + height / 2;
                if ( style != null ) {
                    style.drawLegend( g, x, y );
                }
            }
        };
    }
}
