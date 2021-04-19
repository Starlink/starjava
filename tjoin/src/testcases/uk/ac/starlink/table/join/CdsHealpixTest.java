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

    public void testBug2() {

        // Prior to 0.29, this failed with an IllegalArgumentException.
        // It's essentially the same as Bug1, but with different troublesome
        // parameters.  According to F-X the fix applied this time
        // (a data structure without a fixed size limit) ought to prevent
        // similar behaviour for any parameter values in the future.
        HealpixNestedBMOC bmoc =
            Healpix.getNested(14)
                   .newConeComputerApprox(0.001)
                   .overlappingCells(0.002, -1.3);
        assertEquals( 338, bmoc.size() );
    }

    void writeBmoc( HealpixNestedBMOC bmoc ) {
        System.out.println( "Depth,Hash,Raw,Full" );
        for ( HealpixNestedBMOC.CurrentValueAccessor acc : bmoc ) {
            System.out.println( acc.getDepth() + ","
                              + acc.getHash() + ","
                              + acc.getRawValue() + ","
                              + acc.isFull() );
        }
    }
}
