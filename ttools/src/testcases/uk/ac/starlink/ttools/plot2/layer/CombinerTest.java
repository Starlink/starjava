package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import junit.framework.TestCase;

public class CombinerTest extends TestCase {

    private final Random random_;
    private final Combiner[] combiners_;

    public CombinerTest() {
        random_ = new Random( 8882343 );
        combiners_ = Combiner.getKnownCombiners();
    }

    public void testCombinerTypes() {
        assertEquals( 1.0, Combiner.Type.INTENSIVE.getBinFactor( 0.5 ) );
        assertEquals( 1.0, Combiner.Type.EXTENSIVE.getBinFactor( 0.5 ) );
        assertEquals( 2.0, Combiner.Type.DENSITY.getBinFactor( 0.5 ) );
        assertEquals( 1.0, Combiner.Type.DENSITY.getBinFactor( 1.0 ) );
        assertFalse( Combiner.Type.INTENSIVE.isExtensive() );
        assertTrue( Combiner.Type.EXTENSIVE.isExtensive() );
        assertTrue( Combiner.Type.DENSITY.isExtensive() );
    }

    public void testBinLists() {
        for ( Combiner combiner : combiners_ ) {
            exerciseCombiner( combiner );
        }
    }

    public void testCombiners() {
        CTest[] ctests = new CTest[] {
            new CTest( Combiner.COUNT, 4, new double[] { 1, 0, 10, 20 } ),
            new CTest( Combiner.DENSITY, 4, new double[] { 1, 0, 10, 20 } ),
            new CTest( Combiner.HIT, 1, new double[] { 1, 0, 10, 20 } ),
            new CTest( Combiner.MAX, 9.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.MIN, -.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.SUM, 11.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.WEIGHTED_DENSITY,
                       11.5, new double[] { .5, -.5, 9.5, 2 } ),
            new CTest( Combiner.MEAN, 7.5, new double[] { 2, 4, 8, 16 } ),
            new CTest( Combiner.MEDIAN, 99,
                       new double[] { .9, 9, 99, 999, 9999 } ),
            new CTest( Combiner.MEDIAN, 2.5, new double[] { 1, 2, 3, 4 } ),
            new CTest( Combiner.SAMPLE_STDEV, Math.sqrt( 3.5 ),
                       new double[] { 1, 2, 3, 4, 5, 6 } ),
            new CTest( Combiner.POP_STDEV, Math.sqrt( 17.5 / 6.0 ),
                       new double[] { 1, 2, 3, 4, 5, 6 } ),
            new CTest( Combiner.Q1, 1.75, new double[] { 4, 1, 3, 2 } ),
            new CTest( Combiner.Q3, 4.0, new double[] { 5, 4, 1, 3, 2 } ),
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
            assertTrue( Double.isNaN( container.getCombinedValue() ) );
            container.submit( 1 );
            container.submit( 1 );
            assertFalse( Double.isNaN( container.getCombinedValue() ) );
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
            assertEquals( result_, container.getCombinedValue() );
            BinList.Result binResult = bl.getResult();
            assertEquals( result_, binResult.getBinValue( 1 ) );
            assertEquals( result_, binResult.compact().getBinValue( 1 ) );
            assertTrue( Double.isNaN( binResult.compact().getBinValue( 0 ) ) );
        }
    }

    private void exerciseCombiner( Combiner combiner ) {
        int nbin = 200;
        int nsamp = 4000;
        BitSet mask = new BitSet( nbin );
        BinList abins = combiner.createArrayBinList( nbin );
        BinList hbins = new HashBinList( nbin, combiner );
        AdaptiveBinList dbins = new AdaptiveBinList( nbin, combiner, 2 );
        assertTrue( dbins.isHash() );
        for ( int is = 0; is < nsamp; is++ ) {
            int ibin = random_.nextInt( nbin );
            if ( ! skipBin( nbin, ibin ) ) {
                double datum = Math.max( 0, ( random_.nextDouble() * 10 - 1 ) );
                abins.submitToBin( ibin, datum );
                hbins.submitToBin( ibin, datum );
                dbins.submitToBin( ibin, datum );
                mask.set( ibin );
            }
        }

        // If this assertion fails it doesn't mean the BinList code is
        // broken, but it does mean that the test is not covering all
        // the code paths.
        assertFalse( dbins.isHash() );
        int nOc = mask.cardinality();
        int nskip = 0;
        BinList.Result aResult = abins.getResult();
        BinList.Result acResult = aResult.compact();
        BinList.Result hResult = hbins.getResult();
        BinList.Result hcResult = hResult.compact();
        BinList.Result dResult = dbins.getResult();
        BinList.Result dcResult = dbins.getResult();
        for ( int ib = 0; ib < nbin; ib++ ) {
            double value = aResult.getBinValue( ib );
            assertEquals( value, hResult.getBinValue( ib ) );
            assertEquals( value, dResult.getBinValue( ib ) );
            assertEquals( value, acResult.getBinValue( ib ) );
            assertEquals( value, hcResult.getBinValue( ib ) );
            assertEquals( value, dcResult.getBinValue( ib ) );
            if ( skipBin( nbin, ib ) ) {
                nskip++;
                assertTrue( Double.isNaN( value ) );
            }
        }
        assertTrue( nskip > 1 );
        assertEquals( nOc, countOccupiedBins( aResult, nbin ) );
        assertEquals( nOc, countOccupiedBins( acResult, nbin ) );
        assertEquals( nOc, countOccupiedBins( hResult, nbin ) );
        assertEquals( nOc, countOccupiedBins( hcResult, nbin ) );
        assertEquals( nOc, countOccupiedBins( dResult, nbin ) );
        assertEquals( nOc, countOccupiedBins( dcResult, nbin ) );
    }

    private static boolean skipBin( int nbin, int ibin ) {
        return ibin > 0.5 * nbin && ibin < 0.625 * nbin;
    }

    private static int countOccupiedBins( BinList.Result result, int size ) {
        int nOc = 0;
        BitSet mask = new BitSet();
        for ( Iterator<Long> it = result.indexIterator(); it.hasNext(); ) {
            long lndex = it.next().longValue();
            int index = (int) lndex;
            assertTrue( index == lndex );  // not absolutely required,
                                           // but better be for testing
            assertTrue( index >= 0 && index < size );
            assertTrue( ! mask.get( index ) );
            mask.set( index );
            assertTrue( mask.get( index ) );
            nOc++;
        }
        return nOc;
    }
}
