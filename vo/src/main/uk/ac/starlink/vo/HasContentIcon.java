package uk.ac.starlink.vo;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Little icon that indicates boolean state.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2015
 */
public class HasContentIcon implements Icon {

    private final int size_;
    private final boolean hasContent_;

    /** Instance indicating true state. */
    private static final HasContentIcon YES = new HasContentIcon( 10, true );

    /** Instance indicating false state. */
    private static final HasContentIcon NO = new HasContentIcon( 10, false );

    /**
     * Constructor.
     *
     * @param  size  width=height in pixels
     * @param  hasContent  indicates state
     */
    private HasContentIcon( int size, boolean hasContent ) {
        size_ = size;
        hasContent_ = hasContent;
    }

    public int getIconWidth() {
        return size_;
    }

    public int getIconHeight() {
        return size_;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        Graphics2D g2 = (Graphics2D) g.create();
        g = null;
        g2.setColor( c.getForeground() );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON );
        g2.drawOval( x + 1, y + 1, size_ - 2, size_ - 2 );
        if ( hasContent_ ) {
            g2.fillOval( x + 3, y + 3, size_ - 5, size_ - 5 );
        }
    }

    /**
     * Returns an instance of this icon.
     *
     * @param  hasContent  state flag
     * @return  icon indicating one or other state
     */
    public static Icon getIcon( boolean hasContent ) {
        return hasContent ? YES : NO;
    }
}
