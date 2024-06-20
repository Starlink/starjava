package uk.ac.starlink.ttools.cone;

import cds.moc.Healpix;
import cds.moc.HealpixImpl;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Random;
import java.util.Set;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.ttools.cone.PixtoolsHealpix;
import uk.ac.starlink.ttools.func.CoordsDegrees;

public class HealpixImplTest extends TestCase {

    public void testPixtools() {
        workHealpix( PixtoolsHealpix.getInstance(), true );
        workHealpix( PixtoolsHealpix.getInstance(), false );
    }

    public void testFx() {
        workHealpix( CdsHealpix.getInstance(), true );
        workHealpix( CdsHealpix.getInstance(), false );
    }

    public void testGaia() {
        workHealpix( new Healpix(), true );
        workHealpix( new Healpix(), false );
    }

    public void testCompatible() throws Exception {
        checkCompatible( new Healpix(), PixtoolsHealpix.getInstance() );
        checkCompatible( new Healpix(), CdsHealpix.getInstance() );
    }

    private void checkCompatible( HealpixImpl hpi0, HealpixImpl hpi1 )
            throws Exception {
        Random rnd = new Random( 232529L );
        int nExtra1 = 0;
        int nExtra0 = 0;
        int nCommon = 0;
        for ( int i = 0; i < 30; i++ ) {
            Disc disc = new Disc( rnd );
            Set<Long> pixSet1 = toSet( disc.query( hpi1 ) );
            Set<Long> pixSet0 = toSet( disc.query( hpi0 ) );
            Set<Long> commonPixSet = new TreeSet<Long>();
            commonPixSet.addAll( pixSet1 );
            commonPixSet.retainAll( pixSet0 );
            Set<Long> extra1 = new TreeSet<Long>( pixSet1 );
            extra1.removeAll( commonPixSet );
            Set<Long> extra0 = new TreeSet<Long>( pixSet0 );
            extra0.removeAll( commonPixSet );
            nExtra1 += extra1.size();
            nExtra0 += extra0.size();
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
        if ( nExtra1 > 0 || nExtra0 > 0 ) {
            System.out.println( "query_disc false positives: ("
                              + hpi0.getClass().getName() + " vs. "
                              + hpi1.getClass().getName() + ") "
                              + "nCommon: " + nCommon + ", "
                              + "nExtra0: " + nExtra0 + ", "
                              + "nExtra1: " + nExtra1 );
        }
    }

    public void testOverlaps() throws Exception {
        checkOverlaps( new Healpix() );
        checkOverlaps( PixtoolsHealpix.getInstance() );
        checkOverlaps( CdsHealpix.getInstance() );
    }

    private void checkOverlaps( HealpixImpl hpi ) throws Exception {
        Random rnd = new Random( 2301L );
        double scale = 0.1; // degree
        for ( int i = 0; i < 1000; i++ ) {
            double ra1 = rnd.nextDouble() * 360;
            double dec1 = rnd.nextDouble() * 180 - 90;
            double ra2 = Math.min( ra1 + rnd.nextDouble() * scale, 360 );
            double dec2 = Math.max( Math.min( dec1 + rnd.nextDouble() * scale,
                                              90 ), -90 );
            for ( int order = 5; order < 12; order++ ) {
                checkOverlap( hpi, order, ra1, dec1, ra2, dec2 );
            }
        }

        /* This one is known to cause trouble with some versions of PixTools.
         * Thanks to Heinz Andernach for coming up with it. */
        checkOverlap( hpi, 8, 0, -89.983888, 180, -89.983888 );
    }

    private void checkOverlap( HealpixImpl hpi, int order,
                               double lon1, double lat1,
                               double lon2, double lat2 )
            throws Exception {

        /* Two discs with radius slightly greater than half the distance
         * between them must overlap, so must have at least one pixel in
         * common (this is exactly the fact that the crossmatching 
         * relies on). */
        double distance =
            CoordsDegrees.skyDistanceDegrees( lon1, lat1, lon2, lat2 );
        double radius = 0.5001 * distance;
        Set<Long> disc1 = toSet( hpi.queryDisc( order, lon1, lat1, radius ) );
        Set<Long> disc2 = toSet( hpi.queryDisc( order, lon2, lat2, radius ) );
        Set<Long> intersect = new TreeSet<Long>( disc1 );
        intersect.retainAll( disc2 );
        assertTrue( ! intersect.isEmpty() );
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
            set.add( Long.valueOf( pixels[ i ] ) );
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
