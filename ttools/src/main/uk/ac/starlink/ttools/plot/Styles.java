package uk.ac.starlink.ttools.plot;

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

    public static final float[][] DASHES = new float[][] {
        null,
        { 3f, 3f },
        { 8f, 4f },
        { 12f, 3f, 3f, 3f },
    };

    public static final Color[] COLORS = TC1_COLORS;

    /** Colour to use in monochrome colour scheme (black). */
    public static final Color PLAIN_COLOR = Color.BLACK;

    /* Stroke style to use if no stroke variation is taking place (unbroken). */
    public static final Stroke PLAIN_STROKE = new BasicStroke();

    /**
     * Returns a colour labelled by a given index.  The same index always
     * maps to the same colour.
     *
     * @param  index  code
     * @return   colour correspoding to <code>index</code>
     */
    public static Color getColor( int index ) {
        return COLORS[ Math.abs( index ) % COLORS.length ];
    }

    /**
     * Returns a dash pattern labelled by a given index. 
     * The same index always maps to the same pattern.
     *
     * @param   index  code
     * @return  stroke corresponding to <code>index</code>
     */
    public static float[] getDash( int index ) {
        return DASHES[ Math.abs( index ) % DASHES.length ];
    }
}
