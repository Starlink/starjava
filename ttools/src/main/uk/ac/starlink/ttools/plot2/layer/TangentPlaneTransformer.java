package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;

/**
 * Performs normalised 3D coordinate transformations equivalent
 * to displacements from the origin in the tangent plane.
 *
 * <p>This object can deal with the data-&gt;view transformation
 * represented by a {@link uk.ac.starlink.ttools.plot2.geom.SkyDataGeom}.
 * The input tangent position is in view coordinates, but the
 * input displacements on that plane correspond to displacements
 * along the axes of data coordinates.  That may seem baroque, but
 * it corresponds to what sky-based {@link MultiPointCoordSet}
 * implementations are likely to be able to supply.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public class TangentPlaneTransformer {
    private final double[] xyz0_;
    private final SkyDataGeom geom_;

    /**
     * Constructs a transformer for the tangent plane centred at a given
     * normalised vector.
     *
     * @param  xyz0  normalised 3D coordinates in the view coordinate system
     *               at the centre of tangent plane
     * @param  geom  geom object representing data-&gt;view coordinate system
     *               rotation
     */
    public TangentPlaneTransformer( double[] xyz0, SkyDataGeom geom ) {
        geom_ = geom;

        /* Store the coordinates in the data coordinate system, which
         * is what we need for later calculations.  We have to recover
         * these from the view coordinates which is what the client code
         * will have supplied (it is unlikely to have the data coords
         * available). */
        xyz0_ = (double[]) xyz0.clone();
        geom.unrotate( xyz0_ );
    }

    /**
     * Determines the normalised 3D position in view coordinates
     * of a position at given coordinates in this object's tangent plane.
     *
     * @param  xi   horizontal displacement in data coordinates
     *              from tangent plane origin (tangent point)
     * @param  eta  vertical displacement in data coordinates
     *              from tangent plane origin (tangent point)
     * @param   xyz1  3-element array into which normalised 3D coordinates
     *                of result position in view coordinates are written
     */
    public void displace( double xi, double eta, double[] xyz1 ) {
        dtp2v( xi, eta, xyz0_[ 0 ], xyz0_[ 1 ], xyz0_[ 2 ], xyz1 );
        geom_.rotate( xyz1 );
        assert Math.abs( ( xyz1[ 0 ] * xyz1[ 0 ] +
                           xyz1[ 1 ] * xyz1[ 1 ] +
                           xyz1[ 2 ] * xyz1[ 2 ] ) - 1 ) < 1e-8;
    }

    /**
     * Tangent plane to direction cosines routine.
     * This is function DTP2V from SLALIB.
     *
     * @param  xi  X tangent plane coordinate in radians
     * @param  eta  Y tangent plane coordinate in radians
     * @param  x  X direction cosine of tangent point
     * @param  y  Y direction cosine of tangent point
     * @param  z  Z direction cosine of tangent point
     * @param   3-element array giving direction cosines of transformed point
     */
    private static void dtp2v( double xi, double eta,
                               double x, double y, double z,
                               double[] v ) {
        double f = Math.sqrt( 1 + xi * xi + eta * eta );
        double r = Math.hypot( x, y );
        if ( r == 0 ) {
            r = 1d-20;
            x = r;
        }
        double f1 = 1.0 / f;
        double r1 = 1.0 / r;
        v[ 0 ] = ( x - ( xi * y + eta * x * z ) * r1 ) * f1;
        v[ 1 ] = ( y + ( xi * x - eta * y * z ) * r1 ) * f1;
        v[ 2 ] = ( z + eta * r ) * f1;
    }
}
