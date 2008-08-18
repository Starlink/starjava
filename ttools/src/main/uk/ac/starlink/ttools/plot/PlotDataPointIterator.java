package uk.ac.starlink.ttools.plot;

import java.awt.Point;

/**
 * PointIterator implementation based on a {@link PlotData} object.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2008
 */
public class PlotDataPointIterator extends PointIterator {

    private final PlotData data_;
    private final PointPlacer placer_;
    private final int nset_;
    private final int[] point_;
    private PointSequence pseq_;
    private int ip_ = -1;

    /**
     * Constructor.
     *
     * @param  data  object supplying point data
     */
    public PlotDataPointIterator( PlotData data, PointPlacer placer ) {
        data_ = data;
        placer_ = placer;
        nset_ = data.getSetCount();
        pseq_ = data.getPointSequence();
        point_ = new int[ 3 ];
    }

    protected int[] nextPoint() {
        while ( pseq_ != null && pseq_.next() ) {
            ip_++;
            boolean use = false;
            for ( int is = 0; is < nset_ && ! use; is++ ) {
                use = use || pseq_.isIncluded( is );
            }
            if ( use ) {
                Point xy = placer_.getXY( pseq_.getPoint() );
                if ( xy != null ) {
                    point_[ 0 ] = ip_;
                    point_[ 1 ] = xy.x;
                    point_[ 2 ] = xy.y;
                    return point_;
                }
            }
        }
        pseq_.close();
        pseq_ = null;
        return null;
    }
}
