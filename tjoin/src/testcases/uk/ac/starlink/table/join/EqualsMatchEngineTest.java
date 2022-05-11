package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import uk.ac.starlink.util.TestCase;

public class EqualsMatchEngineTest extends TestCase {

    private final Supplier<MatchKit> kitFact_;

    public EqualsMatchEngineTest( String name ) {
        super( name );
        kitFact_ = new EqualsMatchEngine().createMatchKitFactory();
    }

    public void testEquals() {
        assertMatch( true, new Integer( 4 ), new Integer( 4 ) );
        assertMatch( true, "abc", "abc" );
        assertMatch( false, new Integer( 350 ), new Integer( 351 ) );
        assertMatch( false, new Integer( 350 ), new Long( 351L ) );
        assertMatch( true, new Integer( 4 ), new Long( 4 ) );
        assertMatch( true, new Short( (short) 4 ), new Float( 4 ) );
        assertMatch( false, new Double( Math.PI ), new Long( 3 ) );
        assertMatch( true, new int[] { 1, 1, 2, 3, 5, 8, 11, 19, },
                           new int[] { 1, 1, 2, 3, 5, 8, 11, 19, } );
        assertMatch( false, new float[] { 1, 1, 2, 3, 5, 8, 11, 19, }, 
                            new float[] { 1, 1, 2, 3, 5, 8, 11, 20, } );
        assertMatch( false, new float[] { 1, 1, 2, 3, 5, 8, 11, 19, }, 
                            new float[] { 1, 1, 2, 3, 5, 8, 11, 19, 30, } );
        boolean[] aL = new boolean[] { true, false, true, };
        char[] aC = new char[] { 'M', 'a', 'r', 'k', };
        byte[] aB = new byte[] { (byte) 1, (byte) 2, (byte) 3, };
        short[] aS = new short[] { (short) 4, (short) 5, (short) 6, };
        int[] aI = new int[] { 7, 8, 9, };
        long[] aJ = new long[] { 10L, 11L, 12L, };
        float[] aF = new float[] { 3.14f, 2.17f, };
        double[] aD = new double[] { Math.PI, Math.E };
        assertMatch( true, aL, aL.clone() );
        assertMatch( true, aC, aC.clone() );
        assertMatch( true, aB, aB.clone() );
        assertMatch( true, aS, aS.clone() );
        assertMatch( true, aI, aI.clone() );
        assertMatch( true, aJ, aJ.clone() );
        assertMatch( true, aF, aF.clone() );
        assertMatch( true, aD, aD.clone() );
        assertMatch( true, new Object[] { aL, aC, aB, aS, aI, aJ, aF, aD },
                           new Object[] { aL, aC, aB, aS, aI, aJ, aF, aD } );
        float[] aF1 = (float[]) aF.clone();
        aF1[ 1 ]++;
        assertMatch( false, new Object[] { aL, aC, aB, aS, aI, aJ, aF, aD },
                            new Object[] { aL, aC, aB, aS, aI, aJ, aF1, aD } );
    }

    private void assertMatch( boolean match, Object o1, Object o2 ) {
        MatchKit kit = kitFact_.get();
        assertEquals( match ? 0.0 : -1.0,
                      kit.matchScore( new Object[] { o1 },
                                      new Object[] { o2 } ) );
        if ( match ) {
            assertEquals( kit.getBins( new Object[] { o1 } )[ 0 ],
                          kit.getBins( new Object[] { o2 } )[ 0 ] );
        }
    }
}
