package uk.ac.starlink.ttools.plot;

/**
 * Describes the point data to be plotted, including information about
 * different data subsets and corresponding plotting styles.
 *
 * @author   Mark Taylor
 * @since    4 Apr 2008
 */
public interface PlotData {

    /**
     * Returns the number of subsets in this object.
     *
     * @return  subset count
     */
    int getSetCount();

    /**
     * Returns the name for a given subset.
     *
     * @param  iset  subset index
     * @return  subset name
     */
    String getSetName( int iset );

    /**
     * Returns the plotting style for a given subset.
     *
     * @param  iset  subset index
     * @return  subset style
     */
    Style getSetStyle( int iset );

    /**
     * Returns the dimensionality of data points in this object.
     *
     * @return  length of {@link PointSequence#getPoint} return values
     */
    int getNdim();

    /**
     * Returns the number of error points per data point in this object.
     *
     * @return   length of {@link PointSequence#getErrors} return values
     */
    int getNerror();

    /**
     * Indicates whether there are or may be text labels 
     * associated with the data points in this object.
     *
     * @return  true if {@link PointSequence#getLabel} may have non-null returns
     */
    boolean hasLabels();

    /**
     * Returns an iterator over the data points in this object.
     *
     * @return  new point iterator
     */
    PointSequence getPointSequence();
}
