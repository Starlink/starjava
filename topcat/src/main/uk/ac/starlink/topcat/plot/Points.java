package uk.ac.starlink.topcat.plot;

/**
 * Encapsulates a list of N-dimensional points in data space, perhaps with
 * additional information about error bounds.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 June 2004
 */
public interface Points {

    /**
     * Returns the number of points in this dataset.
     *
     * @return  numer of points
     */
    int getCount();

    /**
     * Returns the length of the coordinate array for each point.
     *
     * @return  number of coordinate values at each point
     */
    int getNdim();

    /**
     * Reads the coordinates of one of the stored points.
     * The returned array may be modified by subsequent calls to this method.
     * The caller is also permitted to modify it.
     *
     * @param   ipoint  point index
     * @return  coords  an ndim-element array containing point coordinates
     */
    double[] getPoint( int ipoint );

    /**
     * Returns the number of error points returned for each point.
     *
     * @return  number of error values at each point
     */
    int getNerror();

    /**
     * Reads the errors for one of the stored points.
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
     * @param   ipoint  point index
     * @return  double[nerr][ndim] array with error extremum coordinates
     */
    double[][] getErrors( int ipoint );

    /**
     * Indicates whether a string label is associated with some points.
     *
     * @return  true if {@link #getLabel} may return a non-null value for
     *          any point
     */
    boolean hasLabels();

    /**
     * Returns a string associated with a given point.
     * May only return a non-null value if {@link #hasLabels} returns true.
     *
     * @param  ipoint  point index
     * @return  label associated with points
     */
    String getLabel( int ipoint );
}
