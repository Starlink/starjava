package uk.ac.starlink.ttools.plot2.layer;

import java.util.BitSet;
import java.util.Iterator;
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
    private final boolean isCopyResult_;
    private final BitSet mask_;

    /**
     * Constructor.
     * The <code>isCopyResult</code> flag determines how the
     * {@link #getResult} method is implemented.
     * As a rule it should be true if an accumulating bin requires
     * more than a <code>double</code>'s worth of storage,
     * and false otherwise.
     *
     * @param  size   bin count
     * @param  combiner   combiner
     * @param  isCopyResult  true if getResult copies data to a new array,
     *                       false if it acts as an adapter on existing data
     */
    protected ArrayBinList( int size, Combiner combiner,
                            boolean isCopyResult ) {
        size_ = size;
        combiner_ = combiner;
        isCopyResult_ = isCopyResult;
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
        return isCopyResult_ ? createCopyResult() : createAdapterResult();
    }

    /**
     * Returns a Result object that extracts values as required from
     * the data structure into which bin values are accumulated.
     *
     * @return   bin result structure
     */
    private Result createAdapterResult() {
        return new MaskResult( mask_ ) {
            public double getPopulatedBinValue( int index ) {
                return getBinResultInt( index );
            }
        };
    }

    /**
     * Constructs and returns a Result object by reading the current state
     * of the bins and storing the values into a new array.
     *
     * @return   bin result structure
     */
    private Result createCopyResult() {
        double[] values = new double[ mask_.length() ];
        for ( Iterator<Long> it = createMaskIterator( mask_ ); it.hasNext(); ) {
            int ix = it.next().intValue();
            values[ ix ] = getBinResultInt( ix );
        }
        return new CopyResult( mask_, values );
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
                    Long result = new Long( ibit );
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
     * Partial result implementation implementation in which non-empty
     * bins are identified using a bit mask.
     */
    private static abstract class MaskResult implements Result {
        private final BitSet mask_;

        /**
         * Constructor.
         *
         * @param  mask  bit mask identifying populated bins
         */
        MaskResult( BitSet mask ) {
            mask_ = mask;
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
     * Result implementation based on a fixed double array.
     */
    private static class CopyResult extends MaskResult {
        private final double[] values_;

        /**
         * Constructor.
         *
         * @param  mask  bit mask identifying populated bins
         * @param  values   value array
         */
        CopyResult( BitSet mask, double[] values ) {
            super( mask );
            values_ = values;
        }
        public double getPopulatedBinValue( int index ) {
            return values_[ index ];
        }
    }
}
