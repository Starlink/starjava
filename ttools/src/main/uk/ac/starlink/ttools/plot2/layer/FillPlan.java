package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.util.SplitCollector;

/**
 * Plan object for fill plots.
 * This is an unweighted pixel density map (2d histogram),
 * plus some additional compact information describing the data
 * that falls outside of the plot density map.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2016
 */
public class FillPlan {

    private final Binner binner_;
    private final Gridder gridder_;
    private final int[] xlos_;
    private final int[] xhis_;
    private final int[] ylos_;
    private final int[] yhis_;
    private final Point cpXlo_;
    private final Point cpXhi_;
    private final Point cpYlo_;
    private final Point cpYhi_;
    private final DataGeom geom_;
    private final DataSpec dataSpec_;
    private final Surface surface_;

    /**
     * Constructor.
     *
     * @param  dataSpec  data specification
     * @param  fcollector   colllector
     * @param  fdata   accumulated data
     */
    private FillPlan( DataSpec dataSpec, FillCollector fcollector,
                      FillData fdata ) {
        dataSpec_ = dataSpec;
        binner_ = fdata.binner_;
        gridder_ = fcollector.gridder_;
        xlos_ = fdata.xlos_;
        xhis_ = fdata.xhis_;
        ylos_ = fdata.ylos_;
        yhis_ = fdata.yhis_;
        cpXlo_ = fdata.cpXlo_;
        cpXhi_ = fdata.cpXhi_;
        cpYlo_ = fdata.cpYlo_;
        cpYhi_ = fdata.cpYhi_;
        geom_ = fcollector.geom_;
        surface_ = fcollector.surface_;
    }

    /**
     * Returns the object containing density map pixel counts.
     *
     * @return  binner
     */
    public Binner getBinner() {
        return binner_;
    }

    /**
     * Returns the object encapsulating grid geometry.
     *
     * @return  gridder
     */
    public Gridder getGridder() {
        return gridder_;
    }

    /**
     * Returns array of bins containing all points above each pixel column.
     *
     * @return   xlos
     */
    public int[] getXlos() {
        return xlos_;
    }

    /**
     * Returns array of bins containing all points below each pixel column.
     *
     * @return  xhis
     */
    public int[] getXhis() {
        return xhis_;
    }

    /**
     * Returns array of bins containing all points to left of each pixel row.
     *
     * @return   ylos
     */
    public int[] getYlos() {
        return ylos_;
    }

    /**
     * Returns array of bins containing all points to right of each pixel row.
     */
    public int[] getYhis() {
        return yhis_;
    }

    /**
     * Returns closest point to the lower X boundary
     * that falls outside the grid.
     *
     * @return  cpXlo
     */
    public Point getCpXlo() {
        return cpXlo_;
    }

    /**
     * Returns the closest point to the upper X boundary
     * that falls outside the grid.
     *
     * @return  cpXhi
     */
    public Point getCpXhi() {
        return cpXhi_;
    }

    /**
     * Returns the closest point to the lower Y boundary
     * that falls outside the grid.
     *
     * @return  cpYlo
     */
    public Point getCpYlo() {
        return cpYlo_;
    }

    /**
     * Returns the closest point to the upper Y boundary
     * that falls outside the grid.
     *
     * @return  cpYhi
     */
    public Point getCpYhi() {
        return cpYhi_;
    }

    /**
     * Indicates whether this map's data is valid for a particular context.
     *
     * @param  geom   data geom
     * @param  dataSpec  data specification
     * @param  surface  plot surface
     * @return   true iff this map can be used for the given params
     */
    public boolean matches( DataGeom geom, DataSpec dataSpec,
                            Surface surface ) {
        return geom_.equals( geom )
            && dataSpec_.equals( dataSpec )
            && surface_.equals( surface );
    }

    /**
     * Creates a fill plan object for point cloud data.
     *
     * @param   surface  plot surface
     * @param  dataSpec  data specification
     * @param  geom   data geom
     * @param  icPos   position coordinate index
     * @param  dataStore   data store
     * @return  new plan object
     */
    public static FillPlan createPlan( Surface surface, DataSpec dataSpec,
                                       DataGeom geom, int icPos,
                                       DataStore dataStore ) {
        FillCollector fcollector =
            new PointsFillCollector( surface, geom, icPos );
        FillData fdata =
            PlotUtil.tupleCollect( fcollector, dataSpec, dataStore );
        return new FillPlan( dataSpec, fcollector, fdata );
    }

    /**
     * Creates a fill plan object for XY array data.
     *
     * @param   surface  plot surface
     * @param  dataSpec  data specification
     * @param  geom   data geom
     * @param  xsCoord  coordinate reader for X coordinate array
     * @param  ysCoord  coordinate reader for Y coordinate array
     * @param  icXs   index in tuple of line X coordinate array
     * @param  icYs   index in tuple of line Y coordinate array
     * @param  dataStore   data store
     * @return  new plan object
     */
    public static FillPlan createPlanArrays( Surface surface, DataSpec dataSpec,
                                             DataGeom geom,
                                             FloatingArrayCoord xsCoord,
                                             FloatingArrayCoord ysCoord,
                                             int icXs, int icYs,
                                             DataStore dataStore ) {
        FillCollector fcollector =
            new ArraysFillCollector( surface, geom,
                                     xsCoord, ysCoord, icXs, icYs );
        FillData fdata =
            PlotUtil.tupleCollect( fcollector, dataSpec, dataStore );
        return new FillPlan( dataSpec, fcollector, fdata );
    }

    /**
     * Accumulator object for use with FillCollector.
     */
    private static class FillData {

        final Binner binner_;
        final int[] xlos_;
        final int[] xhis_;
        final int[] ylos_;
        final int[] yhis_;
        Point cpXlo_;
        Point cpXhi_;
        Point cpYlo_;
        Point cpYhi_;

        /**
         * Constructor.
         *
         * @param  nx  grid width in pixels
         * @param  ny  grid height in pixels
         */
        FillData( int nx, int ny ) {
            binner_ = new Binner( nx * ny );
            xlos_ = new int[ nx ];
            xhis_ = new int[ nx ];
            ylos_ = new int[ ny ];
            yhis_ = new int[ ny ];
        }
    }

    /**
     * Converts data to graphics coordinates for a FillPlan.
     * Instances of this interface are thread-safe.
     */
    @FunctionalInterface
    private static interface GraphicsConverter {

        /**
         * Maps a data position to a graphics position.
         * This does a similar job to Surface.dataToGraphics(),
         * but makes sure that all reasonable input data points end up
         * in a representative place on the output graphics plane,
         * even if they are unplottable.  Specifically, negative data values
         * will appear below the visible graphics region even for log axes.
         *
         * @param  dpos  input position in data coordinates
         * @param  gpos  updated with position in graphics coordinates on output
         * @return  true iff the resulting point can be used for a FillPlan
         */
        boolean dataToGraphics( double[] dpos, Point2D.Double gpos );
    }

    /**
     * Creates a graphics converter for a given surface.
     *
     * @param  surf  plotting surface
     * @return  converter
     */
    private static GraphicsConverter createGraphicsConverter( Surface surf ) {

        /* We know how to locate log axes on a PlaneSurface. */
        if ( surf instanceof PlaneSurface ) {
            PlaneSurface psurf = (PlaneSurface) surf;
            boolean xflip = psurf.getFlipFlags()[ 0 ];
            boolean yflip = psurf.getFlipFlags()[ 1 ];
            boolean xlog = psurf.getLogFlags()[ 0 ];
            boolean ylog = psurf.getLogFlags()[ 1 ];
            Axis xAxis = psurf.getAxes()[ 0 ];
            Axis yAxis = psurf.getAxes()[ 1 ];
            double big = Integer.MAX_VALUE / 2;
            double xMinusInf = xflip ? big : -big;
            double yMinusInf = yflip ? -big : big;
            if ( xlog || ylog ) {
                return ( dpos, gpos ) -> {
                    double dx = dpos[ 0 ];
                    double dy = dpos[ 1 ];
                    gpos.x = xlog && dx <= 0
                           ? xMinusInf
                           : xAxis.dataToGraphics( dx );
                    gpos.y = ylog && dy <= 0
                           ? yMinusInf
                           : yAxis.dataToGraphics( dy );
                    return PlotUtil.isPointReal( gpos );
                };
            }
            else {
                return ( dpos, gpos ) ->
                    surf.dataToGraphics( dpos, false, gpos ) &&
                    PlotUtil.isPointReal( gpos );
            }
        }

        /* Probably this won't be called on other surface types,
         * bug if they do just fall back to default behaviour. */
        else {
            return ( dpos, gpos ) ->
                surf.dataToGraphics( dpos, false, gpos ) &&
                PlotUtil.isPointReal( gpos );
        }
    }

    /**
     * Abstract superclass for SplitCollector to accumulate fill data.
     */
    private static abstract class FillCollector
            implements SplitCollector<TupleSequence,FillData> {

        final Surface surface_;
        final DataGeom geom_;
        final int x0_;
        final int y0_;
        final int nx_;
        final int ny_;
        final Gridder gridder_;
        final GraphicsConverter gconv_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data geom
         */
        FillCollector( Surface surface, DataGeom geom ) {
            surface_ = surface;
            geom_ = geom;
            Rectangle bounds = surface.getPlotBounds();
            x0_ = bounds.x;
            y0_ = bounds.y;
            nx_ = bounds.width;
            ny_ = bounds.height;
            gridder_ = new Gridder( nx_, ny_ );
            gconv_ = createGraphicsConverter( surface );
        }

        public FillData createAccumulator() {
            return new FillData( nx_, ny_ );
        }

        public FillData combine( FillData fdata1, FillData fdata2 ) {
            fdata1.binner_.add( fdata2.binner_ );
            for ( int ix = 0; ix < nx_; ix++ ) {
                fdata1.xlos_[ ix ] = addInt( fdata1.xlos_[ ix ],
                                             fdata2.xlos_[ ix ] );
                fdata1.xhis_[ ix ] = addInt( fdata1.xhis_[ ix ],
                                             fdata2.xhis_[ ix ] );
            }
            for ( int iy = 0; iy < ny_; iy++ ) {
                fdata1.ylos_[ iy ] = addInt( fdata1.ylos_[ iy ],
                                             fdata2.ylos_[ iy ] );
                fdata1.yhis_[ iy ] = addInt( fdata1.yhis_[ iy ],
                                             fdata2.yhis_[ iy ] );
            }
            if ( fdata1.cpXlo_ == null ) {
                fdata1.cpXlo_ = fdata2.cpXlo_;
            }
            if ( fdata1.cpXhi_ == null ) {
                fdata1.cpXhi_ = fdata2.cpXhi_;
            }
            if ( fdata1.cpYlo_ == null ) {
                fdata1.cpYlo_ = fdata2.cpYlo_;
            }
            if ( fdata1.cpYhi_ == null ) {
                fdata1.cpYhi_ = fdata2.cpYhi_;
            }
            return fdata1;
        }

        /**
         * Adds two integers together, returning Integer.MAX_VALUE
         * in case of overflow.  Input values are assumed to be positive.
         *
         * @param  i1  first value
         * @param  i2  second value
         * @return  sum or Integer.MAX_VALUE
         */
        private static int addInt( int i1, int i2 ) {
            long sum = i1 + i2;
            int isum = (int) sum;
            return isum == sum ? isum : Integer.MAX_VALUE;
        }
    }

    /**
     * FillCollector implementation for use with point clouds.
     */
    private static class PointsFillCollector extends FillCollector {

        final int icPos_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data geom
         * @param  icPos   position index
         */
        PointsFillCollector( Surface surface, DataGeom geom, int icPos ) {
            super( surface, geom );
            icPos_ = icPos;
        }

        public void accumulate( TupleSequence tseq, FillData fdata ) {
            double[] dpos = new double[ surface_.getDataDimCount() ];
            Point2D.Double gp = new Point2D.Double();
            Binner binner = fdata.binner_;
            int[] xlos = fdata.xlos_;
            int[] xhis = fdata.xhis_;
            int[] ylos = fdata.ylos_;
            int[] yhis = fdata.yhis_;
            Point cpXlo = fdata.cpXlo_;
            Point cpXhi = fdata.cpXhi_;
            Point cpYlo = fdata.cpYlo_;
            Point cpYhi = fdata.cpYhi_;
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, icPos_, dpos ) &&
                     gconv_.dataToGraphics( dpos, gp ) ) {
                    int x = (int) ( gp.x - x0_ );
                    int y = (int) ( gp.y - y0_ );
                    boolean inX = x >= 0 && x < nx_;
                    boolean inY = y >= 0 && y < ny_;
                    if ( inX && inY ) {
                        binner.increment( gridder_.getIndex( x, y ) );
                    }
                    else if ( inX ) {
                        if ( y < 0 ) {
                            xlos[ x ]++;
                            if ( cpYlo == null || y > cpYlo.y ) {
                                cpYlo = new Point( x, y );
                            }
                        }
                        else {
                            xhis[ x ]++;
                            if ( cpYhi == null || y < cpYhi.y ) {
                                cpYhi = new Point( x, y );
                            }
                        }
                    }
                    else if ( inY ) {
                        if ( x < 0 ) {
                            ylos[ y ]++;
                            if ( cpXlo == null || x > cpXlo.x ) {
                               cpXlo = new Point( x, y );
                            }
                        }
                        else {
                            yhis[ y ]++;
                            if ( cpXhi == null || x < cpXhi.x ) {
                                cpXhi = new Point( x, y );
                            }
                        }
                    }
                }
            }
            fdata.cpXlo_ = cpXlo;
            fdata.cpXhi_ = cpXhi;
            fdata.cpYlo_ = cpYlo;
            fdata.cpYhi_ = cpYhi;
        }
    }

    /**
     * FillCollector implementation for use with line data.
     */
    private static class ArraysFillCollector extends FillCollector {

        final FloatingArrayCoord xsCoord_;
        final FloatingArrayCoord ysCoord_;
        final int icXs_;
        final int icYs_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data geom
         * @param  xsCoord  coordinate reader for X coordinate array
         * @param  ysCoord  coordinate reader for Y coordinate array
         * @param  icXs   tuple index for X coordinate array
         * @param  icYs   tuple index for Y coordinate array
         */
        ArraysFillCollector( Surface surface, DataGeom geom,
                             FloatingArrayCoord xsCoord,
                             FloatingArrayCoord ysCoord,
                             int icXs, int icYs ) {
            super( surface, geom );
            xsCoord_ = xsCoord;
            ysCoord_ = ysCoord;
            icXs_ = icXs;
            icYs_ = icYs;
        }

        public void accumulate( TupleSequence tseq, FillData fdata ) {
            double[] dpos = new double[ 2 ];
            Point2D.Double gp = new Point2D.Double();
            Binner binner = fdata.binner_;
            int[] xlos = fdata.xlos_;
            int[] xhis = fdata.xhis_;
            int[] ylos = fdata.ylos_;
            int[] yhis = fdata.yhis_;
            Point cpXlo = fdata.cpXlo_;
            Point cpXhi = fdata.cpXhi_;
            Point cpYlo = fdata.cpYlo_;
            Point cpYhi = fdata.cpYhi_;
            while ( tseq.next() ) {
                double[] xs = xsCoord_.readArrayCoord( tseq, icXs_ );
                double[] ys = ysCoord_.readArrayCoord( tseq, icYs_ );
                if ( xs != null && ys != null && xs.length == ys.length ) {
                    int np = xs.length;
                    for ( int ip = 0; ip < np; ip++ ) {
                        dpos[ 0 ] = xs[ ip ];
                        dpos[ 1 ] = ys[ ip ];
                        if ( gconv_.dataToGraphics( dpos, gp ) ) {
                            int x = (int) ( gp.x - x0_ );
                            int y = (int) ( gp.y - y0_ );
                            boolean inX = x >= 0 && x < nx_;
                            boolean inY = y >= 0 && y < ny_;
                            if ( inX && inY ) {
                                binner.increment( gridder_.getIndex( x, y ) );
                            }
                            else if ( inX ) {
                                if ( y < 0 ) {
                                    xlos[ x ]++;
                                    if ( cpYlo == null || y > cpYlo.y ) {
                                        cpYlo = new Point( x, y );
                                    }
                                }
                                else {
                                    xhis[ x ]++;
                                    if ( cpYhi == null || y < cpYhi.y ) {
                                        cpYhi = new Point( x, y );
                                    }
                                }
                            }
                            else if ( inY ) {
                                if ( x < 0 ) {
                                    ylos[ y ]++;
                                    if ( cpXlo == null || x > cpXlo.x ) {
                                       cpXlo = new Point( x, y );
                                    }
                                }
                                else {
                                    yhis[ y ]++;
                                    if ( cpXhi == null || x < cpXhi.x ) {
                                        cpXhi = new Point( x, y );
                                    }
                                }
                            }
                        }
                    }
                    fdata.cpXlo_ = cpXlo;
                    fdata.cpXhi_ = cpXhi;
                    fdata.cpYlo_ = cpYlo;
                    fdata.cpYhi_ = cpYhi;
                }
            }
        }
    }
}
