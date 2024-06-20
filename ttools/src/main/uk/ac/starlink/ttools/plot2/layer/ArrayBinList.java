package uk.ac.starlink.ttools.plot2.layer;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Abstract subclass of BinList suitable for implementations based
 * on arrays.  The defining feature is that the the maximum bin count
 * can be described as an int rather than a long.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2015
 */
public abstract class ArrayBinList implements BinList {

    private final int size_;
    private final Combiner combiner_;
    private final BitSet mask_;

    /**
     * Constructor.
     *
     * @param  size   bin count
     * @param  combiner   combiner
     */
    protected ArrayBinList( int size, Combiner combiner ) {
        size_ = size;
        combiner_ = combiner;
        mask_ = new BitSet( size );
    }

    /**
     * Variant of the <code>addToBin</code> method
     * that takes a 32-bit index.
     *
     * @param   index  bin index
     * @param   value  increment for the current bin value
     */
    protected abstract void submitToBinInt( int index, double value );

    /**
     * Variant of the <code>getValue</code> method
     * that takes a 32-bit index.
     *
     * @param  index  bin index
     * @return   bin value
     */
    protected abstract double getBinResultInt( int index );

    /**
     * Copies bin content from a Container into the storage used by this
     * implementation for a given bin.
     * The container must be one appropriate to this bin list's Combiner.
     *
     * @param  index  bin index
     * @param  container  combiner's container containing bin state
     */
    protected abstract void copyBin( int index, Combiner.Container container );

    /**
     * Accumulates the contents of a numbered bin from another BinList into
     * the corresponding bin of this BinList.  The effect is the same as if
     * all the data submitted to the given bin of <code>other</code>
     * had been submitted to the corresponding bin of this.
     *
     * <p>The other list must be of the same type as this one.
     *
     * @param  index   index of a non-empty bin in the other list
     * @param  other   second ArrayBinList compatible with this one
     * @throws   ClassCastException   if <code>other</code>'s type
     *                                does not match this one
     */
    protected abstract void addBin( int index, ArrayBinList other );

    public void submitToBin( long lndex, double datum ) {
        int index = (int) lndex;
        mask_.set( index );
        submitToBinInt( index, datum );
    }

    public long getSize() {
        return size_;
    }

    public Combiner getCombiner() {
        return combiner_;
    }

    public Result getResult() {
        return new MaskResult( mask_, size_, combiner_.hasBigBin() ) {
            public double getPopulatedBinValue( int index ) {
                return getBinResultInt( index );
            }
        };
    }

    public Combiner.Container getBinContainer( long index ) {
        int ix = (int) index;
        if ( mask_.get( ix ) && ix == index ) {
            Combiner.Container container = combiner_.createContainer();
            copyBin( ix, container );
            return container;
        }
        else {
            return null;
        }
    }

    /**
     * Accumulates all the data from another BinList into this one.
     * The effect is the same as if all the data submitted to <code>other</code>
     * had been submitted to this.
     *
     * <p>The other list must be compatible with this one; of the same
     * type and with the same bin count.
     *
     * @param  other   second ArrayBinList compatible with this one
     * @throws   ClassCastException   if <code>other</code>'s type
     *                                does not match this one
     */
    public void addBins( ArrayBinList other ) {
        BitSet otherMask = other.mask_;
        for ( int ibit = otherMask.nextSetBit( 0 ); ibit >= 0;
              ibit = otherMask.nextSetBit( ibit + 1 ) ) {
            mask_.set( ibit );
            addBin( ibit, other );
        }
    }

    /**
     * Tries to create an ArrayBinList with the same content as a
     * supplied HashBinList.  Null may be returned if it can't be done.
     *
     * @param  in  bin list whose data is to be copied
     * @return  array bin list with copied content, or null if unsuccessful
     */
    public static ArrayBinList fromHashBinList( HashBinList in ) {
        int size = (int) in.getSize();
        if ( size != in.getSize() ) {
            return null;
        }
        ArrayBinList out = in.getCombiner().createArrayBinList( size );
        if ( out == null ) {
            return null;
        }
        Map<Long,Combiner.Container> inMap = in.getMap();
        for ( Map.Entry<Long,Combiner.Container> entry : inMap.entrySet() ) {
            Combiner.Container bin = entry.getValue();
            if ( bin != null ) {
                Long lndex = entry.getKey();
                int ix = lndex.intValue();
                assert ix == lndex.longValue();
                out.copyBin( ix, bin );
                out.mask_.set( ix );
            }
        }
        return out;
    }

    /**
     * Returns an iterator over the indices of the set bits in a BitSet.
     *
     * @param  mask  bit mask
     * @return  iterator over set bit indices in mask
     */
    private static Iterator<Long> createMaskIterator( final BitSet mask ) {
        return new Iterator<Long>() {
            int ibit = mask.nextSetBit( 0 );
            public Long next() {
                if ( ibit >= 0 ) {
                    Long result = Long.valueOf( ibit );
                    ibit = mask.nextSetBit( ibit + 1 );
                    return result;
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public boolean hasNext() {
                return ibit >= 0;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Partial result implementation implementation in which occupied
     * bins are identified using a bit mask.
     */
    private static abstract class MaskResult implements Result {
        private final BitSet mask_;
        private final int arraysize_;
        private final boolean hasBigBin_;

        /**
         * Constructor.
         *
         * @param  mask  bit mask identifying populated bins
         * @param  arraysize  size of original array (max possible bin index+1)
         * @param  hasBigBin  whether the combiner bins are large objects
         *                    (larger than a double)
         */
        MaskResult( BitSet mask, int arraysize, boolean hasBigBin ) {
            mask_ = mask;
            arraysize_ = arraysize;
            hasBigBin_ = hasBigBin;
        }

        public long getBinCount() {
            return mask_.cardinality();
        }

        public Iterator<Long> indexIterator() {
            return createMaskIterator( mask_ );
        }

        public double getBinValue( long lndex ) {
            int index = (int) lndex;
            return mask_.get( index ) ? getPopulatedBinValue( index )
                                      : Double.NaN;
        }

        public Result compact() {

            /* If the array is sparse, use a hash-based implementation. */
            double frac = mask_.cardinality() * 1.0 / arraysize_;
            if ( frac < 0.25 ) {
                Map<Long,Double> map = new HashMap<Long,Double>();
                for ( Iterator<Long> it = indexIterator(); it.hasNext(); ) {
                    Long key = it.next();
                    map.put( key, getPopulatedBinValue( key.intValue() ) );
                }
                return HashBinList.createHashResult( map );
            }

            /* If the value container array elements are large, copy the
             * results into a primitive array instead. /
            else if ( hasBigBin_ ) {
                double[] darray = new double[ arraysize_ ];
                for ( int i = 0; i < arraysize_; i++ ) {
                    darray[ i ] = getBinValue( i );
                }
                return createDoubleMaskResult( mask_, darray );
            }

            /* Otherwise, it's compact enough already. */
            else {
                return this;
            }
        }

        /**
         * Returns the numeric value of a bin that is known to have been
         * populated.
         *
         * @param   index   bin index
         * @return   bin value
         */
        abstract double getPopulatedBinValue( int index );
    }

    /**
     * Returns a Result implementation based on a bin occupation mask
     * and an array of bin content values.
     *
     * @param   mask  bin occupation mask
     * @param   values   data values per bin
     */
    public static Result createDoubleMaskResult( final BitSet mask,
                                                 final double[] values ) {
        return new Result() {
            public double getBinValue( long lndex ) {
                int index = (int) lndex;
                return mask.get( index ) ? values[ index ] : Double.NaN;
            }
            public long getBinCount() {
                return mask.cardinality();
            }
            public Iterator<Long> indexIterator() {
                return createMaskIterator( mask );
            }
            public Result compact() {
                return this;
            }
        };
    }
}
