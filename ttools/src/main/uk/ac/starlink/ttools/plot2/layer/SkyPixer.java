package uk.ac.starlink.ttools.plot2.layer;

import cds.healpix.Healpix;
import cds.healpix.HashComputer;
import uk.ac.starlink.ttools.cone.CdsHealpixUtil;

/**
 * Maps positions on the unit sphere to pixel indices using a given pixel
 * scheme.
 * The current implementation uses the HEALPix nested scheme for a given
 * HEALPix level.
 *
 * <p>Instances of this class are not in general thread-safe.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2015
 */
public class SkyPixer {

    private final HashComputer hasher_;

    /**
     * Constructor.
     *
     * @param   level  HEALPix level
     */
    public SkyPixer( int level ) {
        hasher_ = Healpix.getNested( level ).newHashComputer();
    }

    /**
     * Returns the HEALPix level for this pixellisation scheme.
     *
     * @return   HEALPix level
     */
    public int getLevel() {
        return hasher_.depth();
    }

    /**
     * Returns the number of pixels used by this pixellisation scheme.
     *
     * @return   pixel count
     */
    public long getPixelCount() {
        return 12L << ( 2 * hasher_.depth() );
    }

    /**
     * Returns the sky pixel index corresponding to a given position
     * on the unit sphere.
     *
     * @param  v3  3-element vector giving a position on the celestial sphere;
     *             if the modulus of the vector is not close to unity,
     *             the result is undefined
     * @return pixel index
     */
    public long getIndex( double[] v3 ) {
        return CdsHealpixUtil.vectorToHash( hasher_, v3 );
    }

    /**
     * Indicates whether this object uses the HEALPix NESTED or RING scheme.
     *
     * @return true for nested, false for ring; currently always true
     */
    public boolean isNested() {
        return true;
    }
}
