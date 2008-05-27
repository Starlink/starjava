package uk.ac.starlink.tplot;

/**
 * Encapsulates what is known about the ranges along each axis of a data set.
 *
 * @author   Mark Taylor
 * @since    2 May 2008
 */
public class DataBounds {

    private final Range[] ranges_;
    private final int npoint_;

    /**
     * Constructor.
     *
     * @param  ranges  array of data ranges, one for each axis
     * @param  npoint  number of points in the data set
     */
    public DataBounds( Range[] ranges, int npoint ) {
        ranges_ = (Range[]) ranges.clone();
        npoint_ = npoint;
    }

    /**
     * Returns the array of data ranges, one for each axis.
     *
     * @return  data range array
     */
    public Range[] getRanges() {
        return ranges_;
    }

    /**
     * Returnst the number of points in the data set.
     *
     * @return  point count
     */
    public int getPointCount() {
        return npoint_;
    }
}
