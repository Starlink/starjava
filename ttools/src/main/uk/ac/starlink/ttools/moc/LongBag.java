package uk.ac.starlink.ttools.moc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.PrimitiveIterator;

/**
 * IndexBag implementation that stores values individually.
 * It can hold any long value.
 *
 * @author  Mark Taylor
 * @since   28 Jan 2025
 */
public class LongBag implements IndexBag {

    private final Set<Long> longSet_;
    private final int setmax_;
    private long[] sortedLongs_;

    /** Default set size threshold. */
    public static final int DFLT_SETMAX = 100_000;

    /**
     * Constructs a LongBag with a default set size threshold.
     */
    public LongBag() {
        this( DFLT_SETMAX );
    }

    /**
     * Constructs a LongBag with a supplied set size threshold.
     *
     * @param  setmax  maximum HashSet size
     */
    public LongBag( int setmax ) {
        longSet_ = new HashSet<Long>();
        setmax_ = setmax;
        sortedLongs_ = new long[ 0 ];
    }

    public boolean hasIndex( long lval ) {
        return longSet_.contains( Long.valueOf( lval ) )
            || Arrays.binarySearch( sortedLongs_, lval ) >= 0;
    }

    public void addIndex( long lval ) {
        if ( Arrays.binarySearch( sortedLongs_, lval ) < 0 ) {
            longSet_.add( Long.valueOf( lval ) );
            if ( longSet_.size() > setmax_ ) {
                drainSet();
            }
        }
    }

    public long getCount() {
        drainSet();
        return sortedLongs_.length;
    }

    public PrimitiveIterator.OfLong sortedLongIterator() {
        drainSet();
        return Arrays.stream( sortedLongs_ ).iterator();
    }

    /**
     * Dumps all the values in the HashSet into the sorted array.
     * The result should be a more compact representation of the same content.
     * Following this call all the indices will be in the sorted array.
     */
    private void drainSet() {
        long[] array0 = sortedLongs_;
        long[] array1 = new long[ array0.length + longSet_.size() ];
        System.arraycopy( array0, 0, array1, 0, array0.length );
        int ix = array0.length;
        for ( Long lval : longSet_ ) {
            array1[ ix++ ] = lval.longValue();
        }
        longSet_.clear();
        Arrays.sort( array1 );
        sortedLongs_ = array1;
    }
}
