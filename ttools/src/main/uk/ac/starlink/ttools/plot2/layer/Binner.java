package uk.ac.starlink.ttools.plot2.layer;

/**
 * Accumulates counts in an array of bins.
 *
 * <p>The array type starts at bytes and is dynamically adjusted
 * to contain counts up to int size to save on memory.
 * Is this overengineered?
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class Binner {

    private final int n_;
    private long total_;
    private ArrayBinner aBinner_;

    /**
     * Constructor.
     *
     * @param   n   number of bins
     */
    public Binner( int n ) {
        n_ = n;
        aBinner_ = new ByteBinner( n );
    }

    /**
     * Returns the number of bins.
     *
     * @return  bin count
     */
    public int getLength() {
        return n_;
    }

    /**
     * Increments the count in a given bin by 1.
     *
     * @param  index  bin index
     */
    public void increment( int index ) {
        total_++;
        if ( ! aBinner_.increment( index ) ) {
            final ArrayBinner aBinner1;
            if ( aBinner_ instanceof ByteBinner ) {
                aBinner1 = new ShortBinner( n_ );
            }
            else if ( aBinner_ instanceof ShortBinner ) {
                aBinner1 = new IntBinner( n_ );
            }
            else {
                throw new RuntimeException( "Do what?" );
            }
            for ( int i = 0; i < n_; i++ ) {
                aBinner1.setCount( i, aBinner_.getCount( i ) );
            }
            aBinner1.setCount( index, aBinner_.maxValue() );
            aBinner_ = aBinner1;
            increment( index );
        }
    }
 
    /**
     * Returns the count in a given bin.
     *
     * @param  index  bin index
     */
    public int getCount( int index ) {
        return aBinner_.getCount( index );
    }

    /**
     * Returns the total number of increments made to this binner.
     *
     * @return  sum of all bins
     */
    public long getTotal() {
        return total_;
    }

    /**
     * Abstract Binner utility class based on an array.
     */
    private static abstract class ArrayBinner {

        /**
         * Increments a bin and indicates overflow.
         *
         * @param  index  bin index
         * @return  true if increment worked, false on overflow
         */
        abstract boolean increment( int index );

        /**
         * Returns the maximum value storable by this object per bin.
         * This is the value which causes overflow.
         *
         * @return  bin count limit
         */
        abstract int maxValue();

        /**
         * Returns the count in a given bin.
         *
         * @param  index  bin index
         * @return  count in bin
         */
        abstract int getCount( int index );

        /**
         * Sets the count in a given bin.
         *
         * @param  index  bin index
         * @param  value  new count value
         */
        abstract void setCount( int index, int value );
    }

    /**
     * ArrayBinner based on a byte array.
     */
    private static class ByteBinner extends ArrayBinner {
        final byte[] buf_;
        ByteBinner( int n ) {
            buf_ = new byte[ n ];
        }
        int maxValue() {
            return Byte.MAX_VALUE - Byte.MIN_VALUE;
        }
        boolean increment( int index ) {
            return ++buf_[ index ] != 0;
        }
        int getCount( int index ) {
            return buf_[ index ] & 0xff;
        }
        void setCount( int index, int value ) {
            buf_[ index ] = (byte) value;
        }
    }

    /**
     * Array binner based on a short array.
     */
    private static class ShortBinner extends ArrayBinner {
        final short[] buf_;
        ShortBinner( int n ) {
            buf_ = new short[ n ];
        }
        int maxValue() {
            return Short.MAX_VALUE - Short.MIN_VALUE;
        }
        boolean increment( int index ) {
            return ++buf_[ index ] != 0;
        }
        int getCount( int index ) {
            return buf_[ index ] & 0xffff;
        }
        void setCount( int index, int value ) {
            buf_[ index ] = (short) value;
        }
    }

    /**
     * Array binner based on an int array.
     */
    private static class IntBinner extends ArrayBinner {
        final int[] buf_;
        IntBinner( int n ) {
            buf_ = new int[ n ];
        }
        int maxValue() {
            return Integer.MAX_VALUE;
        }
        boolean increment( int index ) {
            return ++buf_[ index ] > 0;
        }
        int getCount( int index ) {
            return buf_[ index ];
        }
        void setCount( int index, int value ) {
            buf_[ index ] = value;
        }
    }
}
