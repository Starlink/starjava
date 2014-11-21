package uk.ac.starlink.ttools.plot2;

/**
 * Represents a row from a dataset in relation to a reference position,
 * by aggregating a row index and the distance from the point.
 * 
 * @author   Mark Taylor
 * @since    21 Nov 2014
 */
public class IndicatedRow {

    private final long index_;
    private final double distance_;

    /**
     * Constructor.
     *
     * @param  index  row index
     * @param  distance   distance from reference position
     */
    public IndicatedRow( long index, double distance ) {
        index_ = index;
        distance_ = distance;
    }

    /**
     * Returns the row index.
     *
     * @return  row index
     */
    public long getIndex() {
        return index_;
    }

    /**
     * Returns the distance from the reference position.
     *
     * @return  distance
     */
    public double getDistance() {
        return distance_;
    }

    @Override
    public String toString() {
        return "index=" + index_ + ",distance=" + distance_;
    }
}
