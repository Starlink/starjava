package uk.ac.starlink.xdoc.fig;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Utility for positioning things in the graphics context.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2007
 */
public abstract class Anchor {

    /** Background colour. */
    public static final Color BG = Color.WHITE;

    /**
     * Returns the position of this anchor point in a given rectangle.
     *
     * @param    box   rectangle
     * @return   position of this anchor point in <code>box</code>
     */
    public abstract Point getPoint( Rectangle box );

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
    public Rectangle drawString( Graphics g, String text, int x, int y,
                                 boolean clearBg ) {
        Rectangle bounds = getBounds( g, text );
        Point p = getPoint( bounds );
        Rectangle transBounds =
            new Rectangle( bounds.x + x - p.x, bounds.y + y - p.y,
                           bounds.width, bounds.height );
        if ( clearBg ) {
            clearRect( g, transBounds );
        }
        g.drawString( text, transBounds.x - bounds.x,
                            transBounds.y - bounds.y );
        return transBounds;
    }

    /**
     * Returns an anchor defined by fractional amounts along each edge of a box.
     *
     * @param   xfrac  left-right amount (0-1)
     * @param   yfrac  top-bottom amount (0-1)
     * @return  new anchor
     */
    public static Anchor createFractionAnchor( float xfrac, float yfrac ) {
        return new FractionAnchor( xfrac, yfrac );
    }

    public static final Anchor CENTRE = new FractionAnchor( 0.5f, 0.5f );
    public static final Anchor WEST = new FractionAnchor( 0f, 0.5f );
    public static final Anchor EAST = new FractionAnchor( 1f, 0.5f );
    public static final Anchor NORTH = new FractionAnchor( 0.5f, 0f );
    public static final Anchor SOUTH = new FractionAnchor( 0.5f, 1f );
    public static final Anchor NORTH_WEST = new FractionAnchor( 0f, 0f );
    public static final Anchor SOUTH_WEST = new FractionAnchor( 0f, 1f );
    public static final Anchor NORTH_EAST = new FractionAnchor( 1f, 0f );
    public static final Anchor SOUTH_EAST = new FractionAnchor( 1f, 1f );

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

    /**
     * Fractional anchor implementation.
     */
    private static class FractionAnchor extends Anchor {

        private final float xfrac_;
        private final float yfrac_;

        /**
         * Constructor.
         *
         * @param   xfrac  left-right amount (0-1)
         * @param   yfrac  top-bottom amount (0-1)
         */
        FractionAnchor( float xfrac, float yfrac ) {
            xfrac_ = xfrac;
            yfrac_ = yfrac;
        }

        public Point getPoint( Rectangle box ) {
            return new Point( box.x + (int) ( box.width * xfrac_ ),
                              box.y + (int) ( box.height * yfrac_ ) );
        }
    }
}
