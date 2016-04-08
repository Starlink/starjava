package uk.ac.starlink.ttools.plot2.layer;

import java.util.HashMap;
import java.util.Map;

/**
 * BinList implementation based on a hash.
 * Good for sparse bin lists.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2015
 */
public class HashBinList implements BinList {

    private final long size_;
    private final Combiner combiner_;
    private final boolean isCopyResult_;
    private final Map<Long,Combiner.Container> map_;

    /**
     * Constructor.
     * The <code>isCopyResult</code> flag determines how the
     * {@link #getResult} method is implemented.
     * As a rule it should be true if an accumulating bin requires
     * more than a <code>double</code>'s worth of storage,
     * and false otherwise.
     *
     * @param  size  number of bins
     * @param  combiner  combiner
     * @param  isCopyResult  true if getResult copies data to a new array,
     *                       false if it acts as an adapter on existing data
     */
    public HashBinList( long size, Combiner combiner, boolean isCopyResult ) {
        size_ = size;
        combiner_ = combiner;
        isCopyResult_ = isCopyResult;
        map_ = new HashMap<Long,Combiner.Container>();
    }

    public long getSize() {
        return size_;
    }

    public Combiner getCombiner() {
        return combiner_;
    }

    public void submitToBin( long index, double value ) {
        Long key = new Long( index );
        Combiner.Container container = map_.get( key );
        if ( container == null ) {
            container = getCombiner().createContainer();
            map_.put( key, container );
        }
        container.submit( value );
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
                Combiner.Container container = map_.get( new Long( index ) );
                return container == null ? Double.NaN : container.getResult();
            }
            public double[] getValueBounds() {
                double lo = Double.POSITIVE_INFINITY;
                double hi = Double.NEGATIVE_INFINITY;
                for ( Combiner.Container container : map_.values() ) {
                    double v = container.getResult();
                    assert ! Double.isNaN( v );
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
        final Map<Long,Double> resultMap = new HashMap<Long,Double>();
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for ( Map.Entry<Long,Combiner.Container> entry : map_.entrySet() ) {
            Long key = entry.getKey();
            double value = entry.getValue().getResult();
            if ( ! Double.isNaN( value ) ) {
                if ( value < lo ) {
                    lo = value;
                }
                if ( value > hi ) {
                    hi = value;
                }
                resultMap.put( key, new Double( value ) );
            }
        }
        final double[] bounds = lo <= hi ? new double[] { lo, hi }
                                         : new double[] { 0, 1 };
        return new Result() {
            public double getBinValue( long index ) {
                Double value = resultMap.get( new Long( index ) );
                return value == null ? Double.NaN : value.doubleValue();
            }
            public double[] getValueBounds() {
                return bounds.clone();
            }
        };
    }
}
