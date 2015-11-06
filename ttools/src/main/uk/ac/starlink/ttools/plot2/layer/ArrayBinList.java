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
    private final boolean isCopyResult_;

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
        return new Result() {
            public double getBinValue( long index ) {
                return getBinResultInt( (int) index );
            }
            public double[] getValueBounds() {
                double lo = Double.POSITIVE_INFINITY;
                double hi = Double.NEGATIVE_INFINITY;
                for ( int i = 0; i < size_; i++ ) {
                    double v = getBinResultInt( i );
                    if ( v < lo ) {
                        lo = v;
                    }
                    if ( v > hi ) {
                        hi = v;
                    }
                }
                return lo <= hi ? new double[] { lo, hi }
                                : new double[] { 0, 1 };
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
        final double[] values = new double[ size_ ];
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for ( int i = 0; i < size_; i++ ) {
            double v = getBinResultInt( i );
            if ( v < lo ) {
                lo = v;
            }
            if ( v > hi ) {
                hi = v;
            }
            values[ i ] = v;
        }
        final double[] bounds = lo <= hi ? new double[] { lo, hi }
                                         : new double[] { 0, 1 };
        return new Result() {
            public double getBinValue( long index ) {
                return values[ (int) index ];
            }
            public double[] getValueBounds() {
                return bounds.clone();
            }
        };
    }
}
