package uk.ac.starlink.topcat.plot;

/**
 * Encapsulates a list of N-dimensional points in data space.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 June 2004
 */
public interface Points {

    /**
     * Returns the dimensionality of this point set.
     *
     * @return  number of coordinates in each point
     */
    int getNdim();

    /**
     * Returns the number of points in this dataset.
     *
     * @return  numer of points
     */
    int getCount();

    /**
     * Reads the coordinates of one of the stored points.
     *
     * @param   ipoint  point index
     * @param   coords  an array to receive the data; must be at least
     *          <code>ndim</code> elements
     */
    void getCoords( int ipoint, double[] coords );

    /**
     * Indicates which dimensions may have associated error information.
     * Elements of the returned array which are false indicate that
     * the corresponding elements of the lower and upper error arrays 
     * read by {@link #getErrors} will be zero for all points.
     * The returned array may be of length <code>ndim</code>, but is
     * not necessarily so.
     *
     * @return  map of flags indicating which dimensions have error information
     */
    boolean[] hasErrors();

    /**
     * Reads the error values for one of the stored points.
     * The lower and upper error values are read respectively into 
     * two supplied arrays.  These arrays must have (at least) the 
     * same length as that of the array returned by {@link #hasErrors}.
     *
     * @param   ipoint  point index
     * @param   loErrs  an array to receive the lower error values
     * @param   hiErrs  an array to receive the upper error values
     */
    void getErrors( int ipoint, double[] loErrs, double[] hiErrs );
}
