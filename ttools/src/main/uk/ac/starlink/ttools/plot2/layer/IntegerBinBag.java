package uk.ac.starlink.ttools.plot2.layer;

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

    /**
     * Constructor.
     *
     * @param   size   maximum number of bins
     */
    protected IntegerBinBag( long size ) {
        size_ = size;
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
     * @return   2-element array giving (min,max) of all bin values;
     *           currently, zero is always considered present
     */
    public abstract double[] getBounds();

    /**
     * Returns a bin bag instance for a given size.
     *
     * @param  size  maximum number of bins
     * @return  new unpopulated bin bag
     */
    public static IntegerBinBag createBinBag( long size ) {

        /* If there are not too many bins, use an implementation based on
         * an array.  This is most efficient if a large proportion of the
         * bins are occupied, and efficient enough if the array size is
         * not too big compared to available memory. */
        if ( size < 200000 ) {
            return new ArrayBinBag( (int) size );
        }

        /* Otherwise, use an implementation based on a hash.
         * This is most efficient if a small proportion of the bins
         * are occupied. */
        else {
            return new HashBinBag( size );
        }
    }

    /**
     * IntegerBinBag implementation based on an integer array.
     */
    private static class ArrayBinBag extends IntegerBinBag {
        private final double[] array_;

        /**
         * Constructor.
         *
         * @param  size  number of bins
         */
        ArrayBinBag( int size ) {
            super( size );
            array_ = new double[ size ];
        }

        public void addToBin( long index, double value ) {
            array_[ (int) index ] += value;
        }

        public double getValue( long index ) {
            return array_[ (int) index ];
        }

        public double[] getBounds() {
            double lo = 0;
            double hi = 0;
            int n = (int) getSize();
            for ( int i = 0; i < n; i++ ) {
                double v = array_[ i ];
                if ( v < lo ) {
                    lo = v;
                }
                if ( v > hi ) {
                    hi = v;
                }
            }
            return new double[] { lo, hi };
        }
    }

    /**
     * IntegerBinBag implementation based on a hash.
     */
    private static class HashBinBag extends IntegerBinBag {
        private final Map<Long,DVal> map_;

        /**
         * Constructor.
         *
         * @param  size  number of bins
         */
        HashBinBag( long size ) {
            super( size );
            map_ = new HashMap<Long,DVal>();
        }

        public void addToBin( long index, double value ) {
            Long key = new Long( index );
            DVal dval = map_.get( key );
            if ( dval == null ) {
                dval = new DVal();
                map_.put( key, dval );
            }
            dval.value_ += value;
        }

        public double getValue( long index ) {
            DVal dval = map_.get( new Long( index ) );
            return dval == null ? 0 : dval.value_;
        }

        public double[] getBounds() {
            double lo = 0;
            double hi = 0;
            for ( DVal dval : map_.values() ) {
                double v = dval.value_;
                if ( v < lo ) {
                    lo = v;
                }
                if ( v > hi ) {
                    hi = v;
                }
            }
            return new double[] { lo, hi };
        }

        /**
         * Mutable container for a floating point value.
         * Used as map values.
         */
        private static class DVal {
            double value_;
        }
    }
}
