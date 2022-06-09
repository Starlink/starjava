package uk.ac.starlink.ttools.plot2.layer;

/**
 * Defines a matched pair of vectors.
 *
 * @author   Mark Taylor
 * @since    9 Jun 2022
 */
public interface XYArrayData {

    /**
     * Returns the number of X, Y pairs avilable.
     *
     * @return  shared vector length
     */
    int getLength();

    /**
     * Returns one of the X elements.
     *
     * @param  i  index
     * @return  X value
     */
    double getX( int i );

    /**
     * Returns one of the Y elements.
     *
     * @param  i   index
     * @return Y value
     */
    double getY( int i );
}
