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
        Long key = new Long( index );
        Combiner.Container container = map_.get( key );
        if ( container == null ) {
            container = getCombiner().createContainer();
            map_.put( key, container );
        }
        container.submit( value );
    }

    public Result getResult() {
        return new Result() {
            public double getBinValue( long index ) {
                Combiner.Container container = map_.get( new Long( index ) );
                return container == null ? Double.NaN : container.getResult();
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
                    BitSet mask = new BitSet( (int) isize );
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
                        double val = container == null ? Double.NaN
                                                       : container.getResult();
                        cmap.put( key, new Double( val ) );
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
     * Returns a new Result instance based on a Map.
     *
     * @param  map   map of values
     * @return  result based on <code>map</code>
     */
    public static Result createHashResult( final Map<Long,Double> map ) { 
        return new Result() {
            public double getBinValue( long index ) {
                Double value = map.get( new Long( index ) );
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
