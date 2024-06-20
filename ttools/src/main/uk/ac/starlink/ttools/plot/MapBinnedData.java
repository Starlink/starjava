package uk.ac.starlink.ttools.plot;

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
public class MapBinnedData<K extends Comparable<K>> implements BinnedData {

    /**
     * Map containing the binned data.
     * The keys are objects managed by the mapper.  The values are double[] 
     * arrays giving the bin occupancy counts indexed by subset index.
     */
    private final SortedMap<K,double[]> map_;

    private final int nset_;
    private final BinMapper<K> mapper_;
    private boolean isFloat_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot" );

    /**
     * Constructs a new BinnedData.
     *
     * @param  nset  the number of subsets that this BinnedData can deal with
     * @param  mapper  a BinMapper implementation that defines the bin ranges
     */
    public MapBinnedData( int nset, BinMapper<K> mapper ) {
        nset_ = nset;
        mapper_ = mapper;
        map_ = new TreeMap<K,double[]>();
    }

    public void submitDatum( double value, double weight, boolean[] setFlags ) {

        /* If it's a NaN, this implementation will end up binning it with
         * zeros, so just ignore it.  You could do something sensible 
         * with NaNs, but I've encountered terrible trouble with a 
         * Java 1.4.1 JIT bug (?) which seems to have had something to 
         * do with NaN processing, so be very careful if you try to
         * implement that. */
        if ( Double.isNaN( value ) || Double.isNaN( weight ) || weight == 0 ) {
            return;
        }

        /* Update integer flag. */
        isFloat_ = isFloat_ || ( weight != (int) weight );

        /* Normal value, go ahead. */
        K key = mapper_.getKey( value );
        if ( key != null ) {
            double[] counts = map_.get( key );
            if ( counts == null ) {
                counts = new double[ nset_ ];
                map_.put( key, counts );
            }
            for ( int is = 0; is < nset_; is++ ) {
                if ( setFlags[ is ] ) {
                    counts[ is ] += weight;
                }
            }
        }
    }

    public Iterator<Bin> getBinIterator( boolean includeEmpty ) {
        final Iterator<K> keyIt = ( includeEmpty && ! map_.isEmpty() )
                                ? mapper_.keyIterator( map_.firstKey(),
                                                       map_.lastKey() )
                                : map_.keySet().iterator();
        return new Iterator<Bin>() {
            final double[] EMPTY_SUMS = new double[ nset_ ];
            public boolean hasNext() {
                return keyIt.hasNext();
            }
            public Bin next() {
                K key = keyIt.next();
                final double[] sums = map_.containsKey( key )
                                    ? map_.get( key )
                                    : EMPTY_SUMS; 
                final double[] bounds = mapper_.getBounds( key );
                return new Bin() {
                    public double getLowBound() {
                        return bounds[ 0 ];
                    }
                    public double getHighBound() {
                        return bounds[ 1 ];
                    }
                    public double getWeightedCount( int iset ) {
                        return sums[ iset ];
                    }
                };
            }
            public void remove() {
                keyIt.remove();
            }
        };
    }

    public int getSetCount() {
        return nset_;
    }

    public boolean isInteger() {
        return ! isFloat_;
    }

    /**
     * Returns the BinMapper object used by this BinnedData.
     *
     * @return bin mapper
     */
    public BinMapper<K> getMapper() {
        return mapper_;
    }

    /**
     * Constructs a new linear or logarithmic BinMapper object.
     *
     * @param  logFlag  false for linear spacing, true for logarithmic
     * @param  binWidth  bin spacing
     *                   (additive for linear, multiplicative for logarithmic)
     * @param  binBase   lower bound of one (any) bin; determines bin phase
     * @return   new bin mapper
     */
    public static BinMapper<Long> createBinMapper( boolean logFlag,
                                                   double binWidth,
                                                   double binBase ) {
        return logFlag ? new LogBinMapper( binWidth, binBase )
                       : new LinearBinMapper( binWidth, binBase );
    }

    /**
     * Defines the mapping of numerical values to map keys.
     * The keys must implement <code>equals</code> and <code>hashCode</code>
     * properly.
     */
    public interface BinMapper<K extends Comparable<K>> {

        /**
         * Returns the key to use for a given value.
         * May return <code>null</code> to indicate that the given value
         * cannot be binned.
         *
         * @param  value  numerical value
         * @return   object to be used as a key for the bin into which
         *           <code>value</code> falls
         */
        K getKey( double value );

        /**
         * Returns the upper and lower bounds of the bin corresponding to
         * a given key.
         *
         * @param   key  bin key object
         * @return   2-element array giving (lower,upper) bound for
         *           bin <code>key</code>
         */
        double[] getBounds( K key );

        /**
         * Returns an iterator which covers all keys between the given
         * low and high keys inclusive.
         * <code>loKey</code> and <code>hiKey</code> must be possible keys
         * for this mapper and arranged in the right order.
         *
         * @param  loKey  lower bound (inclusive) for key iteration
         * @param  hiKey  upper bound (inclusive) for key iteration
         * @return  iterator
         */
        Iterator<K> keyIterator( K loKey, K hiKey );

        /**
         * Creates a BinnedData instance based on this mapper.
         *
         * @param  nset  the number of subsets that the BinnedData can deal with
         * @return  binned data instance
         */
        MapBinnedData<K> createBinnedData( int nset );
    }

    /**
     * Linear scaled implementation of BinMapper.
     */
    private static class LinearBinMapper implements BinMapper<Long> {
        final double width_;
        final double base_;

        /**
         * Constructs a new linear mapper.
         *
         * @param  binWidth  width of the bins
         * @param  binBase   lower bound of any one of the bins
         *                   (determines bin phase)
         */
        LinearBinMapper( double binWidth, double binBase ) {
            if ( binWidth <= 0 || Double.isNaN( binWidth ) ) {
                throw new IllegalArgumentException( "Bad width " + binWidth );
            }
            width_ = binWidth;
            base_ = binBase;
        }
        public Long getKey( double value ) {
            return Long.valueOf( (long)
                                 Math.floor( ( value - base_ ) / width_ ) );
        }
        public double[] getBounds( Long key ) {
            final long keyval = key.longValue();
            final double bottom = keyval * width_ + base_;

            /* This nonsensical looking test is here as debugging code for
             * a diabolical JVM bug which was plaguing this code at one
             * stage; it did sometimes log the warning at java 1.4.1 
             * using the default (client) hotspot compiler.  
             * I've modified the code elsewhere to do more explicit NaN
             * testing upstream of this method and it seems to have gone
             * away now, but leave the test here in case it rears its
             * ugly head again. */
            if ( Double.isNaN( bottom ) ) {
                if ( ! Double.isNaN( bottom ) ) {
                    logger_.warning( "Monstrous Java 1.4.1 JVM bug" );
                }
            }

            return new double[] { bottom, bottom + width_ };
        }
        public Iterator<Long> keyIterator( final Long loKey,
                                           final Long hiKey ) {
            return new Iterator<Long>() {
                final long hiVal_ = hiKey.longValue();
                long val_ = loKey.longValue();
                public boolean hasNext() {
                    return val_ <= hiVal_;
                }
                public Long next() {
                    return Long.valueOf( val_++ );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        public MapBinnedData<Long> createBinnedData( int nset ) {
            return new MapBinnedData<Long>( nset, this );
        }
    }

    /**
     * Logarithmically scaled implementation of BinMapper.
     */
    private static class LogBinMapper implements BinMapper<Long> {
        final double factor_;
        final double base_;
        final double logFactor_;
        LogBinMapper( double factor, double base ) {
            factor_ = factor;
            base_ = base > 0 ? base : 1.0;
            logFactor_ = Math.log( factor );
        }
        public Long getKey( double value ) {
            return value > 0.0
                 ? Long.valueOf( (long) Math.floor( ( Math.log( value / base_ )
                                                    / logFactor_ ) ) )
                 : null;
        }
        public double[] getBounds( Long key ) {
            double lo = Math.pow( factor_, key.doubleValue() ) * base_;
            return new double[] { lo, lo * factor_ };
        }
        public Iterator<Long> keyIterator( final Long loKey,
                                           final Long hiKey ) {
            return new Iterator<Long>() {
                final long hiVal_ = hiKey.longValue();
                long val_ = loKey.longValue();
                public boolean hasNext() {
                    return val_ <= hiVal_;
                }
                public Long next() {
                    return Long.valueOf( val_++ );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        public MapBinnedData<Long> createBinnedData( int nset ) {
            return new MapBinnedData<Long>( nset, this );
        }
    }
}
