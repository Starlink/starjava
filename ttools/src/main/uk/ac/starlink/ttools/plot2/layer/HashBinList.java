package uk.ac.starlink.ttools.plot2.layer;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
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

    public void submitToBin( long index, double value ) {
        Long key = Long.valueOf( index );
        Combiner.Container container = map_.get( key );
        if ( container == null ) {
            container = getCombiner().createContainer();
            map_.put( key, container );
        }
        container.submit( value );
    }

    public Combiner.Container getBinContainer( long index ) {
        return map_.get( Long.valueOf( index ) );
    }

    /**
     * Accumulates all the data from another BinList into this one.
     * The effect is the same as if all the data submitted to <code>other</code>
     * had been submitted to this.
     *
     * <p>The other list must be of the same type (have the same combiner)
     * as this one.
     *
     * @param  other   second BinList compatible with this one
     * @throws   ClassCastException   if <code>other</code>'s type
     *                                does not match this one
     */
    public void addBins( BinList other ) {
        for ( Iterator<Long> it = other.getResult().indexIterator();
              it.hasNext(); ) {
            Long key = it.next();
            Combiner.Container container1 =
                other.getBinContainer( key.longValue() );
            Combiner.Container container0 = map_.get( key );
            if ( container0 == null ) {
                map_.put( key, container1 );
            }
            else {
                container0.add( container1 );
            }
        }
    }

    public Result getResult() {
        return new Result() {
            public double getBinValue( long index ) {
                Combiner.Container container =
                    map_.get( Long.valueOf( index ) );
                return container == null ? Double.NaN
                                         : container.getCombinedValue();
            }
            public long getBinCount() {
                return map_.size();
            }
            public Iterator<Long> indexIterator() {
                return map_.keySet().iterator();
            }
            public Result compact() {
                double frac = map_.size() * 1.0 / size_;
                if ( frac > 0.25 && size_ < Integer.MAX_VALUE ) {
                    int isize = (int) size_;
                    double[] values = new double[ isize ];
                    BitSet mask = new BitSet( isize );
                    for ( Iterator<Long> it = indexIterator(); it.hasNext(); ) {
                        int index = it.next().intValue();
                        mask.set( index );
                        values[ index ] = getBinValue( index );
                    }
                    return ArrayBinList.createDoubleMaskResult( mask, values );
                }
                else if ( combiner_.hasBigBin() ) {
                    Map<Long,Double> cmap = new HashMap<Long,Double>();
                    for ( Iterator<Long> it = indexIterator(); it.hasNext(); ) {
                        Long key = it.next();
                        Combiner.Container container = map_.get( key );
                        double val = container == null
                                   ? Double.NaN
                                   : container.getCombinedValue();
                        cmap.put( key, Double.valueOf( val ) );
                    }
                    return createHashResult( cmap );
                }
                else {
                    return this;
                }
            }
        };
    }

    /**
     * Returns the hash used to store this bin list's state.
     *
     * @return  index-&gt;container map
     */
    public Map<Long,Combiner.Container> getMap() {
        return map_;
    }

    /**
     * Returns a new Result instance based on a Map.
     *
     * @param  map   map of values
     * @return  result based on <code>map</code>
     */
    public static Result createHashResult( final Map<Long,Double> map ) { 
        return new Result() {
            public double getBinValue( long index ) {
                Double value = map.get( Long.valueOf( index ) );
                return value == null ? Double.NaN : value.doubleValue();
            }
            public long getBinCount() {
                return map.size();
            }
            public Iterator<Long> indexIterator() {
                return map.keySet().iterator();
            }
            public Result compact() {
                return this;
            }
        };
    }
}
