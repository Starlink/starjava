package uk.ac.starlink.ttools.plot2.layer;

import java.awt.geom.Point2D;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
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
    private final Scaling scaling_;

    /**
     * Constructor.
     *
     * @param  coord  coordinate reader
     * @param  icol   column index in tuple sequence corresponding to value
     * @param  geom   converts data to graphics coordinates
     * @param  visibleOnly  true to include only points visible in the
     *                      current plot bounds, false for all potentially
     *                      plottable points
     * @param  scaling  scaling
     */
    public FloatingCoordAuxReader( FloatingCoord coord, int icol,
                                   DataGeom geom, boolean visibleOnly,
                                   Scaling scaling ) {
        coord_ = coord;
        icol_ = icol;
        geom_ = geom;
        visibleOnly_ = visibleOnly;
        scaling_ = scaling;
    }

    public int getCoordIndex() {
        return icol_;
    }

    public ValueInfo getAxisInfo( DataSpec dataSpec ) {
        ValueInfo[] infos = dataSpec.getUserCoordInfos( icol_ );
        return infos.length == 1 ? infos[ 0 ] : null;
    }

    public Scaling getScaling() {
        return scaling_;
    }

    public void adjustAuxRange( Surface surface, DataSpec dataSpec,
                                DataStore dataStore, Object[] plans,
                                Ranger ranger ) {
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );

        /* If no positional coordinates, just submit every value. */
        if ( geom_ == null ) {
            while ( tseq.next() ) {
                ranger.submitDatum( coord_.readDoubleCoord( tseq, icol_ ) );
            }
        }

        /* If there are positional coordinates, check each value to see
         * whether it is plottable, and only submit the ones that are. */
        else {
            double[] dpos = new double[ geom_.getDataDimCount() ];
            Point2D.Double gpos = new Point2D.Double();
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, 0, dpos ) &&
                     surface.dataToGraphics( dpos, visibleOnly_, gpos ) &&
                     ( visibleOnly_ || PlotUtil.isPointFinite( gpos ) ) ) {
                    ranger.submitDatum( coord_.readDoubleCoord( tseq, icol_ ) );
                }
            }
        }
    }
}
