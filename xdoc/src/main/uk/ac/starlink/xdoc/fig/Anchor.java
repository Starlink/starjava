package uk.ac.starlink.xdoc.fig;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Utility for positioning strings in the graphics context.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2007
 */
public abstract class Anchor {

    /** Background colour. */
    public static final Color BG = Color.WHITE;

    /**
     * Positions a string relative to the given reference point, 
     * optionally clearing the background first.
     *
     * @param   g  graphics context
     * @param   text  string to draw
     * @param   x   reference point X coordinate
     * @param   y   reference point Y coordinate
     * @param   clearBg  true iff you want to clear the background for 
     *          the text to the background colour (white) before
     *          painting the text
     */
    public abstract Rectangle drawString( Graphics g, String text,
                                          int x, int y, boolean clearBg );

    /** Anchor instance which centres strings on the reference point. */
    public static final Anchor CENTRE = new Anchor() {
        public Rectangle drawString( Graphics g, String text, int x, int y,
                                     boolean clearBg ) {
            Rectangle bounds = getBounds( g, text );
            x -= bounds.width / 2;
            y += bounds.height / 2 - g.getFontMetrics().getDescent();
            bounds.translate( x, y );
            if ( clearBg ) {
                clearRect( g, bounds );
            }
            g.drawString( text, x, y );
            return bounds;
        }
    };

    /** Anchor instance which puts the reference point at bottom left. */
    public static final Anchor SOUTH_WEST = new Anchor() {
        public Rectangle drawString( Graphics g, String text, int x, int y,
                                     boolean clearBg ) {
            Rectangle bounds = getBounds( g, text );
            bounds.translate( x, y );
            if ( clearBg ) {
                clearRect( g, bounds );
            }
            g.drawString( text, x, y - g.getFontMetrics().getDescent() );
            return bounds;
        }
    };

    /** Anchor instance which puts the reference point at the top right. */
    public static final Anchor NORTH_WEST = new Anchor() {
        public Rectangle drawString( Graphics g, String text, int x, int y,
                                     boolean clearBg ) {
            Rectangle bounds = getBounds( g, text );
            y += bounds.height;
            bounds.translate( x, y );
            if ( clearBg ) {
                clearRect( g, bounds );
            }
            g.drawString( text, x, y );
            return bounds;
        }
    };

    /**
     * Clears a given rectangle to the background colour.
     *
     * @param   g   graphics context
     * @param   rect   rectangle to clear
     */
    private static void clearRect( Graphics g, Rectangle rect ) {
        Color color = g.getColor();
        g.setColor( BG );
        g.fillRect( rect.x, rect.y, rect.width, rect.height );
        g.setColor( color );
    }

    /**
     * Returns the bounds of a text string drawn at the origin in a
     * given graphics context.
     *
     * @param   g  graphics context
     * @param   text   string
     */
    private static Rectangle getBounds( Graphics g, String text ) {
        return g.getFontMetrics().getStringBounds( text, g ).getBounds();
    }
}
