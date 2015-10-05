package uk.ac.starlink.ttools.plot2.layer;

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

    /**
     * Constructor.
     *
     * @param  size   bin count
     * @param  combiner   combiner
     */
    protected ArrayBinList( int size, Combiner combiner ) {
        size_ = size;
        combiner_ = combiner;
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

    public void submitToBin( long index, double datum ) {
        submitToBinInt( (int) index, datum );
    }

    public double getBinResult( long index ) {
        return getBinResultInt( (int) index );
    }

    public double[] getBounds() {
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        int n = (int) getSize();
        for ( int i = 0; i < n; i++ ) {
            double v = getBinResultInt( i );
            if ( v < lo ) {
                lo = v;
            }
            if ( v > hi ) {
                hi = v;
            }
        }
        return lo <= hi ? new double[] { lo, hi } : new double[] { 0, 1 };
    }

    public long getSize() {
        return size_;
    }

    public Combiner getCombiner() {
        return combiner_;
    }
}
