package uk.ac.starlink.ttools.plot2.layer;

import gov.fnal.eag.healpix.PixTools;
import javax.vecmath.Vector3d;

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

    private final int level_;
    private final long nside_;
    private final PixTools pixTools_;
    private final Vector3d vector3d_;

    /**
     * Constructor.
     *
     * @param   level  HEALPix level
     */
    public SkyPixer( int level ) {
        level_ = level;
        nside_ = 1L << level;
        pixTools_ = new PixTools();
        vector3d_ = new Vector3d();
    }

    /**
     * Returns the HEALPix level for this pixellisation scheme.
     *
     * @return   HEALPix level
     */
    public int getLevel() {
        return level_;
    }

    /**
     * Returns the number of pixels used by this pixellisation scheme.
     *
     * @return   pixel count
     */
    public long getPixelCount() {
        return 12L << 2 * level_;
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
        vector3d_.x = v3[ 0 ];
        vector3d_.y = v3[ 1 ];
        vector3d_.z = v3[ 2 ];
        return pixTools_.vect2pix_nest( nside_, vector3d_ );
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
