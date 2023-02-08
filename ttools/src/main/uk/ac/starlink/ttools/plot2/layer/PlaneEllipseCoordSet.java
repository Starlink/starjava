package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * MultiPointCoordSet for ellipses on a plane.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class PlaneEllipseCoordSet implements CartesianMultiPointCoordSet {

    private static final int NP = 4;
    private static final FloatingCoord AR_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "ra", "Primary Radius" )
           .setShortDescription( "Ellipse first principal radius" )
           .setXmlDescription( new String[] {
                "<p>Ellipse first principal radius.",
                "</p>",
            } )
        , true );
    private static final FloatingCoord BR_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "rb", "Secondary Radius" )
           .setShortDescription( "Ellipse second principal radius" )
           .setXmlDescription( new String[] {
                "<p>Ellipse second principal radius.",
                "If this value is blank, the two radii will be assumed equal,",
                "i.e. the ellipses will be circles.",
                "</p>",
            } )
        , false );
    private static final FloatingCoord POSANG_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "posang", "Position Angle" )
           .setShortDescription( "Anticlockwise angle from X axis"
                               + "of primary axis" )
           .setXmlDescription( new String[] {
                "<p>Orientation of the ellipse.",
                "The value is the angle in degrees",
                "from the X axis towards the Y axis",
                "of the first principal axis of the ellipse.",
                "</p>",
            } )
           .setValueUsage( "deg" )
        , false );

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

    public boolean readPoints( Tuple tuple, int icol, double[] xy0,
                               double[][] xyExtras ) {
        double ar = AR_COORD.readDoubleCoord( tuple, icol );
        double br = BR_COORD.readDoubleCoord( tuple, icol + 1 );
        double posang = POSANG_COORD.readDoubleCoord( tuple, icol + 2 );
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

    /**
     * Creates a MultiPointForm that can plot ellipses on the plane,
     * corresponding to this coordset.
     *
     * @return  new form
     */
    public static MultiPointForm createForm() {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots an ellipse (or rectangle, triangle,",
            "or other similar figure)",
            "defined by two principal radii and",
            "an optional angle of rotation,",
            "the so-called position angle.",
            "This angle, if specified, is in degrees and",
            "gives the angle counterclockwise from the horizontal axis",
            "to the first principal radius.",
            "</p>",
        } );
        boolean canScale = true;
        if ( canScale ) {
            descrip += MultiPointForm.getDefaultScalingDescription( "ellipse" );
        }
        return new CartesianMultiPointForm( "XYEllipse",
                                            ResourceIcon.FORM_XYELLIPSE,
                                            descrip, new PlaneEllipseCoordSet(),
                                            StyleKeys.ELLIPSE_SHAPE, canScale );
    }
}
