package uk.ac.starlink.ttools.cone;

import cds.healpix.FlatHashIterator;
import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;
import cds.moc.HealpixImpl;
import uk.ac.starlink.table.Tables;

/**
 * Healpix implementation for use with the MOC library based on
 * F-X Pineau's cds-healpix-java library.
 *
 * <p>Use {@link #getInstance} to obtain the singleton instance of this class.
 *
 * @author   Mark Taylor
 * @since    21 May 2020
 * @see    <a href="https://github.com/cds-astro/cds-healpix-java"
 *                 >https://github.com/cds-astro/cds-healpix-java</a>
 */
public class CdsHealpix implements HealpixImpl {

    private static final double DEG_TO_RAD = Math.PI / 180.;
    private static final double RAD_TO_DEG = 180. / Math.PI;
    private static final CdsHealpix instance_ = new CdsHealpix();

    /**
     * Private constructor prevents instantiation.
     */
    private CdsHealpix() {
    }

    public long ang2pix( int order, double lonDeg, double latDeg ) {
        return Healpix.getNestedFast( order )
                      .hash( lonDeg * DEG_TO_RAD, latDeg * DEG_TO_RAD );
    }

    public double[] pix2ang( int order, long ipix ) {
        double[] pos = new double[ 2 ];
        Healpix.getNestedFast( order ).center( ipix, pos );
        pos[ 0 ] *= RAD_TO_DEG;
        pos[ 1 ] *= RAD_TO_DEG;
        return pos;
    }

    public long[] queryDisc( int order, double lonDeg, double latDeg,
                             double radiusDeg ) {
        HealpixNestedBMOC bmoc =
            Healpix
           .getNested( order )
           .newConeComputerApprox( radiusDeg * DEG_TO_RAD )
           .overlappingCells( lonDeg * DEG_TO_RAD, latDeg * DEG_TO_RAD );
        int ntile = Tables.checkedLongToInt( bmoc.computeDeepSize() );
        long[] itiles = new long[ ntile ];
        FlatHashIterator fhit = bmoc.flatHashIterator();
        for ( int itile = 0; itile < ntile; itile++ ) {
            assert fhit.hasNext();
            itiles[ itile ] = fhit.next();
        }
        assert ! fhit.hasNext();
        return itiles;
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   singleton instance
     */
    public static CdsHealpix getInstance() {
        return instance_;
    }
}
