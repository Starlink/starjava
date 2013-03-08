package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * MultiPointCoordSet for ellipses on a sphere.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class SkyEllipseCoordSet implements MultiPointCoordSet {

    private static final int NP = 4;
    private static final FloatingCoord AR_COORD =
        FloatingCoord.createCoord( "Major radius",
                                   "One of the ellipse principal radii", true );
    private static final FloatingCoord BR_COORD =
        FloatingCoord.createCoord( "Minor radius",
                                   "The other ellipse principal radius",
                                   false );
    private static final FloatingCoord POSANG_COORD =
        FloatingCoord.createCoord( "Position Angle",
                                   "Angle from north pole to primary axis, "
                                 + "in direction of positive RA", false );
    static {
        AR_COORD.getUserInfo().setUnitString( "degrees" );
        BR_COORD.getUserInfo().setUnitString( "degrees" );
        POSANG_COORD.getUserInfo().setUnitString( "degrees" );
    }

    /**
     * Constructor.
     */
    public SkyEllipseCoordSet() {
    }

    public Coord[] getCoords() {
        return new Coord[] { AR_COORD, BR_COORD, POSANG_COORD };
    }

    public int getPointCount() {
        return NP;
    }

    public boolean readPoints( TupleSequence tseq, int icol, double[] xyz0,
                               double[][] xyzExtras ) {
        double ar =
            Math.toRadians( AR_COORD.readDoubleCoord( tseq, icol ) );
        double br =
            Math.toRadians( BR_COORD.readDoubleCoord( tseq, icol + 1 ) );
        double posang =
            Math.toRadians( POSANG_COORD.readDoubleCoord( tseq, icol + 2 ) );
        boolean aNan = Double.isNaN( ar );
        boolean bNan = Double.isNaN( br );
        if ( aNan && bNan ) {
            return false;
        }
        else if ( aNan ) {
            ar = br;
            posang = 0;
        }
        else if ( bNan ) {
            br = ar;
            posang = 0;
        }
        else if ( Double.isNaN( posang ) ) {
            posang = 0;
        }
        final double ax;
        final double ay;
        final double bx;
        final double by;
        if ( posang == 0 ) {
            ax = 0;
            ay = ar;
            bx = br;
            by = 0;
        }
        else {
            double sp = Math.sin( posang );
            double cp = Math.cos( posang );
            ax = +sp * ar;
            ay = +cp * ar;
            bx = +cp * br;
            by = -sp * br;
        }
        if ( ax == 0 && ay == 0 && bx == 0 && by == 0 ) {
            return false;
        }
        else {
            TangentPlaneTransformer trans = new TangentPlaneTransformer( xyz0 );
            trans.displace( -ax, -ay, xyzExtras[ 0 ] );
            trans.displace( +ax, +ay, xyzExtras[ 1 ] );
            trans.displace( -bx, -by, xyzExtras[ 2 ] );
            trans.displace( +bx, +by, xyzExtras[ 3 ] );
            return true;
        }
    }
}
