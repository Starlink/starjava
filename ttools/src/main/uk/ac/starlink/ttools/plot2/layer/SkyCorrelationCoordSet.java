package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;

/**
 * MultiPointCoordSet for ellipses on the sky defined by lon/lat errors
 * and a correlation.
 * This is how Gaia errors are quoted.
 *
 * @author   Mark Taylor
 * @since    5 Apr 2017
 */
public class SkyCorrelationCoordSet implements MultiPointCoordSet {

    private final boolean preMultCosLat_;
    private final FloatingCoord aerrCoord_;
    private final FloatingCoord derrCoord_;
    private final FloatingCoord corrCoord_;
    private static final int NP = 4;

    /**
     * Constructor.
     *
     * @param  preMultCosLat  true iff the user-supplied longitude values have
     *                        been premultiplied by cos(latitude)
     */
    public SkyCorrelationCoordSet( boolean preMultCosLat ) {
        preMultCosLat_ = preMultCosLat;
        aerrCoord_ = FloatingCoord.createCoord(
            new InputMeta( "lonerr", "Longitude error" )
           .setShortDescription( "Error in longitude in degrees"
                               + ( preMultCosLat ? "" : "NOT " )
                               + "premultiplied by cos(lat)" )
           .setXmlDescription( new String[] {
                "<p>Error in the longitude coordinate.",
                "The supplied value is an angle in degrees, and",
                "<strong>" + ( preMultCosLat ? "is" : "is not" ) + "</strong>",
                "considered to be premultiplied by cos(Latitude).",
                "</p>",
            } )
           .setValueUsage( "deg" )
        , true );
        derrCoord_ = FloatingCoord.createCoord(
            new InputMeta( "laterr", "Latitude error" )
           .setShortDescription( "Error in latitude in degrees" )
           .setXmlDescription( new String[] {
                "<p>Error in the latitude coordinate.",
                "The supplied value is an angle in degrees.",
                "</p>",
            } )
           .setValueUsage( "deg" )
        , true );
        corrCoord_ = FloatingCoord.createCoord(
            new InputMeta( "corr", "Lon-Lat Correlation" )
           .setShortDescription( "Correlation between longitude and latitude" )
           .setXmlDescription( new String[] {
                "<p>Correlation between the errors in longitude and latitude.",
                "This is a dimensionless quantity in the range -1..+1,",
                "and is equivalent to the covariance divided by",
                "the product of the Longitude and Latitude error values",
                "themselves.",
                "It corresponds to the <code>ra_dec_corr</code> value",
                "supplied in the Gaia source catalogue.",
                "</p>",
            } )
        , true );
    }

    public Coord[] getCoords() {
        return new Coord[] { aerrCoord_, derrCoord_, corrCoord_ };
    }

    public int getPointCount() {
        return NP;
    }

    public boolean readPoints( TupleSequence tseq, int icol, DataGeom geom,
                               double[] xyz0, double[][] xyzExtras ) {

        /* Read error and correlation values from data. */
        double aerrRaw =
            Math.toRadians( aerrCoord_.readDoubleCoord( tseq, icol + 0 ) );
        double derr =
            Math.toRadians( derrCoord_.readDoubleCoord( tseq, icol + 1 ) );
        if ( Double.isNaN( aerrRaw ) || Double.isNaN( derr ) ||
             ( aerrRaw == 0 && derr == 0 ) ) {
            return false;
        }
        double corr = corrCoord_.readDoubleCoord( tseq, icol + 2 );
        if ( Double.isNaN( corr ) ) {
            return false;
        }
        final double aerrPre;
        if ( preMultCosLat_ ) {
            aerrPre = aerrRaw;
        }
        else {
            double z = xyz0[ 2 ];
            double cosDelta = Math.sqrt( 1 - z * z );
            aerrPre = cosDelta * aerrRaw;
        }

        /* Calculate error vectors. */
        double[] ra = new double[ 2 ];
        double[] rb = new double[ 2 ];
        PlaneCorrelationCoordSet
            .calculateErrorVectors( aerrPre, derr, corr, ra, rb );
        double rax = ra[ 0 ];
        double ray = ra[ 1 ];
        double rbx = rb[ 0 ];
        double rby = rb[ 1 ];
      
        /* Turn the error ellipse axes into four plotted points. */
        TangentPlaneTransformer trans =
            new TangentPlaneTransformer( xyz0, (SkyDataGeom) geom );
        trans.displace( -rax, -ray, xyzExtras[ 0 ] );
        trans.displace( +rax, +ray, xyzExtras[ 1 ] );
        trans.displace( -rbx, -rby, xyzExtras[ 2 ] );
        trans.displace( +rbx, +rby, xyzExtras[ 3 ] );
        return true;
    }
}
