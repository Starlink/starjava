package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.Icon;

/**
 * Uncoloured icon suitable for legends representing multi-position plot types.
 * This abstract class generates X,Y positions representing the positions
 * on the icon, and concrete subclasses can do something visually specific
 * with them.
 *
 * @author   Mark Taylor
 * @since    28 Nov 2013
 */
public abstract class MultiPosIcon implements Icon {

    private final int size_;
    private final Point[] positions_;

    /**
     * Constructor.
     *
     * @param  npos  number of points
     */
    public MultiPosIcon( final int npos ) {
        size_ = 16;
        int size2 = size_ / 2;
        positions_ = new Point[ npos ];
        for ( int ip = 0; ip < npos; ip++ ) {
            double theta = 0.125 * Math.PI + ( 2.0 * Math.PI * ip ) / npos;
            int x = (int) ( size2 + size2 * Math.cos( theta ) );
            int y = (int) ( size2 - size2 * Math.sin( theta ) );
            positions_[ ip ] = new Point( x, y );
        }
    }

    public int getIconWidth() {
        return size_;
    }

    public int getIconHeight() {
        return size_;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        g.translate( x, y );
        paintPositions( g, positions_ );
        g.translate( -x, -y );
    }

    /**
     * Generates graphics for a given set of positions in a way that
     * represents the behaviour this icon wants to illustrate.
     * The number of positions presented is that specified at
     * construction time.
     *
     * @param   g  graphics context
     * @param   positions  graphic positions of points
     */
    protected abstract void paintPositions( Graphics g, Point[] positions );
}
