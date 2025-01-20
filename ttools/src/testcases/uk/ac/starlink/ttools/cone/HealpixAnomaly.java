package uk.ac.starlink.ttools.cone;

import cds.moc.Healpix;
import cds.moc.HealpixImpl;
import gov.fnal.eag.healpix.PixTools;
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
        final int order;
        final double lon;
        final double lat;
        final double radius;
        if ( args.length == 0 ) {
            order = 8;
            lon = 114.112;
            lat = 80.700;
            radius = 0.17866;
        }
        else if ( args.length == 4 ) {
            order = Integer.parseInt( args[ 0 ] );
            lon = Double.parseDouble( args[ 1 ] );
            lat = Double.parseDouble( args[ 2 ] );
            radius = Double.parseDouble( args[ 3 ] );
        }
        else {
            System.err.println( "usage: " + HealpixAnomaly.class.getName()
                              + " <order> <lon> <lat> <radius>" );
            System.exit( 1 );
            return;
        }
        printResults( order, lon, lat, radius );
    }

    public static void printResults( int order, double lon, double lat,
                                     double radius ) throws Exception {
        HealpixImpl gHpi = new Healpix();
        HealpixImpl pHpi = PixtoolsHealpix.getInstance();
        HealpixImpl cHpi = CdsHealpix.getInstance();
        System.out.println( gHpi.getClass().getName() );
        printPixels( gHpi.queryDisc( order, lon, lat, radius ) );
        System.out.println( cHpi.getClass().getName() );
        printPixels( cHpi.queryDisc( order, lon, lat, radius ) );
        System.out.println( pHpi.getClass().getName() );
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
