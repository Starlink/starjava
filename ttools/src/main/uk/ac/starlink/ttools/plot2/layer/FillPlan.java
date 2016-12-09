package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;


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
     * @param  binner  contains density map pixel counts
     * @param  gridder   encapsulates geometry for grid indexing
     * @param  xlos    bins counting all points above each pixel column
     * @param  xhis    bins counting all points below each pixel column
     * @param  ylos    bins counting all points to left of each pixel row
     * @param  yhis    bins counding all points to right of each pixel row
     * @param  cpXlo   closest point to the lower X boundary
     *                 that falls outside the grid
     * @param  cpXhi   closest point to the upper X boundary
     *                 that falls outside the grid
     * @param  cpYlo   closest point to the lower Y boundary
     *                 that falls outside the grid
     * @param  cpYhi   closest point to the upper Y boundary
     *                 that falls outside the grid
     * @param  geom   data geom
     * @param  dataSpec  data specification
     * @param  surface  plot surface
     */
    FillPlan( Binner binner, Gridder gridder,
              int[] xlos, int[] xhis, int[] ylos, int[] yhis,
              Point cpXlo, Point cpXhi, Point cpYlo, Point cpYhi,
              DataGeom geom, DataSpec dataSpec, Surface surface ) {
        binner_ = binner;
        gridder_ = gridder;
        xlos_ = xlos;
        xhis_ = xhis;
        ylos_ = ylos;
        yhis_ = yhis;
        cpXlo_ = cpXlo;
        cpXhi_ = cpXhi;
        cpYlo_ = cpYlo;
        cpYhi_ = cpYhi;
        geom_ = geom;
        dataSpec_ = dataSpec;
        surface_ = surface;
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
     * Creates a fill plan object.
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
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point2D.Double gp = new Point2D.Double();
        Rectangle bounds = surface.getPlotBounds();
        Gridder gridder = new Gridder( bounds.width, bounds.height );
        Binner binner = new Binner( gridder.getLength() );
        int x0 = bounds.x;
        int y0 = bounds.y;
        int nx = bounds.width;
        int ny = bounds.height;
        int[] xlos = new int[ nx ];
        int[] xhis = new int[ nx ];
        int[] ylos = new int[ ny ];
        int[] yhis = new int[ ny ];
        Point cpXlo = null;
        Point cpXhi = null;
        Point cpYlo = null;
        Point cpYhi = null;
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, icPos, dpos ) &&
                 surface.dataToGraphics( dpos, false, gp ) &&
                 PlotUtil.isPointReal( gp ) ) {
                int x = (int) gp.x - x0;
                int y = (int) gp.y - y0;
                boolean inX = x >= 0 && x < nx;
                boolean inY = y >= 0 && y < ny;
                if ( inX && inY ) {
                    binner.increment( gridder.getIndex( x, y ) );
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
        return new FillPlan( binner, gridder, xlos, xhis, ylos, yhis,
                             cpXlo, cpXhi, cpYlo, cpYhi,
                             geom, dataSpec, surface );
    }
}
