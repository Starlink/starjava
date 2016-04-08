package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class CombinerTest extends TestCase {

    private final Random random_;
    private final Combiner[] combiners_;

    public CombinerTest() {
        random_ = new Random( 8882343 );
        combiners_ = Combiner.getKnownCombiners();
    }

    public void testBinLists() {
        for ( Combiner combiner : combiners_ ) {
            exerciseCombiner( combiner );
        }
    }

    public void testCombiners() {
        CTest[] ctests = new CTest[] {
            new CTest( Combiner.COUNT, 4, new double[] { 1, 0, 10, 20 } ),
            new CTest( Combiner.HIT, 1, new double[] { 1, 0, 10, 20 } ),
            new CTest( Combiner.MAX, 9.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.MIN, -.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.SUM, 11.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.MEAN, 7.5, new double[] { 2, 4, 8, 16 } ),
            new CTest( Combiner.MEDIAN, 99,
                       new double[] { .9, 9, 99, 999, 9999 } ),
            new CTest( Combiner.MEDIAN, 2.5, new double[] { 1, 2, 3, 4 } ),
            new CTest( Combiner.SAMPLE_VARIANCE, 3.5,
                       new double[] { 1, 2, 3, 4, 5, 6 } ),
        };
        Set<Combiner> cset = new HashSet<Combiner>();
        for ( CTest ct : ctests ) {
            cset.add( ct.combiner_ );
            ct.checkData();
        }
        assertEquals( "Not all instances tested",
                      cset,
                      new HashSet<Combiner>( Arrays.asList( combiners_ ) ) );
    }

    public void testEmpties() {
        for ( Combiner combiner : combiners_ ) {
            Combiner.Container container = combiner.createContainer();
            assertTrue( Double.isNaN( container.getResult() ) );
            container.submit( 1 );
            container.submit( 1 );
            assertFalse( Double.isNaN( container.getResult() ) );
        }
    }

    private class CTest {
        final Combiner combiner_;
        final double result_;
        final double[] data_;

        CTest( Combiner combiner, double result, double[] data ) {
            combiner_ = combiner;
            result_ = result;
            data_ = data;
        }

        void checkData() {
            Combiner.Container container = combiner_.createContainer();
            BinList bl = combiner_.createArrayBinList( 3 );
            for ( double datum : data_ ) {
                container.submit( datum );
                bl.submitToBin( 1, datum );
            }
            assertEquals( result_, container.getResult() );
            BinList.Result binResult = bl.getResult();
            assertEquals( result_, binResult.getBinValue( 1 ) );
            assertTrue( Double.isNaN( binResult.getBinValue( 0 ) ) );
        }
    }

    private void exerciseCombiner( Combiner combiner ) {
        int nbin = 200;
        int nsamp = 1000;
        BinList abins = combiner.createArrayBinList( nbin );
        BinList hbins1 = new HashBinList( nbin, combiner, false );
        BinList hbins2 = new HashBinList( nbin, combiner, true );
        for ( int is = 0; is < nsamp; is++ ) {
            int ibin = random_.nextInt( nbin );
            if ( ! skipBin( nbin, ibin ) ) {
                double datum = Math.max( 0, ( random_.nextDouble() * 10 - 1 ) );
                abins.submitToBin( ibin, datum );
                hbins1.submitToBin( ibin, datum );
                hbins2.submitToBin( ibin, datum );
            }
        }
        int nskip = 0;
        BinList.Result aResult = abins.getResult();
        BinList.Result h1Result = hbins1.getResult();
        BinList.Result h2Result = hbins2.getResult();
        for ( int ib = 0; ib < nbin; ib++ ) {
            assertEquals( h1Result.getBinValue( ib ),
                          h2Result.getBinValue( ib ) );
            assertEquals( aResult.getBinValue( ib ),
                          h1Result.getBinValue( ib ) );
            if ( skipBin( nbin, ib ) ) {
                nskip++;
                assertTrue( Double.isNaN( aResult.getBinValue( ib ) ) );
            }
        }
        assertTrue( nskip > 1 );
    }

    private static boolean skipBin( int nbin, int ibin ) {
        return ibin > 0.5 * nbin && ibin < 0.6 * nbin;
    }
}
