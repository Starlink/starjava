package uk.ac.starlink.ttools.plot;

/**
 * Iterator over point data described by {@link PlotData}.
 *
 * @author   Mark Taylor
 * @since    4 Apr 2008
 */
public interface PointSequence {

    /**
     * Attempts to advance the current row to the next one.
     * If <code>true</code> is returned the attempt has been successful,
     * and if <code>false</code> is returned there are no more rows in
     * this sequence.  Since the initial position of this sequence is before
     * the first row, this method must be called before calling any of the
     * data access methods.
     *
     * @return  true if row advance has succeeded, false for end of sequence
     */
    boolean next();

    /**
     * Reads the coordinates of the current point.
     * The returned array may be modified by subsequent calls to this method.
     * The caller is also permitted to modify it.
     *
     * @return  coords  an ndim-element array containing point coordinates
     */
    double[] getPoint();

    /**
     * Reads the errors for the current row.
     * The returned value is an array of <code>nerror</code> double[] arrays,
     * each of which has <code>ndim</code> elements and represents the
     * coordinates of the end of an error bar.  If any of these
     * coordinate arrays is <code>null</code>, it represents an error
     * bar of zero size, that is one whose end sits right on the data point.
     * The ordering of these points is up to the user of this object,
     * but typically they will be in pairs, e.g. (xlo,xhi, ylo,hi, ...).
     * The content of the returned double[][] array and of its elements
     * may be modified by subsequent calls to this method.  The caller is
     * also permitted to modify these.
     *
     * @return  double[nerr][ndim] array with error extremum coordinates
     */
    double[][] getErrors();

    /**
     * Returns any text label associated with the current point.
     *
     * @return  text label, or null
     */
    String getLabel();

    /**
     * Indicates whether the current row is included in the given subset.
     *
     * @return  true  iff set <code>iset</code> is included
     */
    boolean isIncluded( int iset );

    /**
     * Call when this sequence is no longer required.
     */
    void close();
}
