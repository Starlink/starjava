package uk.ac.starlink.ttools.gpl;

import java.util.function.Supplier;
import java.util.concurrent.ForkJoinPool;
import uk.ac.starlink.ttools.filter.IntComparator;

/**
 * Utilities for sorting.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2025
 */
public class SortUtils {

    /**
     * The minimum array length below which a parallel sorting
     * algorithm will not further partition the sorting task. Using
     * smaller sizes typically results in memory contention across
     * tasks that makes parallel speedups unlikely.
     * This value and text taken from java.util.Arrays.
     */ 
    private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;

    /**
     * Private constructor prevents instantiation.
     */
    private SortUtils() {
    }

    /**
     * Serial sort of an integer array with a custom comparator.
     *
     * @param  array  array to sort
     * @param  cmp  comparator
     */
    public static void intSort( int[] array, IntComparator cmp ) {
        if ( cmp == null ) {
            cmp = Integer::compare;
        }
        IntTimSort.sort( array, 0, array.length, cmp, null, 0, 0 );
    }

    /**
     * Parallel sort of an integer array with a custom comparator.
     *
     * @param  array  array to sort
     * @param  cmp  comparator
     */
    public static void parallelIntSort( int[] array, IntComparator cmp ) {
        final IntComparator cmp0 = cmp == null ? Integer::compare : cmp;
        parallelIntSort( array, () -> cmp0 );
    }
        
    /**
     * Parallel sort of an integer array with a custom, possibly
     * non-thread-safe, comparator.
     * Any comparator obtained from the supplied factory will only
     * be used within a single thread.
     *
     * @param  array  array to sort
     * @param  cmpSupplier  supplier for comparators
     */
    public static void parallelIntSort( int[] array,
                                        Supplier<? extends IntComparator> cmpSupplier ) {
        if ( cmpSupplier == null ) {
            cmpSupplier = () -> Integer::compare;
        }
        int n = array.length;
        int p = ForkJoinPool.getCommonPoolParallelism();
        if ( n <= MIN_ARRAY_SORT_GRAN || p == 1 ) {
            IntTimSort.sort( array, 0, n, cmpSupplier.get(), null, 0, 0 );
        }
        else {
            int g = n / ( p << 2 );
            new ArraysParallelSortHelpers.FJObject.Sorter(
                    null, array, new int[ n ], 0, n, 0,
                    ( g <= MIN_ARRAY_SORT_GRAN ) ? MIN_ARRAY_SORT_GRAN : g,
                    cmpSupplier )
           .invoke();
        }
    }
}
