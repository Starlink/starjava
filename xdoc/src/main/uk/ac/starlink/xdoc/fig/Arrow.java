package uk.ac.starlink.xdoc.fig;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;

/**
 * Draws arrows.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2007
 */
public abstract class Arrow {

    private int headPad_;
    private int tailPad_;

    /**
     * Draws an arrow.  The positions specified here may not be the actual
     * positions of the head and tail; if padding is in use they will
     * stop short of those.
     *
     * @param   g2   graphics context
     * @param   x1   nominal X coordinate of start (tail) position
     * @param   y1   nominal Y coordinate of start (tail) position
     * @param   x2   nominal X coordinate of end (head) position
     * @param   y2   nominal Y coordinate of end (head) position
     */
    public void drawArrow( Graphics2D g2, int x1, int y1, int x2, int y2 ) {
        AffineTransform trans = g2.getTransform();
        g2.translate( x2, y2 );
        g2.rotate( Math.atan2( y1 - y2, x1 - x2 ) );
        g2.translate( + headPad_, 0 );
        double r1 = Math.sqrt( ( x2 - x1 ) * ( x2 - x1 ) + 
                               ( y2 - y1 ) * ( y2 - y1 ) );
        drawArrow( g2, (int) Math.round( r1 - tailPad_ - headPad_ ) );
        g2.setTransform( trans );
    }

    /**
     * Does the shape-specific work of drawing the arrow.
     * Draws an arrow line and head from (x1, 0) to the origin.
     *
     * @param   g2   graphics context
     * @param   x1   X coordinate of tail of arrow; x1 >= 0
     */
    protected abstract void drawArrow( Graphics2D g2, int x1 );

    /**
     * Sets padding.  This is the number of pixels to leave blank at each
     * end of the arrow trajectory.
     *
     * @param   pad  padding in pixels
     */
    public void setPad( int pad ) {
        headPad_ = pad;
        tailPad_ = pad;
    }

    /**
     * Returns an arrow instance with a filled head.
     *
     * @param   lpar   head length parallel to the trajectory
     * @param   lperp  head half-width perpendicular to the trajectory
     */
    public static Arrow createFilledArrow( int lpar, int lperp ) {
        return new FilledArrow( lpar, lperp );
    }

    /**
     * Arrow instance with a filled triangular head.
     */
    public static class FilledArrow extends Arrow {

        private final int lpar_;
        private final int lperp_;

        /**
         * Constructor.
         *
         * @param   lpar   head length parallel to the trajectory
         * @param   lperp  head half-width perpendicular to the trajectory
         */
        public FilledArrow( int lpar, int lperp ) {
            lpar_ = lpar;
            lperp_ = lperp;
        }

        protected void drawArrow( Graphics2D g2, int x1 ) {
            g2.drawLine( x1, 0, lpar_ / 2, 0 );
            g2.fillPolygon( new int[] { 0, lpar_, lpar_, },
                            new int[] { 0, -lperp_, +lperp_, }, 3 );
        }
    }
}
