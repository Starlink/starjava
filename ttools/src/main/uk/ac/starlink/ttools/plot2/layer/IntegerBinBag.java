package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a bounded list of bins.
 * Each bin is given a fixed integer label, from zero to a specified maximum.  
 * In practice, not all the bins may be used, and implementations may
 * take advantage of this.
 * Each bin accumulates a floating point value, which starts at zero.
 *
 * <p>Instances of this class are in general not thread-safe.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2015
 */
public abstract class IntegerBinBag {

    private final long size_;
    private final Combiner combiner_;

    /**
     * Constructor.
     *
     * @param   size   maximum number of bins
     * @param   combiner  combination method for bins
     */
    protected IntegerBinBag( long size, Combiner combiner ) {
        size_ = size;
        combiner_ = combiner;
    }

    /**
     * Returns the maximum number of bins.  All bins have an index in the
     * range 0..size-1.
     *
     * @return  bin count
     */
    public long getSize() {
        return size_;
    }

    /**
     * Returns the combination method used for bins.
     *
     * @return  combiner
     */
    public Combiner getCombiner() {
        return combiner_;
    }

    /**
     * Adds a given value to the bin at the given index.
     *
     * @param   index  bin index
     * @param   value  increment for the current bin value
     */
    public abstract void addToBin( long index, double value );

    /**
     * Returns the value that has been accumulated into the given bin index.
     *
     * @param  index  bin index
     * @return   bin value
     */
    public abstract double getValue( long index );

    /**
     * Returns the range of bin values currently present in all the bins.
     *
     * @return   2-element array giving (min,max) of all bin values
     */
    public abstract double[] getBounds();

    /**
     * Returns a bin bag instance for a given size and combination algorithm.
     *
     * @param  size  maximum number of bins
     * @param  combiner  combination algorithm
     * @return  new unpopulated bin bag
     */
    public static IntegerBinBag createBinBag( long size, Combiner combiner ) {

        /* If there are not too many bins, use an implementation based on
         * an array.  This is most efficient if a large proportion of the
         * bins are occupied, and efficient enough if the array size is
         * not too big compared to available memory. */
        if ( size < 200000 && combiner instanceof Combiner.ScalarCombiner ) {
            return new ScalarArrayBinBag( (int) size,
                                          (Combiner.ScalarCombiner) combiner );
        }

        /* Otherwise, use an implementation based on a hash.
         * This is most efficient if a small proportion of the bins
         * are occupied. */
        else {
            return new HashBinBag( size, combiner );
        }
    }

    /**
     * IntegerBinBag implementation based on a double[] array.
     * This can only be used if the array length is small enough
     * (memory usage will not benefit from sparseness).
     * It also requires that the bin state can be represented by
     * a single floating point value, that is it requires a
     * ScalarCombiner rather than a general Combiner.
     */
    private static class ScalarArrayBinBag extends IntegerBinBag {
        private final double[] array_;
        private final Combiner.ScalarCombiner scalarCombiner_;

        /**
         * Constructor.
         *
         * @param  size  number of bins
         * @param  combiner  combination algorithm
         */
        ScalarArrayBinBag( int size, Combiner.ScalarCombiner combiner ) {
            super( size, combiner );
            scalarCombiner_ = combiner;
            array_ = new double[ size ];
            Arrays.fill( array_, combiner.getInitialState() );
        }

        public void addToBin( long index, double value ) {
            int ix = (int) index;
            array_[ ix ] =
                scalarCombiner_.getUpdatedState( array_[ ix ], value );
        }

        public double getValue( long index ) {
            return scalarCombiner_.extractResult( array_[ (int) index ] );
        }

        public double[] getBounds() {
            double lo = Double.POSITIVE_INFINITY;
            double hi = Double.NEGATIVE_INFINITY;
            int n = (int) getSize();
            for ( int i = 0; i < n; i++ ) {
                double v = scalarCombiner_.extractResult( array_[ i ] );
                if ( v < lo ) {
                    lo = v;
                }
                if ( v > hi ) {
                    hi = v;
                }
            }
            return lo <= hi ? new double[] { lo, hi } : new double[] { 0, 1 };
        }
    }

    /**
     * IntegerBinBag implementation based on a hash.
     */
    private static class HashBinBag extends IntegerBinBag {
        private final Map<Long,Combiner.Container> map_;

        /**
         * Constructor.
         *
         * @param  size  number of bins
         */
        HashBinBag( long size, Combiner combiner ) {
            super( size, combiner );
            map_ = new HashMap<Long,Combiner.Container>();
        }

        public void addToBin( long index, double value ) {
            Long key = new Long( index );
            Combiner.Container container = map_.get( key );
            if ( container == null ) {
                container = getCombiner().createContainer();
                map_.put( key, container );
            }
            container.submit( value );
        }

        public double getValue( long index ) {
            Combiner.Container container = map_.get( new Long( index ) );
            return container == null ? 0 : container.getResult();
        }

        public double[] getBounds() {
            double lo = Double.POSITIVE_INFINITY;
            double hi = Double.NEGATIVE_INFINITY;
            for ( Combiner.Container container : map_.values() ) {
                double v = container.getResult();
                if ( v < lo ) {
                    lo = v;
                }
                if ( v > hi ) {
                    hi = v;
                }
            }
            return lo <= hi ? new double[] { lo, hi } : new double[] { 0, 1 };
        }
    }
}
