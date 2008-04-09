package uk.ac.starlink.tplot;

import java.awt.Point;

/**
 * PointIterator implementation based on a {@link PlotData} object.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2008
 */
public abstract class PlotDataPointIterator extends PointIterator {

    private final PlotData data_;
    private final int nset_;
    private final int[] point_;
    private PointSequence pseq_;
    private int ip_ = -1;

    /**
     * Constructor.
     *
     * @param  data  object supplying point data
     */
    public PlotDataPointIterator( PlotData data ) {
        data_ = data;
        nset_ = data.getSetCount();
        pseq_ = data.getPointSequence();
        point_ = new int[ 3 ];
    }

    /**
     * Supplies the coordinates of the current point in a supplied 
     * PointSequence, if it appears in the data set iterated over by 
     * this object.
     * If it should be excluded from the iteration, then null should be
     * returned.
     *
     * @param  pseq  point sequence whose current point is to be examined
     * @return   screen coordinates of point, or null if point does not appear
     */
    protected abstract Point getXY( PointSequence pseq );

    protected int[] nextPoint() {
        while ( pseq_ != null && pseq_.next() ) {
            ip_++;
            boolean use = false;
            for ( int is = 0; is < nset_ && ! use; is++ ) {
                use = use || pseq_.isIncluded( is );
            }
            if ( use ) {
                Point xy = getXY( pseq_ );
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
