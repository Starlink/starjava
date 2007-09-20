package uk.ac.starlink.xdoc.fig;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Object which can draw a string on a graphics context.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2007
 */
public class StringDrawer {

    private final Anchor anchor_;
    private final boolean clearBg_;
    private final float fontScale_;

    /**
     * Constructor.
     *
     * @param   anchor  positional anchor for string
     * @param   clearBg  true to clear the bounds of the string area to
     *                   the background colour (white) first
     * @param   fontScale  scales the font size; 1 is normal
     */
    public StringDrawer( Anchor anchor, boolean clearBg, float fontScale ) {
        anchor_ = anchor;
        clearBg_ = clearBg;
        fontScale_ = fontScale;
    }

    /**
     * Draws a string on the graphics context using the settings of
     * this object.
     *
     * @param   g   graphics context
     * @param   text  text to write
     * @param   x   anchor X coordinate
     * @param   y   anchor Y coordinate
     * @return   bounding box for painted string
     */
    public Rectangle drawString( Graphics g, String text, int x, int y ) {
        Font font = g.getFont();
        if ( fontScale_ != 1f ) {
            g.setFont( font.deriveFont( font.getSize2D() * fontScale_ ) );
        }
        Rectangle bounds = anchor_.drawString( g, text, x, y, clearBg_ );
        if ( fontScale_ != 1f ) {
            g.setFont( font );
        }
        return bounds;
    }
}
