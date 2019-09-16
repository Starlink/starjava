package uk.ac.starlink.ttools.plot2.layer;

/**
 * Accumulates counts in an array of bins.
 *
 * <p>An earlier implementation started off with a <code>byte[]</code> array
 * and dynamically adjusted the storage as the maximum bin count increased
 * to a <code>short[]</code> and then an <code>int[]</code> array,
 * to save on memory.
 * The current implementation just uses an <code>int[]</code> array,
 * on the untested assumption that
 * the extra cleverness is more trouble than it's worth;
 * the array size is not going to be of unlimited size
 * (expected use is to map a pixel grid, so it will usually be not much
 * more than a million).
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class Binner {

    private final int n_;
    private long total_;
    private int[] array_;

    /**
     * Constructor.
     *
     * @param   n   number of bins
     */
    public Binner( int n ) {
        n_ = n;
        array_ = new int[ n ];
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
        int value = array_[ index ];
        if ( value != Integer.MAX_VALUE ) {
            array_[ index ] = value + 1;
        }
        total_++;
    }

    /**
     * Returns the count in a given bin.
     *
     * @param  index  bin index
     * @return   current total for given bin, or Integer.MAX_VALUE
     *           in case of overflow
     */
    public int getCount( int index ) {
        return array_[ index ];
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
     * Adds the contents of another binner to this one.
     * The effect is as if all the increments made to the other bin
     * were made to this one as well.
     *
     * @param  other  other binner, expected to be the same size as this
     */
    public void add( Binner other ) {
        int n = Math.min( this.n_, other.n_ );
        int[] otherArray = other.array_;
        for ( int i = 0; i < n; i++ ) {
            long sum = array_[ i ] + otherArray[ i ];
            array_[ i ] = sum < Integer.MAX_VALUE ? (int) sum
                                                  : Integer.MAX_VALUE;
        }
        total_ += other.total_;
    }
}
