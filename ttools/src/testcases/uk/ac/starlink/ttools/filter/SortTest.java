package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.gpl.SortUtils;
import uk.ac.starlink.util.TestCase;

public class SortTest extends TestCase {

    public void testSort() {
        int n = ( 1 << 13 ) * 3;
        int[] fwd = new int[ n ];
        int[] rev = new int[ n ];
        int[] rnd = new int[ n ];
        List<Integer> work = new ArrayList<>( n );
        for ( int i = 0; i < n; i++ ) {
            fwd[ i ] = i;
            rev[ i ] = n - i - 1;
            work.add( Integer.valueOf( i ) );
        }
        Collections.shuffle( work, new Random( 99120043 ) );
        for ( int i = 0; i < n; i++ ) {
            rnd[ i ] = work.get( i ).intValue();
        }
        IntComparator invCmp = (a, b) -> Integer.compare( b, a );
        {
            int[] a1 = rnd.clone();
            SortUtils.intSort( a1, null );
            assertArrayEquals( fwd, a1 );
        }
        {
            int[] a2 = rnd.clone();
            SortUtils.intSort( a2, invCmp );
            assertArrayEquals( rev, a2 );
        }
        {
            int[] a3 = rnd.clone();
            SortUtils.parallelIntSort( a3, invCmp );
            assertArrayEquals( rev, a3 );
        }
        {
            int[] a4 = rnd.clone();
            CountSupplier csupp = new CountSupplier( () -> invCmp );
            SortUtils.parallelIntSort( a4, csupp );
            assertArrayEquals( rev, a4 );
            assert csupp.count_ > 1
                || ForkJoinPool.getCommonPoolParallelism() <= 1;
        }
    }

    private class CountSupplier implements Supplier<IntComparator> {
        final Supplier<IntComparator> baseSupplier_;
        int count_;
        CountSupplier( Supplier<IntComparator> baseSupplier ) {
            baseSupplier_ = baseSupplier;
        }
        public IntComparator get() {
            count_++;
            final Thread thread = Thread.currentThread();
            final IntComparator cmp = baseSupplier_.get();
            return (a, b) -> {
                assertEquals( thread, Thread.currentThread() );
                return cmp.compare( a, b );
            };
        }
    }
}
