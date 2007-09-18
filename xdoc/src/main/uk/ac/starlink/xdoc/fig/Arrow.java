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

    private int pad_;

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
    public final void drawArrow( Graphics2D g2, int x1, int y1,
                                 int x2, int y2 ) {
        double theta = Math.atan2( y2 - y1, x2 - x1 );
        double xpad = pad_ * Math.cos( theta );
        double ypad = pad_ * Math.sin( theta );
        x1 += xpad;
        y1 += ypad;
        x2 -= xpad;
        y2 -= ypad;
        doDrawArrow( g2, x1, y1, x2, y2 );
    }

    /**
     * Does the shape-specific work of drawing the arrow.
     * The positions here are the actual positions of the line ends
     *
     * @param   g2   graphics context
     * @param   x1   actual X coordinate of start (tail) position
     * @param   y1   actual Y coordinate of start (tail) position
     * @param   x2   actual X coordinate of end (head) position
     * @param   y2   actual Y coordinate of end (head) position
     */
    protected abstract void doDrawArrow( Graphics2D g2, int x1, int y1,
                                         int x2, int y2 );

    /**
     * Sets padding.  This is the number of pixels to leave blank at each
     * end of the arrow trajectory.
     *
     * @param   pad  padding in pixels
     */
    public void setPad( int pad ) {
        pad_ = pad;
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

        protected void doDrawArrow( Graphics2D g2, int x1, int y1,
                                    int x2, int y2 ) {
            AffineTransform trans = g2.getTransform();
            g2.translate( x2, y2 );
            g2.rotate( Math.atan2( y2 - y1, x2 - x1 ) );
            g2.fillPolygon( new Polygon( new int[] { 0, -lpar_, -lpar_ },
                                         new int[] { 0, -lperp_, +lperp_ },
                                         3 ) );
            g2.setTransform( trans );
            g2.drawLine( x1, y1, x2, y2 );
        }
    }
}
