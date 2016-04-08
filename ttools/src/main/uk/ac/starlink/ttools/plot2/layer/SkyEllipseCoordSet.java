package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
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
        FloatingCoord.createCoord(
            new InputMeta( "ra", "Primary Radius" )  // is "ra" a bad name?
           .setShortDescription( "Ellipse first principal radius in degrees" )
           .setValueUsage( "deg" )
           .setXmlDescription( new String[] {
                "<p>Ellipse first principal radius in degrees.",
                "</p>",
            } )
        , true );
    private static final FloatingCoord BR_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "rb", "Secondary Radius" )
           .setShortDescription( "Ellipse second principal radius in degrees" )
           .setValueUsage( "deg" )
           .setXmlDescription( new String[] {
                "<p>Ellipse second principal radius in degrees.",
                "If this value is blank, the two radii will be assumed equal,",
                "i.e. the ellipses will be circles.",
                "</p>",
            } )
        , false );
    private static final FloatingCoord POSANG_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "posang", "Position Angle" )
           .setShortDescription( "Clockwise angle from north pole "
                               + "to primary axis" )
           .setXmlDescription( new String[] {
                "<p>Orientation of the ellipse.",
                "The value is the angle in degrees from the North pole",
                "to the primary axis of the ellipse",
                "in the direction of increasing longitude.",
                "</p>",
            } )
           .setValueUsage( "deg" )
        , false );

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
