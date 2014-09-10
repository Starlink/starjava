package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Range;

/**
 * Toy projection class that provides a rotatable sphere.
 * May not be completely working.  Use SinProjection instead.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public class HemisphereProjection implements Projection {

    private final boolean upNorth_;
    private static final double[] XVEC = new double[] { 1., 0., 0. };
    private static final double[] YVEC = new double[] { 0., 1., 0. };
    private static final double[] ZVEC = new double[] { 0., 0., 1. };

    /**
     * Constructs a HemisphereProjection with default characteristics.
     */
    public HemisphereProjection() {
        this( false );
    }

    /**
     * Constructs a HemisphereProjection optionally
     * with north fixed pointing up.
     *
     * @param  upNorth  whether north is fixed to align with the
     *                  screen Y direction
     */
    public HemisphereProjection( boolean upNorth ) {
        upNorth_ = upNorth;
    }

    public String getProjectionName() {
        return "Hemisphere";
    }

    public String getProjectionDescription() {
        return "no-frills, possibly buggy projection onto a rotatable sphere";
    }

    public boolean project( double rx, double ry, double rz,
                            Point2D.Double pos ) {
        if ( rx >= 0 ) {
            pos.x = ry;
            pos.y = rz;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean unproject( Point2D.Double ppos, double[] r3 ) {
        double px = ppos.x;
        double py = ppos.y;
        double pz2 = 1.0 - px * px - py * py;
        if ( pz2 >= 0.0 && pz2 <= 1.0 ) {
            r3[ 0 ] = Math.sqrt( pz2 );
            r3[ 1 ] = px;
            r3[ 2 ] = py;
            return true;
        }
        else {
            return false;
        }
    }

    public Shape getProjectionShape() {
        return new Ellipse2D.Double( -1, -1, 2, 2 );
    }

    public double[] cursorRotate( double[] rot0, Point2D.Double pos0,
                                                 Point2D.Double pos1 ) {
        double phi = ( pos1.x - pos0.x );
        double psi = ( pos1.y - pos0.y );
        double[] rm = rot0;
        rm = Matrices.mmMult( rotateAround( new double[] { 0, 0, -phi } ), rm );
        rm = Matrices.mmMult( rotateAround( new double[] { 0, psi, 0 } ), rm );
        // Can't get this working.
        if ( upNorth_ ) {
            double theta = Math.atan2( rm[ 5 ], rm[ 2 ] );
            double[] correction =
                rotateAround( Matrices.mvMult( Matrices.invert( rm ),
                                               new double[] { 0, 0, theta } ) );
            rm = Matrices.mmMult( rm, correction );
        }
        return rm;
    }

    public double[] projRotate( double[] rot0, Point2D.Double pos0,
                                               Point2D.Double pos1 ) {
        double[] r0 = new double[ 3 ];
        double[] r1 = new double[ 3 ];
        if ( unproject( pos0, r0 ) && unproject( pos1, r1 ) ) {
            double[] unrot0 = Matrices.invert( rot0 );
            r0 = Matrices.mvMult( unrot0, r0 );
            r1 = Matrices.mvMult( unrot0, r1 );
            double[] x01 = Matrices.cross( r1, r0 );
            double modx = Matrices.mod( x01 );
            double[] axvec = Matrices.mult( x01, Math.asin( modx ) / modx );
            double[] rm = rotateAround( axvec );
            return Matrices.mmMult( rot0, rm );
        }
        else {
            return null;
        }
    }

    public boolean useRanges( boolean reflect, double[] r3, double radiusRad ) {
        return false;
    }

    public SkyAspect createAspect( boolean reflect, double[] r3,
                                   double radiusRad, Range[] ranges ) {
        return new SkyAspect( this, reflect );
    }

    /**
     * Returns a rotation matrix which rotates around a given axis vector.
     *
     * @param  axvec  axis vector
     * @return  rotation matrix
     */
    private static double[] rotateAround( double[] axvec ) {
        return Matrices.fromPal( new Pal().Dav2m( axvec ) );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof HemisphereProjection ) {
            HemisphereProjection other = (HemisphereProjection) o;
            return this.upNorth_ == other.upNorth_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.upNorth_ ? 1 : 0;
    }
}
