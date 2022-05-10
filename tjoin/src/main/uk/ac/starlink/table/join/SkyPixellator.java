package uk.ac.starlink.table.join;

import java.util.function.Supplier;
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
     * Returns a parameter whose value may be adjusted to alter the
     * pixellisation scale.  This is not necessarily the same as the
     * scale attribute (its value need not be an angle).
     *
     * @return   tuning parameter
     */
    DescribedValue getTuningParameter();

    /**
     * Returns a factory for variable radius pixel calculators
     * based on the current settings of this object.
     *
     * @return  immutable factory for pixel calculators;
     *          subsequent changes to this object will not affect
     *          the objects it supplies
     */
    Supplier<VariableRadiusConePixer> createVariableRadiusPixerFactory();

    /**
     * Returns a factory for fixed radius pixel calculators
     * based on the current settings of this object.
     *
     * @param   radius   cone radius in radians
     * @return  immutable factory for pixel calculators;
     *          subsequent changes to this object will not affect
     *          the objects it supplies
     */
    Supplier<FixedRadiusConePixer>
            createFixedRadiusPixerFactory( double radius );
}
