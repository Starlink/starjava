package uk.ac.starlink.ttools.plot2.geom;

import junit.framework.TestCase;

public class DateTickLevelTest extends TestCase {

    public void testDivision() {
        for ( int d = 1; d < 6; d++ ) {
            for ( long n = -12; n < 12; n++ ) {
                long f = DateTickLevel.floorDiv( n, d );
                int r = DateTickLevel.remainder( n, d );
                assertTrue( r >= 0 );
                assertTrue( r < d );
                assertTrue( d * f <= n );
                assertTrue( d * ( f + 1 ) > n );
                assertEquals( n, d * f + r );
            }
        }
    }
}
