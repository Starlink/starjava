package uk.ac.starlink.topcat.plot;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;

/**
 * BinnedData implementation that uses a map.
 * Bins are dispensed from the iterator in order. 
 *
 * @author   Mark Taylor
 * @since    14 Nov 2005
 */
public class MapBinnedData implements BinnedData {

    /**
     * Map containing the binned data.
     * The keys are objects managed by the mapper.  The values are int[] 
     * arrays giving the bin occupancy counts indexed by subset index.
     */
    private final SortedMap map_;

    private final int nset_;
    private final BinMapper mapper_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructs a new BinnedData.
     *
     * @param  nset  the number of subsets that this BinnedData can deal with
     * @param  mapper  a BinMapper implementation that defines the bin ranges
     */
    public MapBinnedData( int nset, BinMapper mapper ) {
        nset_ = nset;
        mapper_ = mapper;
        map_ = new TreeMap();
    }

    public void submitDatum( double value, boolean[] setFlags ) {

        /* If it's a NaN, this implementation will end up binning it with
         * zeros, so just ignore it.  You could do something sensible 
         * with NaNs, but I've encountered terrible trouble with a 
         * Java 1.4.1 JIT bug (?) which seems to have had something to 
         * do with NaN processing, so be very careful if you try to
         * implement that. */
        if ( Double.isNaN( value ) ) {
            return;
        }

        /* Normal value, go ahead. */
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
        Comparable getKey( double value );

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
            if ( binWidth <= 0 || Double.isNaN( binWidth ) ) {
                throw new IllegalArgumentException( "Bad width " + binWidth );
            }
            width_ = binWidth;
        }
        public Comparable getKey( double value ) {
            return new Long( (long) Math.floor( value / width_ ) );
        }
        public double[] getBounds( Object key ) {
            final long keyval = ((Long) key).longValue();
            final double centre = keyval * width_;

            /* This nonsensical looking test is here as debugging code for
             * a diabolical JVM bug which was plaguing this code at one
             * stage; it did sometimes log the warning at java 1.4.1 
             * using the default (client) hotspot compiler.  
             * I've modified the code elsewhere to do more explicit NaN
             * testing upstream of this method and it seems to have gone
             * away now, but leave the test here in case it rears its
             * ugly head again. */
            if ( Double.isNaN( centre ) ) {
                if ( ! Double.isNaN( centre ) ) {
                    logger_.warning( "Monstrous Java 1.4.1 JVM bug" );
                }
            }

            return new double[] { centre, centre + width_ };
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
        public Comparable getKey( double value ) {
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
