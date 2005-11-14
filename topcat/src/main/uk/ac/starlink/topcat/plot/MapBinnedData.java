package uk.ac.starlink.topcat.plot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * BinnedData implementation that uses a map.
 *
 * @author   Mark Taylor
 * @since    14 Nov 2005
 */
public class MapBinnedData implements BinnedData {

    private final int nset_;
    private final BinMapper mapper_;

    /**
     * Map containing the binned data.
     * The keys are Longs which, when multiplied by binWidth_, give the bin
     * central value.  The values are int[] arrays giving the bin occupancy
     * counts indexed by subset index.
     */
    private final Map map_;

    /**
     * Constructs a new BinnedData.
     *
     * @param  nset  the number of subsets that this BinnedData can deal with
     * @param  mapper  a BinMapper implementation that defines the bin ranges
     */
    public MapBinnedData( int nset, BinMapper mapper ) {
        nset_ = nset;
        mapper_ = mapper;
        map_ = new HashMap();
    }

    public void submitDatum( double value, boolean[] setFlags ) {
        Object key = mapper_.getKey( value );
        if ( key != null ) {
            int[] counts = (int[]) map_.get( key );
            if ( counts == null ) {
                counts = new int[ nset_ ];
                map_.put( key, counts );
            }
            for ( int is = 0; is < nset_; is++ ) {
                if ( setFlags[ is ] ) {
                    counts[ is ]++;
                }
            }
        }
    }

    public Iterator getBinIterator() {
        final Iterator entryIt = map_.entrySet().iterator();
        return new Iterator() {
            public boolean hasNext() {
                return entryIt.hasNext();
            }
            public Object next() {
                Map.Entry entry = (Map.Entry) entryIt.next();
                Object key = entry.getKey();
                final int[] counts = (int[]) entry.getValue();
                final double[] bounds = mapper_.getBounds( key );
                return new Bin() {
                    public double getLowBound() {
                        return bounds[ 0 ];
                    }
                    public double getHighBound() {
                        return bounds[ 1 ];
                    }
                    public int getCount( int iset ) {
                        return counts[ iset ];
                    }
                };
            }
            public void remove() {
                entryIt.remove();
            }
        };
    }

    /**
     * Constructs a new BinnedData with linearly spaced bins.
     *
     * @param  nset  number of subsets
     * @param  binWidth  bin spacing
     * @return   new BinnedData object
     */
    public static MapBinnedData createLinearBinnedData( int nset,
                                                        double binWidth ) {
        return new MapBinnedData( nset, new LinearBinMapper( binWidth ) );
    }

    /**
     * Constructs a new BinnedData with logarithmically spaced bins.
     *
     * @param  nset  number of subsets
     * @param  binFactor   logarithmic spacing of bins
     * @return  new BinnedData object
     */
    public static MapBinnedData createLogBinnedData( int nset,
                                                     double binFactor ) {
        return new MapBinnedData( nset, new LogBinMapper( binFactor ) );
    }

    /**
     * Defines the mapping of numerical values to map keys.
     * The keys must implement <code>equals</code> and <code>hashCode</code>
     * properly.
     */
    public interface BinMapper {

        /**
         * Returns the key to use for a given value.  
         * May return <code>null</code> to indicate that the given value
         * cannot be binned.
         *
         * @param  value  numerical value
         * @return   object to be used as a key for the bin into which 
         *           <code>value</code> falls
         */
        Object getKey( double value );

        /**
         * Returns the upper and lower bounds of the bin corresponding to
         * a given key.
         *
         * @param   key  bin key object
         * @return   2-element array giving (lower,upper) bound for
         *           bin <code>key</code>
         */
        double[] getBounds( Object key );
    }

    /**
     * Linear scaled implementation of BinMapper.
     */
    private static class LinearBinMapper implements BinMapper {
        final double width_;
        LinearBinMapper( double binWidth ) {
            width_ = binWidth;
        }
        public Object getKey( double value ) {
            return new Long( Math.round( value / width_ ) );
        }
        public double[] getBounds( Object key ) {
            double centre = ((Long) key).longValue() * width_;
            return new double[] { centre - width_ * 0.5,
                                  centre + width_ * 0.5 };
        }
    }

    /**
     * Logarithmically scaled implementation of BinMapper.
     */
    private static class LogBinMapper implements BinMapper {
        final double factor_;
        final double logFactor_;
        final double sqrtFactor_;
        LogBinMapper( double factor ) {
            factor_ = factor;
            logFactor_ = Math.log( factor );
            sqrtFactor_ = Math.sqrt( factor );
        }
        public Object getKey( double value ) {
            return value > 0.0 
                 ? new Long( Math.round( Math.log( value ) / logFactor_ ) )
                 : null;
        }
        public double[] getBounds( Object key ) {
            double centre = Math.pow( factor_, ((Long) key).doubleValue() );
            return new double[] { centre / sqrtFactor_, centre * sqrtFactor_ };
        }
    }

}
