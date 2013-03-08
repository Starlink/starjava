package uk.ac.starlink.ttools.plot2.layer;

/**
 * Performs normalised 3D coordinate transformations equivalent
 * to displacements from the origin in the tangent plane.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public class TangentPlaneTransformer {
    private final double x0_;
    private final double y0_;
    private final double z0_;

    /**
     * Constructs a transformer for the tangent plane centred at a given
     * normalised vector.
     *
     * @param  xyz0  normalised 3D coordinates at centre of tangent plane
     */
    public TangentPlaneTransformer( double[] xyz0 ) {
        x0_ = xyz0[ 0 ];
        y0_ = xyz0[ 1 ];
        z0_ = xyz0[ 2 ];
    }

    /**
     * Determines the normalised 3D coordinates of a position at a given
     * coordinates in this object's tangent plane.
     *
     * @param  xi  horizontal displacement from tangent plane origin
     *             (tangent point)
     * @param  eta  vertical displacement from tangent plane origin
     *              (tangent point)
     * @param   xyz1  3-element array into which normalised 3D coordinates
     *                of result position are written
     */
    public void displace( double xi, double eta, double[] xyz1 ) {
        dtp2v( xi, eta, x0_, y0_, z0_, xyz1 );
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
