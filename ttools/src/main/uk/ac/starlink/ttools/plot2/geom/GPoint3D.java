package uk.ac.starlink.ttools.plot2.geom;

import java.awt.geom.Point2D;

/**
 * Extends Point2D.Double to include a Z coordinate.
 * This third coordinate is not in graphics coordinates as such,
 * but it represents the depth of the point in the Z stack.
 * There is no limit on the Z coordinate range, but lower values are
 * closer to the viewer.
 *
 * @author   Mark Taylor
 * @since    19 Jul 2018
 */
public class GPoint3D extends Point2D.Double {

    /** The Z coordinate of this Point3D. */
    public double z;

    /**
     * Constructs a point at the origin.
     */
    public GPoint3D() {
        this( 0, 0, 0 );
    }

    /**
     * Constructs a point with given coordinates.
     *
     * @param  x   X graphics coordinate
     * @param  y   Y graphics coordinae
     * @param  z   depth coordinate
     */
    public GPoint3D( double x, double y, double z ) {
        super( x, y );
        this.z = z;
    }
}
