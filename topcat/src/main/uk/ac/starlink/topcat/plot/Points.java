package uk.ac.starlink.topcat.plot;

/**
 * Encapsulates a list of X,Y coordinates in data space.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public class Points {

    private final double[] x_;
    private final double[] y_;
    private final int count_;

    /**
     * Constructs a new Points object from X and Y coordinate vectors.
     * Both <tt>x</tt> and <tt>y</tt> must have the same number of
     * elements.
     * 
     * @param  x  X coordinates
     * @param  y  Y coordinates
     */
    public Points( double[] x, double[] y ) {
        x_ = x;
        y_ = y;
        count_ = x.length;
        if ( y.length != count_ ) {
            throw new IllegalArgumentException( 
                          "x and y vectors are different lengths" );
        }
    }

    /**
     * Returns the number of points in this dataset.
     *
     * @return  length of the X and Y array
     */
    public int getCount() {
        return count_;
    }

    /**
     * Returns the array of X coordinates
     *
     * @return  x values
     */
    public double[] getXVector() {
        return x_;
    }

    /**
     * Returns the array of Y coordinates.
     *
     * @return  y values
     */
    public double[] getYVector() {
        return y_;
    }
}
