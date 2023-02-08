package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;

/**
 * MultiPointCoordSet for vectors on the sky.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class SkyVectorCoordSet implements SkyMultiPointCoordSet {

    private final FloatingCoord dlonCoord_;
    private final FloatingCoord dlatCoord_;
    private final boolean preMultCosLat_;
    private static final String DELTA = "Delta";

    /**
     * Constructor.
     *
     * @param  preMultCosLat  true iff the user-supplied longitude values have
     *                        been premultiplied by cos(latitude)
     */
    public SkyVectorCoordSet( boolean preMultCosLat ) {
        preMultCosLat_ = preMultCosLat;
        dlonCoord_ = FloatingCoord.createCoord(
            new InputMeta( "dlon",
                           ( preMultCosLat ? DELTA + " Lon(*)"
                                           : DELTA + " Lon" ) )
           .setShortDescription( "Change in longitude coordinate "
                               + ( preMultCosLat ? "" : "NOT " )
                               + "premultiplied by cos(lat)" )
           .setXmlDescription( new String[] {
                "<p>Change in the longitude coordinate represented by",
                "the plotted vector.",
                "The supplied value",
                "<strong>" + ( preMultCosLat ? "is" : "is not" ) + "</strong>",
                "considered to be premultiplied by cos(Latitude).",
                SkyMultiPointForm.getCoordUnitText(),
                "</p>",
            } )
        , true );
        dlatCoord_ = FloatingCoord.createCoord(
            new InputMeta( "dlat", DELTA + " Lat" )
           .setShortDescription( "Change in latitude coordinate" )
           .setXmlDescription( new String[] {
                "<p>Change in the latitude coordinate represented by",
                "the plotted vector.",
                SkyMultiPointForm.getCoordUnitText(),
                "</p>",
            } )
        , true );
    }

    public Coord[] getCoords() {
        return new Coord[] { dlonCoord_, dlatCoord_ };
    }

    public int getPointCount() {
        return 1;
    }

    public double readSize( Tuple tuple, int icol, double[] xyz0 ) {
        double dLon = dlonCoord_.readDoubleCoord( tuple, icol + 0 )
                    * lonMultiplier( xyz0 );
        double dLat = dlatCoord_.readDoubleCoord( tuple, icol + 1 );
        return Math.hypot( dLon, dLat );
    }

    public boolean readPoints( Tuple tuple, int icol, double[] xyz0,
                               double unitInDegrees, SkyDataGeom geom,
                               double[][] xyzExtras ) {
        double dLon =
            Math.toRadians( dlonCoord_.readDoubleCoord( tuple, icol + 0 )
                          * unitInDegrees );
        double dLat =
            Math.toRadians( dlatCoord_.readDoubleCoord( tuple, icol + 1 )
                          * unitInDegrees );
        if ( Double.isNaN( dLon ) || Double.isNaN( dLat ) ) {
            return false;
        }
        if ( dLon == 0 && dLat == 0 ) {
            return false;
        }
        else {
            double xi = dLon * lonMultiplier( xyz0 );
            double eta = dLat;
            double[] xyz1 = xyzExtras[ 0 ];
            new TangentPlaneTransformer( xyz0, geom )
               .displace( xi, eta, xyz1 );
            return true;
        }
    }

    /**
     * Creates a MultiPointform that can plot vectors on the sky,
     * corresponding to this coordset.
     *
     * @return  new form
     */
    public static MultiPointForm createForm() {
        SkyVectorCoordSet coordSet = new SkyVectorCoordSet( true );
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots directed lines from the data position",
            "given delta values for the coordinates",
            "The plotted markers are typically little arrows,",
            "but there are other options.",
            "</p>",
            SkyMultiPointForm
           .getScalingDescription( new FloatingCoord[] { coordSet.dlonCoord_,
                                                         coordSet.dlatCoord_ },
                                   "vector" ),
        } );
        return new SkyMultiPointForm( "SkyVector", ResourceIcon.FORM_VECTOR,
                                      descrip, coordSet,
                                      StyleKeys.VECTOR_SHAPE );
    }

    /**
     * Returns the multiplier to apply to input longitude coordinate values.
     * This takes account of cos(lat) premultiplication as appropriate.
     *
     * @param  central position in data coordinates
     * @return   multiplier to apply to longitude coordinates
     */
    private double lonMultiplier( double[] xyz0 ) {
        if ( preMultCosLat_ ) {
            return 1;
        }
        else {
            double z = xyz0[ 2 ];
            double cosLat = Math.sqrt( 1 - z * z );
            return cosLat;
        }
    }
}
