package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Drawing plan object for counting the number of hits to each bin in
 * a grid.  It's a 2-d histogram.
 *
 * <p>The {@link #calculatePointCloudPlan calculatePointCloudPlan} method
 * is intended for use by {@link uk.ac.starlink.ttools.plot2.Drawing}
 * implementations.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class BinPlan {
    private final Binner binner_;
    private final Gridder gridder_;

    /**
     * Constructor.
     *
     * @param  binner  contains counts
     * @param  gridder  contains pixel grid geometry
     */
    public BinPlan( Binner binner, Gridder gridder ) {
        binner_ = binner;
        gridder_ = gridder;
    }

    /**
     * Returns count information.
     *
     * @return  binner
     */
    public Binner getBinner() {
        return binner_;
    }

    /**
     * Returns grid geometry.
     *
     * @return  gridder
     */
    public Gridder getGridder() {
        return gridder_;
    }

    /**
     * Returns a BinPlan instance which reports where on a grid points
     * in a PointCloud have landed.
     * Used as a plan for plot layers which want a count of the data points
     * falling in each plot surface pixel.
     * Instances returned by this method are reusable by layers
     * which have the same requirements.
     *
     * @param   pointCloud  data position set
     * @param   surface   plot surface
     * @param   dataStore  data storage object
     * @param  knownPlans   existing pre-calculated plans;
     *                      if one of these fits the bill it will be
     *                      returned without any calculations being performed
     */
    public static BinPlan calculatePointCloudPlan( PointCloud pointCloud,
                                                   Surface surface,
                                                   DataStore dataStore,
                                                   Object[] knownPlans ) {
        for ( int ip = 0; ip < knownPlans.length; ip++ ) {
            if ( knownPlans[ ip ] instanceof PointCloudBinPlan ) {
                PointCloudBinPlan plan = (PointCloudBinPlan) knownPlans[ ip ];
                if ( plan.matches( pointCloud, surface ) ) {
                    return plan;
                }
            }
        }
        Rectangle bounds = surface.getPlotBounds();
        int xoff = bounds.x;
        int yoff = bounds.y;
        Gridder gridder = new Gridder( bounds.width, bounds.height );
        Binner binner = new Binner( gridder.getLength() );
        Point2D.Double gp = new Point2D.Double();
        for ( double[] dpos : pointCloud.createDataPosIterable( dataStore ) ) {
            if ( surface.dataToGraphics( dpos, true, gp ) ) {
                int gx = PlotUtil.ifloor( gp.x ) - xoff;
                int gy = PlotUtil.ifloor( gp.y ) - yoff;
                binner.increment( gridder.getIndex( gx, gy ) );
            }
        }
        return new PointCloudBinPlan( binner, gridder, pointCloud, surface );
    }

    /**
     * Concrete BinPlan implementation for the positions represented
     * in a PointCloud.
     */
    private static class PointCloudBinPlan extends BinPlan {
        final PointCloud pointCloud_;
        final Surface surface_;

        /**
         * Constructor.
         *
         * @param  binner  contains counts
         * @param  gridder  contains pixel grid geometry
         * @param  pointCloud  data point set
         * @param  surface  plot surface
         */
        PointCloudBinPlan( Binner binner, Gridder gridder,
                           PointCloud pointCloud, Surface surface ) {
            super( binner, gridder );
            pointCloud_ = pointCloud;
            surface_ = surface;
        }

        /**
         * Indicates whether this object can be used as a plan for a given
         * set of constraints.
         *
         * @param  pointCloud  data point set
         * @param  surface  required plot surface
         */
        boolean matches( PointCloud pointCloud, Surface surface ) {
            return pointCloud.equals( pointCloud_ )
                && surface.equals( surface_ );
        }
    }
}
