package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Shape;
import java.awt.geom.Point2D;
import uk.ac.starlink.ttools.plot.Range;

/**
 * Sky projection.  Defines the mapping of normalised X,Y,Z coordinates
 * (direction cosines) to dimensionless coordinates on a 2D plane.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public interface Projection {

    /**
     * Returns the projection name.
     *
     * @return   user-directed projection name
     */
    String getProjectionName();

    /**
     * Returns a short description of the projection.
     *
     * return  projection description
     */
    String getProjectionDescription();

    /**
     * Transforms a sky position to a plane position.
     *
     * @param  rx  normalised 3D X coordinate
     * @param  ry  normalised 3D Y coordinate
     * @param  rz  normalised 3D Z coordinate
     * @param  pos  point object into which projected dimensionless X,Y 
     *              coordinates will be written on success
     * @return  true if transformation succeeded
     */
    boolean project( double rx, double ry, double rz, Point2D.Double pos );

    /**
     * Transforms a plane position to a sky position.
     *
     * @param  pos  contains dimensionless X,Y coordinates of plane position
     * @param  r3   3-element array into which normalised X,Y,Z sky coordinates
     *              will be written on success
     * @return  true if transformation succeeded
     */
    boolean unproject( Point2D.Double pos, double[] r3 );

    /**
     * Returns the shape which encloses all the plane positions to which
     * legal sky coordinates can be projected.
     * Typically this has linear dimensions of the order of PI.
     *
     * @return   projected sky shape
     */
    Shape getProjectionShape();

    /**
     * Attempts to return a rotation matrix corresponding to moving the
     * cursor between two plane positions.
     * Ideally this should do the same thing as <code>projRotate</code>,
     * for both positions on the sky, and provide some other intuitive
     * behaviour if one or both is out of the projection range.
     *
     * <p>Null may be returned if this projection does not support
     * rotation.
     *
     * @param    rotmat  initial rotation matrix
     * @param    pos0   initial cursor position
     * @param    pos1   destination cursor position
     * @return  destination rotation matrix, or null
     */
    double[] cursorRotate( double[] rotmat, Point2D.Double pos0,
                                            Point2D.Double pos1 );

    /**
     * Attempts to return a rotation matrix that will transform
     * a sky position from one plane position to another.
     *
     * <p>Consider a sky point S, rotated by an initial rotation
     * matrix <code>rotmat</code> to S', which when projected by
     * this projection lands on the plane at <code>pos0</code>.
     * This method attempts to determine a rotation matrix
     * which when used instead of <code>rotmat</code> would
     * end up with the rotated and projected point at <code>pos1</code>.
     * 
     * <p>Null may be returned if this projection does not support projection.
     *
     * @param   rotmat  initial rotation matrix
     * @param   pos0   initial projected position
     * @param   pos1   destination projected position
     * @return  destination rotation matrix, or null
     */
    double[] projRotate( double[] rotmat, Point2D.Double pos0,
                                          Point2D.Double pos1 );

    /**
     * Indicates whether ranges should be provided to generate a SkyAspect.
     * If supplied field of view arguments are sufficient,
     * or if a default aspect is always produced, return false.
     *
     * @param  reflect  whether requested aspect will be reflected
     * @param  r3   central position of field of view (may be null)
     * @param  radiusRad  radius of field of view (may be NaN)
     * @return  true if ranges would be useful given the other arguments
     * @see   uk.ac.starlink.ttools.plot2.SurfaceFactory#readRanges
     */
    boolean useRanges( boolean reflect, double[] r3, double radiusRad );

    /**
     * Creates a SkyAspect from configuration information.
     * Either the supplied field of view or data ranges may be used, or neither.
     *
     * @param  reflect  whether requested aspect will be reflected
     * @param  r3   central position of field of view (may be null)
     * @param  radiusRad  radius of field of view (may be NaN)
     * @param  vxyzRanges  definite ranges for normalised X,Y,Z coordinates
     *                     acquired from data
     * @return   new sky aspect
     * @see  uk.ac.starlink.ttools.plot2.SurfaceFactory#createAspect
     */
    SkyAspect createAspect( boolean reflect, double[] r3, double radiusRad,
                            Range[] vxyzRanges );
}
