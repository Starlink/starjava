package uk.ac.starlink.table.join;

import junit.framework.TestCase;
import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;

public class CdsHealpixTest extends TestCase {

    public void testBug1() {

        // Prior to 0.28_1, this failed with an IllegalArgumentException
        // or AssertionError.
        HealpixNestedBMOC bmoc =
            Healpix.getNested(11)
                   .newConeComputerApprox(5./3600.)
                   .overlappingCells(0.5*Math.PI, -1.1617);
        assertEquals( 46, bmoc.size() );
    }
}
