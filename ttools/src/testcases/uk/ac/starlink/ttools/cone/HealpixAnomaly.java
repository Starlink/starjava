package uk.ac.starlink.ttools;

import cds.moc.Healpix;
import cds.moc.HealpixImpl;
import gov.fnal.eag.healpix.PixTools;
import healpix.core.HealpixBase;
import java.util.Arrays;
import uk.ac.starlink.ttools.cone.PixtoolsHealpix;

/**
 * Small standalone program which shows a discrepancy between
 * PixTools and the other implementation of HEALPix.
 * Hopefully harmless (different false positives in different query_disc
 * implementations).
 */
public class HealpixAnomaly {

    public static void main( String[] args ) throws Exception {
        int order = 8;
        double lon = 114.112;
        double lat = 80.700;
        double radius = 0.17866;
        HealpixImpl gHpi = new Healpix();
        HealpixImpl pHpi = PixtoolsHealpix.getInstance();
        printPixels( gHpi.queryDisc( order, lon, lat, radius ) );
        printPixels( pHpi.queryDisc( order, lon, lat, radius ) );
    }

    private static void printPixels( long[] pixels ) {
        pixels = (long[]) pixels.clone();
        Arrays.sort( pixels );
        StringBuffer sbuf = new StringBuffer();
        for ( int ip = 0; ip < pixels.length; ip++ ) {
            if ( ip > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( pixels[ ip ] );
        }
        System.out.println( sbuf );
    }
}
