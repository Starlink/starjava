package uk.ac.starlink.ttools.moc;

import java.util.BitSet;
import java.util.PrimitiveIterator;

/**
 * IndexBag implementation based on a BitSet.
 * It can hold non-negative numbers up to a given limit.
 *
 * <p>A fixed amount of memory is used, namely <code>size</code> bits
 * (plus small change).
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public class BitSetBag implements IndexBag {

    private final BitSet bitset_;

    /**
     * Constructor.
     * Non-negative integers smaller than the given size value can be held.
     *
     * @param  size  one greater than largest permitted value
     */
    public BitSetBag( int size ) {
        bitset_ = new BitSet( size );
    }

    public void addIndex( long lval ) {
        bitset_.set( (int) lval );
    }

    public boolean hasIndex( long lval ) {
        return bitset_.get( (int) lval );
    }

    public long getCount() {
        return bitset_.cardinality();
    }

    public PrimitiveIterator.OfLong sortedLongIterator() {
        return bitset_.stream().mapToLong( i -> (long) i ).iterator();
    }
}
