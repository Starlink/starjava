package uk.ac.starlink.ttools.plot2.layer;

import java.awt.geom.Point2D;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * AuxReader implementation that extends ranges simply by sampling
 * a given scalar floating coordinate for all plottable points.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2015
 */
public class FloatingCoordAuxReader implements AuxReader {

    private final FloatingCoord coord_;
    private final int icol_;
    private final DataGeom geom_;
    private final boolean visibleOnly_;
    private final double[] dpos_;
    private final Point2D.Double gpos_;

    /**
     * Constructor.
     *
     * @param  coord  coordinate reader
     * @param  icol   column index in tuple sequence corresponding to value
     * @param  geom   converts data to graphics coordinates
     * @param  visibleOnly  true to include only points visible in the
     *                      current plot bounds, false for all potentially
     *                      plottable points
     */
    public FloatingCoordAuxReader( FloatingCoord coord, int icol,
                                   DataGeom geom, boolean visibleOnly ) {
        coord_ = coord;
        icol_ = icol;
        geom_ = geom;
        visibleOnly_ = visibleOnly;
        dpos_ = new double[ geom.getDataDimCount() ];
        gpos_ = new Point2D.Double();
    }

    public void updateAuxRange( Surface surface, TupleSequence tseq,
                                Range range ) {

        /* Convert data to graphics coordinates.  The resulting values are
         * not used, but this determines whether the points are plottable. */
        if ( geom_.readDataPos( tseq, 0, dpos_ ) &&
             surface.dataToGraphics( dpos_, visibleOnly_, gpos_ ) ) {

            /* Read the coordinate value. */
            double value = coord_.readDoubleCoord( tseq, icol_ );

            /* Extend the submitted range accordingly. */
            range.submit( value );
        }
    }
}
