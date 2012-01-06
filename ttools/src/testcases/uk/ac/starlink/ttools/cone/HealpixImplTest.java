package uk.ac.starlink.ttools.cone;

import cds.moc.Healpix;
import cds.moc.HealpixImpl;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Random;
import java.util.Set;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.ttools.cone.PixtoolsHealpix;

public class HealpixImplTest extends TestCase {

    public void testPixtools() {
        workHealpix( PixtoolsHealpix.getInstance(), true );
        workHealpix( PixtoolsHealpix.getInstance(), false );
    }

    public void testGaia() {
//      workHealpix( new Healpix(), true );  // known broken
        workHealpix( new Healpix(), false );
    }

    public void testCompatible() throws Exception {
        Random rnd = new Random( 232529L );
        HealpixImpl pHpi = PixtoolsHealpix.getInstance();
        HealpixImpl gHpi = new Healpix();
        int npExtra = 0;
        int ngExtra = 0;
        int nCommon = 0;
        for ( int i = 0; i < 30; i++ ) {
            Disc disc = new Disc( rnd );
            Set<Long> pPixSet = toSet( disc.query( pHpi ) );
            Set<Long> gPixSet = toSet( disc.query( gHpi ) );
            Set<Long> commonPixSet = new TreeSet<Long>();
            commonPixSet.addAll( pPixSet );
            commonPixSet.retainAll( gPixSet );
            Set<Long> pExtra = new TreeSet<Long>( pPixSet );
            pExtra.removeAll( commonPixSet );
            Set<Long> gExtra = new TreeSet<Long>( gPixSet );
            gExtra.removeAll( commonPixSet );
            npExtra += pExtra.size();
            ngExtra += gExtra.size();
            nCommon += commonPixSet.size();
        }

        /* These figures represent the number of pixels found using one
         * HEALPix query_disc implementation compared to the other.
         * You'd like the extra (fp) values to be zero, but I'm told by
         * Martin Reinecke that false positives are not unexpected.
         * They are not harmful, as long as that's what they are;
         * it's not possible using this test (or other tests that I 
         * know of) to test for false negatives, which would be harmful.
         * So for now, just log them and cross fingers. */
        /* See also class HealpixAnomaly. */
        if ( npExtra > 0 || ngExtra > 0 ) {
            System.out.println( "query_disc false positives: "
                              + "nCommon: " + nCommon + ", "
                              + "npExtra: " + npExtra + ", "
                              + "ngExtra: " + ngExtra );
        }
    }

    /**
     * Test a Healpix implementation, especially for thread safety.
     */
    private void workHealpix( HealpixImpl hpi, boolean useThreads ) {
        int nw = 16;
        Worker[] workers = new Worker[ nw ];
        Disc disc = new Disc( new Random( 230001L ) );
        for ( int iw = 0; iw < nw; iw++ ) {
            workers[ iw ] = new Worker( hpi, disc );
        }
        if ( useThreads ) {
            for ( int iw = 0; iw < nw; iw++ ) {
                workers[ iw ].start();
            }
            try {
                for ( int iw = 0; iw < nw; iw++ ) {
                    workers[ iw ].join();
                }
            }
            catch ( InterruptedException e ) {
                throw (AssertionError) new AssertionError().initCause( e );
            }
        }
        else {
            for ( int iw = 0; iw < nw; iw++ ) {
                workers[ iw ].run();
            }
        }
        int npix = workers[ 0 ].pixels_.length;
        assertTrue( npix > 0 && npix < 10000 );
        for ( int iw = 1; iw < nw; iw++ ) {
            assertArrayEquals( workers[ 0 ].pixels_, workers[ iw ].pixels_ );
        }
    }

    private static Set<Long> toSet( long[] pixels ) {
        Set<Long> set = new TreeSet<Long>();
        for ( int i = 0; i < pixels.length; i++ ) {
            set.add( new Long( pixels[ i ] ) );
        }
        return set;
    }

    private static class Disc {
        final int order_;
        final double lon_;
        final double lat_;
        final double radius_;
        Disc( int order, double lon, double lat, double radius ) {
            order_ = order;
            lon_ = lon;
            lat_ = lat;
            radius_ = radius;
        }
        Disc( Random rnd ) {
            this( 6 + rnd.nextInt( 3 ),
                  rnd.nextDouble() * 360, rnd.nextDouble() * 180 - 90,
                  0.5 * rnd.nextDouble() * 1.0 );
        }
        long[] query( HealpixImpl hpi ) throws Exception {
            return hpi.queryDisc( order_, lon_, lat_, radius_ );
        }
        public String toString() {
            return order_ + ":(" + (float) lon_ + "," + (float) lat_ + ")+"
                 + (float)radius_;
        }
    }

    private static class Worker extends Thread {
        final HealpixImpl hpi_;
        final Disc disc_;
        long[] pixels_;

        Worker( HealpixImpl hpi, Disc disc ) {
            hpi_ = hpi;
            disc_ = disc;
        }

        public void run() {
            try {
                pixels_ = disc_.query( hpi_ );
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }
}
