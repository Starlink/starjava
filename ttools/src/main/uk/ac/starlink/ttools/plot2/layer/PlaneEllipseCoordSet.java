package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * MultiPointCoordSet for ellipses on a plane.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class PlaneEllipseCoordSet implements MultiPointCoordSet {

    private static final int NP = 4;
    private static final FloatingCoord AR_COORD =
        FloatingCoord.createCoord( "Major radius",
                                   "One of the ellipse principal radii",
                                   true );
    private static final FloatingCoord BR_COORD =
        FloatingCoord.createCoord( "Minor radius",
                                   "The other ellipse principal radius",
                                   false );
    private static final FloatingCoord POSANG_COORD =
        FloatingCoord.createCoord( "Orientation",
                                   "Angle from X axis towards Y axis "
                                 + "of semi-major radius",
                                   false );
    static {
        POSANG_COORD.getUserInfo().setUnitString( "degrees" );
    }

    /**
     * Constructor.
     */
    public PlaneEllipseCoordSet() {
    }

    public Coord[] getCoords() {
        return new Coord[] { AR_COORD, BR_COORD, POSANG_COORD };
    }

    public int getPointCount() {
        return NP;
    }

    public boolean readPoints( TupleSequence tseq, int icol, double[] xy0,
                               double[][] xyExtras ) {
        double ar = AR_COORD.readDoubleCoord( tseq, icol );
        double br = BR_COORD.readDoubleCoord( tseq, icol + 1 );
        double posang = POSANG_COORD.readDoubleCoord( tseq, icol + 2 );
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
        double dx0 = xy0[ 0 ];
        double dy0 = xy0[ 1 ];
        final double ax;
        final double ay;
        final double bx;
        final double by;
        if ( posang == 0 ) {
            ax = ar;
            ay = 0;
            bx = 0;
            by = br;
        }
        else {
            double prad = Math.toRadians( posang );
            double sp = Math.sin( prad );
            double cp = Math.cos( prad );
            ax = +cp * ar;
            ay = +sp * ar;
            bx = -sp * br;
            by = +cp * br;
        }
        if ( ax == 0 && ay == 0 && bx == 0 && by == 0 ) {
            return false;
        }
        else {
            double[] xy1 = xyExtras[ 0 ];
            double[] xy2 = xyExtras[ 1 ];
            double[] xy3 = xyExtras[ 2 ];
            double[] xy4 = xyExtras[ 3 ];
            xy1[ 0 ] = dx0 - ax;
            xy1[ 1 ] = dy0 - ay;
            xy2[ 0 ] = dx0 + ax;
            xy2[ 1 ] = dy0 + ay;
            xy3[ 0 ] = dx0 - bx;
            xy3[ 1 ] = dy0 - by;
            xy4[ 0 ] = dx0 + bx;
            xy4[ 1 ] = dy0 + by;
            return true;
        }
    }
}
