package uk.ac.starlink.table.join;

import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.HealpixNestedFixedRadiusConeComputer;
import java.util.List;
import junit.framework.TestCase;

/**
 * All the tests in this class fail with versions of PixTools
 * prior to 2017-09-06.
 */
public class RingBugTest extends TestCase {

    public void testRingBug() {
        double PI = Math.PI;
        double scale = 0.0001 * PI;
        double decRad = PI / 18.0;
        double radius = scale * 4;

        // This is not a particularly rigorous test,
        // versions of PixTools 2012-07-28 got it wrong.
        HealpixNested hn = Healpix.getNested( 2 );
        HealpixNestedFixedRadiusConeComputer coner =
            hn.newConeComputerApprox( radius );
        HealpixNestedBMOC bmoc1 =
            coner.overlappingCells( -0.1 * scale, - decRad );
        HealpixNestedBMOC bmoc2 =
            coner.overlappingCells( +0.1 * scale, - decRad );
        assertEquals( 1, bmoc1.computeDeepSize() );
        assertEquals( 1, bmoc2.computeDeepSize() );
        long ring1 = hn.toRing( bmoc1.flatHashIterator().next() );
        long ring2 = hn.toRing( bmoc2.flatHashIterator().next() );
        assertEquals( ring1, ring2 );
        assertEquals( 104, ring1 );
    }
}
