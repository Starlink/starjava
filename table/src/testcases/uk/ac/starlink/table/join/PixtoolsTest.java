package uk.ac.starlink.table.join;

import gov.fnal.eag.healpix.PixTools;
import java.util.List;
import java.util.Random;
import javax.vecmath.Vector3d;
import junit.framework.TestCase;

public class PixtoolsTest extends TestCase {

    public void testPixtools() throws InterruptedException {
        workPixtools( false );
        workPixtools( true );
    }

    public void testIndices() {
        checkIndex( true, 2, 23 );
        checkIndex( false, 2, 23 );
        checkIndex( true, 9, 1110787 );
        checkIndex( false, 9, 1110787 );
    }

    private void checkIndex( boolean isNest, int level, long ipix ) {
        PixTools pt = new PixTools();
        long nside = 1L << level;
        double[] latLon = isNest ? pt.pix2ang_nest( nside, ipix )
                                 : pt.pix2ang_ring( nside, ipix );
        Vector3d vec = isNest ? pt.pix2vect_nest( nside, ipix )
                              : pt.pix2vect_ring( nside, ipix );
        long ipix2 = isNest ? pt.vect2pix_nest( nside, vec )
                            : pt.vect2pix_ring( nside, vec );
        assertEquals( ipix, ipix2 );
    }

    private void workPixtools( boolean useThreads )
            throws InterruptedException {
        int nw = 40;
        Worker[] workers = new Worker[ nw ];
        Random rnd = new Random( 23001L );
        int nside = 6 + rnd.nextInt( 3 );
        Vector3d vector = new Vector3d( rnd.nextDouble(),
                                        rnd.nextDouble(),
                                        rnd.nextDouble() );
        double radius = 0.1 * rnd.nextDouble();
        int nest = 1;
        int inclusive = 1;
        for ( int iw = 0; iw < nw; iw++ ) {
            workers[ iw ] =
                new Worker( nside, vector, radius, nest, inclusive );
        }
        if ( useThreads ) {
            for ( int iw = 0; iw < nw; iw++ ) {
                workers[ iw ].start();
            }
            for ( int iw = 0; iw < nw; iw++ ) {
                workers[ iw ].join();
            }
        }
        else {
            for ( int iw = 0; iw < nw; iw++ ) {
                workers[ iw ].run();
            }
        }
        List result0 = workers[ 0 ].result_;
        for ( int iw = 1; iw < nw; iw++ ) {
            assertEquals( "Fail at " + iw + " for useThreads=" + useThreads,
                          result0, workers[ iw ].result_ );
        }
    }

    private static class Worker extends Thread {
        private final long nside_;
        private final Vector3d vector_;
        private final double radius_;
        private final int nest_;
        private final int inclusive_;
        private List result_;

        Worker( long nside, Vector3d vector, double radius,
                int nest, int inclusive ) {
            nside_ = nside;
            vector_ = vector;
            radius_ = radius;
            nest_ = nest;
            inclusive_ = inclusive;
        }

        public void run() {
            result_ = new PixTools()
                     .query_disc( nside_, vector_, radius_, nest_, inclusive_ );
        }
    }
}
