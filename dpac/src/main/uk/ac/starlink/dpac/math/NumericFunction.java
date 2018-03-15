package uk.ac.starlink.dpac.math;

/**
 * Provides an ordered set of (x,y) samples from a function.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2018
 */
public interface NumericFunction {

    /**
     * Returns the number of samples.
     *
     * @return  number of samples
     */
    int getCount();

    /**
     * Returns the X value for a given sample.
     * Values returned from this method must be monotonic
     * with index <code>i</code>.
     *
     * @param  i  sample index
     * @return   X value for sample <code>i</code>
     */
    double getX( int i );

    /**
     * Returns the Y value for a given sample.
     *
     * @param  i  sample index
     * @return   Y value for sample <code>i</code>
     */
    double getY( int i );
}
