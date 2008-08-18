package uk.ac.starlink.ttools.plot;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Icon implementation which doesn't draw anything.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2007
 */
public class EmptyIcon implements Icon {

    private final int width_;
    private final int height_;

    /**
     * Constructor.
     *
     * @param  width   icon width
     * @param  height  icon height
     */
    public EmptyIcon( int width, int height ) {
        width_ = width;
        height_ = height;
    }

    public int getIconWidth() {
        return width_;
    }

    public int getIconHeight() {
        return height_;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
    }
}
