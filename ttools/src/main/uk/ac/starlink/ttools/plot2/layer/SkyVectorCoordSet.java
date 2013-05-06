package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * MultiPointCoordSet for vectors on the sky.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class SkyVectorCoordSet implements MultiPointCoordSet {

    private final FloatingCoord dlonCoord_;
    private final FloatingCoord dlatCoord_;
    private final boolean preMultCosLat_;

    /**
     * Constructor.
     *
     * @param  preMultCosLat  true iff the user-supplied longitude values have
     *                        been premultiplied by cos(latitude)
     */
    public SkyVectorCoordSet( boolean preMultCosLat ) {
        preMultCosLat_ = preMultCosLat;
        dlonCoord_ = FloatingCoord
                    .createCoord( "Proper Motion Longitude",
                                  "Change in longitude coordinate "
                                + ( preMultCosLat ? "" : "*not* " )
                                + "multiplied by cos(Latitude))",
                                  true );
        dlatCoord_ = FloatingCoord
                    .createCoord( "Proper Motion Latitude",
                                  "Change in latitude coordinate",
                                  true );
        dlonCoord_.getUserInfo().setUnitString( "degrees" );
        dlatCoord_.getUserInfo().setUnitString( "degrees" );
    }

    public Coord[] getCoords() {
        return new Coord[] { dlonCoord_, dlatCoord_ };
    }

    public int getPointCount() {
        return 1;
    }

    public boolean readPoints( TupleSequence tseq, int icol, double[] xyz0,
                               double[][] xyzExtras ) {
        double dLon =
            Math.toRadians( dlonCoord_.readDoubleCoord( tseq, icol ) );
        double dLat =
            Math.toRadians( dlatCoord_.readDoubleCoord( tseq, icol + 1 ) );
        if ( Double.isNaN( dLon ) || Double.isNaN( dLat ) ) {
            return false;
        }
        if ( dLon == 0 && dLat == 0 ) {
            return false;
        }
        else {
            double theta = Math.asin( xyz0[ 2 ] );
            double xi = dLat;
            double eta = preMultCosLat_ ? dLon
                                        : dLon * Math.cos( theta );
            double[] xyz1 = xyzExtras[ 0 ];
            new TangentPlaneTransformer( xyz0 ).displace( xi, eta, xyz1 );
            return true;
        }
    }
}
