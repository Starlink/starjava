package uk.ac.starlink.ttools.moc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.PrimitiveIterator;

/**
 * IndexBag implementation that stores values individually.
 * It can hold any int value.
 *
 * @author  Mark Taylor
 * @since   28 Jan 2025
 */
public class IntegerBag implements IndexBag {

    private final Set<Integer> intSet_;
    private final int setmax_;
    private int[] sortedInts_;

    /** Default set size threshold. */
    public static final int DFLT_SETMAX = 100_000;

    /**
     * Constructs an IntegerBag with a default set size threshold.
     */
    public IntegerBag() {
        this( DFLT_SETMAX );
    }

    /**
     * Constructs an IntegerBag with a supplied set size threshold.
     *
     * @param  setmax  maximum HashSet size
     */
    public IntegerBag( int setmax ) {
        intSet_ = new HashSet<Integer>( setmax );
        setmax_ = setmax;
        sortedInts_ = new int[ 0 ];
    }

    public boolean hasIndex( long lval ) {
        int ival = (int) lval;
        return intSet_.contains( Integer.valueOf( ival ) )
            || Arrays.binarySearch( sortedInts_, ival ) >= 0;
    }

    public void addIndex( long lval ) {
        int ival = (int) lval;
        if ( Arrays.binarySearch( sortedInts_, ival ) < 0 ) {
            intSet_.add( Integer.valueOf( ival ) );
            if ( intSet_.size() > setmax_ ) {
                drainSet();
            }
        }
    }

    public long getCount() {
        drainSet();
        return sortedInts_.length;
    }

    public PrimitiveIterator.OfLong sortedLongIterator() {
        drainSet();
        return Arrays.stream( sortedInts_ ).asLongStream().iterator();
    }

    /**
     * Dumps all the values in the HashSet into the sorted array.
     * The result should be a more compact representation of the same content.
     * Following this call all the indices will be in the sorted array.
     */
    private void drainSet() {
        int[] array0 = sortedInts_;
        int[] array1 = new int[ array0.length + intSet_.size() ];
        System.arraycopy( array0, 0, array1, 0, array0.length );
        int ix = array0.length;
        for ( Integer ival : intSet_ ) {
            array1[ ix++ ] = ival.intValue();
        }
        intSet_.clear();
        Arrays.sort( array1 );
        sortedInts_ = array1;
    }
}
