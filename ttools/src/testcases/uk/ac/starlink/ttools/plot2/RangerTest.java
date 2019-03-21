package uk.ac.starlink.ttools.plot2;

import junit.framework.TestCase;

public class RangerTest extends TestCase {

    /**
     * Tests the Distributor implementation in HistogramRanger.
     * The intention is to place constraints on how uniformly the
     * input data stream is sampled for histogram construction.
     * This could probably be improved by providing a different
     * Distributor implementation, but this test will warn if it
     * appears to be behaving worse.
     */
    public void testDistributor() {
        int nStore = 10000;

        /* Test for streams of different lengths. */
        for ( int npass = 1; npass < 50; npass++ ) {

            /* Perform distribution. */
            double[] array = new double[ nStore ];
            HistoRanger.Distributor distrib =
                HistoRanger.createDistributor( array );
            for ( int ip = 0; ip < npass; ip++ ) {
                for ( int is = 0; is < nStore; is++ ) {
                    assertEquals( ip == 0 ? is : nStore,
                                  distrib.getSampleCount() );
                    distrib.submit( ip );
                }
            }

            /* Count how many output samples come from each input pass. */
            int[] counts = new int[ npass ];
            for ( int is = 0; is < nStore; is++ ) {
                counts[ (int) array[ is ] ]++;
            }

            /* Calculate a measure of the spread of these counts. */
            double sc = 0;
            double sc2 = 0;
            double nc = 0;
            for ( int ic = 0; ic < npass; ic++ ) {
                int c = counts[ ic ];
                nc++;
                sc += c;
                sc2 += c * c;
            }
            double cMean = sc / nc;
            double cVar = sc2 / nc - cMean * cMean;
            double scaledSd = Math.sqrt( cVar ) / cMean;

            /* This is an arbitrary threshold corresponding to the behaviour
             * of the best distributor I've come up with so far.
             * If you can push it down with a better Distributor
             * implementation, please do! */
            assertTrue( scaledSd < 0.27 );
        }
    }
}
