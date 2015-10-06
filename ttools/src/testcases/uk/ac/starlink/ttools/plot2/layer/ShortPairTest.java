package uk.ac.starlink.ttools.plot2.layer;

import junit.framework.TestCase;

public class ShortPairTest extends TestCase {
    public void testPacking() {
        for ( int i = -2; i < 3; i++ ) {
            for ( int j = -2; j < 3; j++ ) {
                int ix = 10000 * i;
                int iy = 32 * j;
                ShortPair p = new ShortPair( (short) ix, (short) iy );
                assertEquals( ix, p.getX() );
                assertEquals( iy, p.getY() );
            }
        }
    }
}
