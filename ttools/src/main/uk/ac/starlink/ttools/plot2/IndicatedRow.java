package uk.ac.starlink.ttools.plot2;

import java.util.Arrays;

/**
 * Represents a row from a dataset in relation to a reference position.
 * 
 * @author   Mark Taylor
 * @since    21 Nov 2014
 */
public class IndicatedRow {

    private final long index_;
    private final double distance_;
    private final double[] dpos_;

    /**
     * Constructor.
     *
     * @param  index  row index
     * @param  distance   distance from reference position
     *                    in graphics coordinates
     * @param  dpos     row position in data coordinates
     */
    public IndicatedRow( long index, double distance, double[] dpos ) {
        index_ = index;
        distance_ = distance;
        dpos_ = dpos.clone();
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
     * Returns the distance of the row position from the reference position.
     *
     * @return  distance
     */
    public double getDistance() {
        return distance_;
    }

    /**
     * Returns the row position in data coordinates.
     *
     * @return   row position coordinates
     */
    public double[] getDataPos() {
        return dpos_.clone();
    }

    @Override
    public String toString() {
        return "index=" + index_
             + ",distance=" + distance_ 
             + ",dpos=" + Arrays.toString( dpos_ );
    }
}
