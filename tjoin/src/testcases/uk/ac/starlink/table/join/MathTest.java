package uk.ac.starlink.table.join;

import cds.healpix.common.math.FastMath;
import junit.framework.TestCase;

public class MathTest extends TestCase {

    public void testAsin() {
        for ( double d = -1; d < 1; d += 0.00001 ) { 
            assertEquals( Math.asin( d ), FastMath.asin( d ), 1e-15 );
        }
    }
}
