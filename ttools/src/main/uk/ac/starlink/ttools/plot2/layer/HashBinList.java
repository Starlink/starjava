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
    private final Map<Long,Combiner.Container> map_;

    /**
     * Constructor.
     *
     * @param  size  number of bins
     * @param  combiner  combiner
     */
    public HashBinList( long size, Combiner combiner ) {
        size_ = size;
        combiner_ = combiner;
        map_ = new HashMap<Long,Combiner.Container>();
    }

    public long getSize() {
        return size_;
    }

    public Combiner getCombiner() {
        return combiner_;
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
