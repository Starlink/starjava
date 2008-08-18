package uk.ac.starlink.ttools.plot;

/**
 * Contains data for a single point in a {@link PointSequence}.
 * Used by {@link ArrayPlotData}.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2008
 */
public class PointData {
    private final double[] point_;
    private final double[][] errors_;
    private final String label_;
    private final boolean[] isIncluded_;

    /**
     * Constructor.
     *
     * @param   point   point coordinates
     * @param   errors  error coordinates array
     * @param   label   text label, or null
     * @param   isIncluded  array of subset inclusion flags
     */
    public PointData( double[] point, double[][] errors, String label,
                      boolean[] isIncluded ) {
        point_ = point;
        errors_ = errors;
        label_ = label;
        isIncluded_ = isIncluded;
    }

    /**
     * Returns the coordinates of this point.
     *
     * @return  point
     */
    public double[] getPoint() {
        return point_;
    }

    /**
     * Returns the coordinates of the error extrema associated with this point.
     *
     * @return  error coordinates array
     */
    public double[][] getErrors() {
        return errors_;
    }

    /**
     * Returns the text label associated with this point.
     *
     * @return  text label, or null
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the inclusion status of a given subset.
     *
     * @param   iset   subset index
     * @return   true iff this point is included in subset <code>iset</code>
     */
    public boolean isIncluded( int iset ) {
        return isIncluded_[ iset ];
    }
}
