package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Icon which modifies an existing one by changing its colour.
 * The colour attribute of the graphics context is changed before the
 * icon is painted.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2007
 */
public class ColoredIcon implements Icon {

    private final Icon baseIcon_;
    private final Color color_;

    /**
     * Constructor.
     *
     * @param   baseIcon  icon to modify
     * @param   color   new colour
     */
    public ColoredIcon( Icon baseIcon, Color color ) {
        baseIcon_ = baseIcon;
        color_ = color;
    }

    public int getIconWidth() {
        return baseIcon_.getIconWidth();
    }

    public int getIconHeight() {
        return baseIcon_.getIconHeight();
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        Color oldColor = g.getColor();
        g.setColor( color_ );
        baseIcon_.paintIcon( c, g, x, y );
        g.setColor( oldColor );
    }
}
