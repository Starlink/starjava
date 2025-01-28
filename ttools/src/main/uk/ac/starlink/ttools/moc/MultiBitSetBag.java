package uk.ac.starlink.ttools.moc;

import java.util.Arrays;
import java.util.BitSet;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;

/**
 * IndexBag implementation based on a dynamically managed collection
 * of BitSets.
 * Regions into which values are never stored take up no storage.
 * It can hold non-negative numbers up to a given limit.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public class MultiBitSetBag implements IndexBag {

    private final long size_;
    private final int bankSize_;
    private final long bankSizeL_;
    private final int nbank_;
    private final BitSet[] banks_;

    /** Default size for a single BitSet. */
    public static final int DFLT_BANKSIZE = 1024 * 1024 * 16;

    /**
     * Constructs a bag with a default bank size.
     * Non-negative integers smaller than the given size value can be held.
     *
     * @param  size one greater than the largest permitted value
     */
    public MultiBitSetBag( long size ) {
        this( size, DFLT_BANKSIZE );
    }

    /**
     * Constructs a bag with a specified bank size.
     * Non-negative integers smaller than the given size value can be held.
     *
     * @param  size one greater than the largest permitted value
     * @param  bankSize   size of sub-buffers into which the storage is
     *                    divided
     */
    public MultiBitSetBag( long size, int bankSize ) {
        size_ = size;
        bankSize_ = bankSize;
        bankSizeL_ = bankSize;
        long nb = ( ( size - 1 ) / bankSizeL_ ) + 1;
        nbank_ = (int) nb;
        if ( nbank_ != nb ) {
            throw new IllegalArgumentException( "Bank count " + nb
                                              + " too high" );
        }
        banks_ = new BitSet[ nbank_ ];
    }

    public void addIndex( long lindex ) {
        getBank( lindex ).set( getOffset( lindex ) );
    }

    public boolean hasIndex( long lindex ) {
        BitSet bank = getBank( lindex );
        return bank != null && bank.get( getOffset( lindex ) );
    }

    public long getCount() {
        return Arrays.stream( banks_ )
                     .mapToLong( b -> b == null ? 0 : b.cardinality() )
                     .sum();
    }

    public PrimitiveIterator.OfLong sortedLongIterator() {
        LongStream stream = LongStream.empty();
        for ( int ib = 0; ib < nbank_; ib++ ) {
            BitSet bank = banks_[ ib ];
            if ( bank != null ) {
                long offset = ib * bankSizeL_;
                LongStream s1 = bank.stream().asLongStream()
                               .map( l -> l + offset );
                stream = LongStream
                        .concat( stream,
                                 bank.stream().asLongStream()
                                     .map( l -> l + offset ) );
            }
        }
        return stream.iterator();
    }

    /**
     * Returns the bank index corresponding to a given integer.
     *
     * @param  lindex  value to store
     * @return   index of storage bank
     */
    private int getBankIndex( long lindex ) {
        return (int) ( lindex / bankSizeL_ );
    }

    /**
     * Returns a non-null bank corresponding to a given integer.
     *
     * @param  lindex  value to store
     * @return  storage bank, lazily constructed if necessary
     */
    private BitSet getBank( long lindex ) {
        int ibank = getBankIndex( lindex );
        BitSet bank = banks_[ ibank ];
        if ( bank == null ) {
            bank = new BitSet( bankSize_ );
            banks_[ ibank ] = bank;
        }
        return bank;
    }

    /**
     * Returns the offset into its bank of a given integer.
     *
     * @param  lindex  value to store
     * @return   storage offset
     */
    private int getOffset( long lindex ) {
        return (int) ( lindex % bankSizeL_ );
    }
}
