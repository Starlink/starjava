package uk.ac.starlink.ttools.plot2.geom;

/**
 * Characterises a field of view on the sky for presentation to the user.
 * The values in degrees should be rounded in decimal to
 * a level of precision suitable for presentation to the user,
 * given the pixel precision of the known plotting surface.
 * The values are therefore approximate, and may in any case be
 * rather rough depending on the geometry involved (which may not
 * lend itself to description as a cone).
 *
 * @author   Mark Taylor
 * @since    25 Jul 2017
 */
public class SkyFov {

    private final double lonDeg_;
    private final double latDeg_;
    private final double radiusDeg_;

    /**
     * Constructor.
     *
     * @param   lonDeg  central longitude in degrees
     * @param   latDeg  central latitude in degrees
     * @param   radiusDeg  radius in degrees
     */
    public SkyFov( double lonDeg, double latDeg, double radiusDeg ) {
        lonDeg_ = lonDeg;
        latDeg_ = latDeg;
        radiusDeg_ = radiusDeg;
    }

    /**
     * Returns central longitude in degrees.
     *
     * @return  longitude
     */
    public double getLonDeg() {
        return lonDeg_;
    }

    /**
     * Returns central latitude in degrees.
     *
     * @return   latitude
     */
    public double getLatDeg() {
        return latDeg_;
    }

    /**
     * Returns field of view radius in degrees.
     *
     * @return   radius
     */
    public double getRadiusDeg() {
        return radiusDeg_;
    }
}
