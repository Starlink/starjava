package uk.ac.starlink.table.join;

import junit.framework.TestCase;
import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.HealpixNestedPolygonComputer;

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

    public void testBug3() {

        // Prior to 0.30_3 this failed with AssertionError or
        // NullPointerException.
        int order = 12;
        HealpixNestedPolygonComputer pc =
            Healpix.getNested( 12 ).newPolygonComputer();
        double[][] verts1 = new double[][] {
            { -1.5702949547333407, -0.7295093151415473 },
            { -1.5702171673769950, -0.7295093016804524 },
            { -1.5701393800214274, -0.7295092852142693 },
            { -1.5700615926667945, -0.7295092657429985 },
        };
        double[][] verts2 = new double[][] {
            { -1.5706045044233712, -0.7295105218483977 },
            { -1.5705168372776197, -0.7295105199399403 },
            { -1.5704291701320918, -0.7295105142145686 },
            { -1.5703415029870114, -0.7295105046722821 },
        };
        pc.overlappingCells( verts1 );  // OK
        pc.overlappingCells( verts2 );  // trouble
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
