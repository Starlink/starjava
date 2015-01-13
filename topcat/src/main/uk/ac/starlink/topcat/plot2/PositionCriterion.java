package uk.ac.starlink.topcat.plot2;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;

/**
 * Defines a true/false test on a data position.
 *
 * @author   Mark Taylor
 * @since    29 Jan 2014
 */
public abstract class PositionCriterion {

    /**
     * Evaluates the test for a data position.
     *
     * @param  dpos  data space coordinate array
     * @return  true iff the given position fits this criterion
     */
    public abstract boolean isIncluded( double[] dpos );

    /**
     * Returns a criterion to test whether point positions fall within
     * the plot bounds of a given surface.
     *
     * @param  surface   plot surface
     * @return  new criterion
     */
    public static PositionCriterion
            createBoundsCriterion( final Surface surface ) {
        return new PositionCriterion() {
            final Point2D.Double gp = new Point2D.Double();
            public boolean isIncluded( double[] dpos ) {
                return surface.dataToGraphics( dpos, true, gp );
            }
        };
    }

    /**
     * Returns a criterion to test whether partial positions fall within
     * the plot bounds of a given surface.  These partial positions are
     * things like histogram data, which have an X but not a Y graphics
     * coordinate.  For this case, either X or Y coordinate within the
     * plot bounds counts as success.
     *
     * @param  surface   plot surface
     * @return  new criterion
     */
    public static PositionCriterion
            createPartialBoundsCriterion( final Surface surface ) {
        Rectangle plotBounds = surface.getPlotBounds();
        final int gxlo = plotBounds.x;
        final int gxhi = plotBounds.x + plotBounds.width;
        final int gylo = plotBounds.y;
        final int gyhi = plotBounds.y + plotBounds.height;
        return new PositionCriterion() {
            final Point2D.Double gp = new Point2D.Double();
            final Point gpi = new Point();
            public boolean isIncluded( double[] dpos ) {
                if ( surface.dataToGraphics( dpos, false, gp ) ) {
                    PlotUtil.quantisePoint( gp, gpi );
                    return ( ( gxlo < gpi.x && gpi.x < gxhi ) ||
                             ( gxlo < gpi.y && gpi.y < gyhi ) );
                }
                else {
                    return false;
                }
            }
        };
    }

    /**
     * Returns a criterion to test whether point positions fall within a given
     * shape in graphics coordinates.
     *
     * @param  surface   plot surface
     * @param  blob   test shape in graphics coordinates
     * @return  new criterion
     */
    public static PositionCriterion
            createBlobCriterion( final Surface surface, final Shape blob ) {

        /* Test for inclusion within the shape bounding box before testing
         * the shape itself.  This does not change the result, it's an
         * optimisation; for a complicated shape the first test should
         * be fast and may throw out a high proportion of points.
         * I haven't tested to see whether this actually does speed things up, 
         * but at least I can't see it slowing things down significantly. */
        final Rectangle blobBounds = blob.getBounds();
        return new PositionCriterion() {
            final Point2D.Double gp = new Point2D.Double();
            final Point gpi = new Point();
            public boolean isIncluded( double[] dpos ) {
                if ( surface.dataToGraphics( dpos, true, gp ) ) {
                    PlotUtil.quantisePoint( gp, gpi );
                    return blobBounds.contains( gpi )
                        && blob.contains( gpi );
                }
                else {
                    return false;
                }
            }
        };
    }
}
