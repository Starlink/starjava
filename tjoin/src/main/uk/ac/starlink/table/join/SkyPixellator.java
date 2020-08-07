package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DescribedValue;

/**
 * Provides a pixellisation of the celestial sphere.
 * Usually it is necessary to call {@link #setScale} before an instance
 * of this class can be used.
 *
 * @author   Mark Taylor
 * @since    5 Sep 2011
 */
public interface SkyPixellator {

    /**
     * Sets the characteristic angular scale for this pixellator.
     * Pixels should be approximately the size given, so that a larger
     * scale corresponds to larger pixel sizes.  The details of pixel size
     * are determined by the details of the pixellation scheme however.
     *
     * @param  scale  pixel length scale in radians
     */
    void setScale( double scale );

    /**
     * Returns the most recently set angular scale.
     *
     * @return   pixel length scale in radians
     */
    double getScale();

    /**
     * Returns an array of objects representing pixels in a given region.
     * The parameters specify a small circle on the sphere; any pixels
     * which overlap this circle must be returned (additional pixels
     * may also be returned).
     * The output objects are of some opaque type, but must implement
     * the <code>equals</code> and <code>hashCode</code> methods
     * appropriately, so that objects returned from one call can be
     * compared for identity with objects returned from a subsequent call. 
     * This comparability is only guaranteed to work if the pixel scale
     * is not changed in between calls.
     *
     * @param  alpha  right ascension of circle centre in radians
     * @param  delta  declination of circle centre in radians
     * @param  radius   radius of circle in radians
     * @return   array of opaque but comparable pixel objects
     */
    Object[] getPixels( double alpha, double delta, double radius );

    /**
     * Returns a parameter whose value may be adjusted to alter the
     * pixellisation scale.  This is not necessarily the same as the
     * scale attribute (its value need not be an angle).
     *
     * @return   tuning parameter
     */
    DescribedValue getTuningParameter();
}
