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
}
