package uk.ac.starlink.ttools.plot;

/**
 * Encapsulates what is known about the ranges along each axis of a data set.
 *
 * @author   Mark Taylor
 * @since    2 May 2008
 */
public class DataBounds {

    private final Range[] ranges_;
    private final int npoint_;
    private final int[] npoints_;

    /**
     * Constructor.
     *
     * @param  ranges  array of data ranges, one for each axis
     * @param  npoint  number of points in the data set
     * @param  npoints  array of per-subset point counts, one for each set
     */
    public DataBounds( Range[] ranges, int npoint, int[] npoints ) {
        ranges_ = (Range[]) ranges.clone();
        npoint_ = npoint;
        npoints_ = npoints;
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
     * Returns the number of points in the data set.
     *
     * @return  point count
     */
    public int getPointCount() {
        return npoint_;
    }

    /**
     * Returns an array giving the number of points per subset in the 
     * data set.
     *
     * @return   nset-element array of point counts in each subset
     */
    public int[] getPointCounts() {
        return npoints_;
    }
}
