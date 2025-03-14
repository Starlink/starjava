package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import uk.ac.starlink.ttools.plot2.Scale;

/**
 * Data model for a one-dimensional histogram.
 * Linear and logarithmic axis scaling are supported.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
public class BinBag {

    private final Scale scale_;
    private final double binWidth_;
    private final double binPhase_;
    private final Combiner combiner_;
    private final BinMapper mapper_;
    private final Map<Integer,Combiner.Container> valueMap_;

    /**
     * Constructor.
     *
     * <p>The <code>point</code> parameter is used internally to determine
     * the zero point of the bins.  In principle this should make no
     * difference to behaviour, but in case that the data is situated
     * a very long way from 1,  setting it close to
     * the actual data point locations may avoid rounding errors.
     *
     * @param   scale      axis scaling
     * @param   binWidth   width of each bin in scale units
     * @param   binPhase   determines sub-bin boundary shifts along axis,
     *                     normally in range 0..1
     * @param   combiner   aggregation mode
     * @param   point   representative point on axis near which bins are
     *                  situated
     */
    public BinBag( Scale scale, double binWidth, double binPhase,
                   Combiner combiner, double point ) {
        scale_ = scale;
        binWidth_ = binWidth;
        binPhase_ = binPhase;
        combiner_ = combiner;
        mapper_ = new BinMapper( scale, binWidth, binPhase, point );
        valueMap_ = new HashMap<Integer,Combiner.Container>();
    }

    /**
     * Submits a value for aggregation in the bin in which a given point falls.
     * Checking is performed; if the point is unsuitable
     * (for instance infinite) it will be ignored.
     *
     * @param  point  axis coordinate
     * @param  datum   value to aggregate into bin value
     */
    public void submitToBin( double point, double datum ) {
        if ( ! Double.isNaN( point ) && ! Double.isInfinite( point ) &&
             ( ( ! scale_.isPositiveDefinite() ) || point > 0 ) ) {
            int ix = mapper_.getBinIndex( point );
            Combiner.Container val = valueMap_.get( ix );
            if ( val == null ) {
                val = combiner_.createContainer();
                valueMap_.put( ix, val );
            }
            val.submit( datum );
        }
    }

    /**
     * Returns a sorted iterator over all bins with non-zero values
     * in the range over which samples were presented.
     *
     * @param   cumul  flag for bins of a cumulative histogram
     * @param   norm  normalisation mode
     * @param   unit  axis unit scaling
     * @return  sorted iterator over bins
     */
    public Iterator<Bin> binIterator( Cumulation cumul, Normalisation norm,
                                      Unit unit ) {
        return binIterator( cumul, norm, unit, null );
    }

    /**
     * Returns a sorted iterator over all bins with non-zero values
     * in the range over which samples were presented, perhaps
     * extended over a given range.
     *
     * <p>The purpose of the supplied extension range is to extend the number
     * of bins returned, specifically for the purpose of cumulative histograms,
     * where bins outside the range of the presented samples can have
     * non-zero values.  At present the range is not used to cut down
     * the number of bins returned; that could be done to improve efficiency,
     * though the effect is not likely to be large since bin counts are
     * usually fairly modest.
     *
     * @param   cumul  flag for bins of a cumulative histogram
     * @param   norm  normalisation mode
     * @param   unit  axis unit scaling
     * @param   range  required range for extending data
     * @return  sorted iterator over bins
     */
    public Iterator<Bin> binIterator( Cumulation cumul, Normalisation norm,
                                      Unit unit, double[] range ) {

        /* Avoid some edge cases by returning an empty iterator immediately
         * in case of no bins. */
        if ( valueMap_.isEmpty() ) {
            return new ArrayList<Bin>().iterator();
        }

        /* Prepare an integer array giving bin indices for all the non-empty
         * bins accumulated by this object. */
        final int nbin = valueMap_.size();
        final int[] binIndices = new int[ nbin ];
        {
            int ib = 0;
            for ( Integer index : valueMap_.keySet() ) {
                binIndices[ ib++ ] = index;
            }
            assert ib == nbin;
        }

        /* Sort it in ascending order of bin index, which is also ascending
         * order of bin axis position. */
        Arrays.sort( binIndices );

        /* Prepare a double array of bin values corresponding to the
         * bin index array.  At the same time accumulate the running total
         * and adjust the values to be cumulative if so requested. */
        final double[] binValues = new double[ nbin ];
        double total = 0;
        double max = 0;
        for ( int ib = 0; ib < nbin; ib++ ) {
            int jb = cumul.isReverse() ? nbin - ib - 1 : ib;
            double value = valueMap_.get( binIndices[ jb ] ).getCombinedValue();
            final double bv;
            switch ( cumul ) {
                case NONE:
                   bv = value;
                   break;
                case FORWARD:
                   bv = total + value;
                   break;
                case REVERSE:
                   bv = total;
                   break;
                default:
                   throw new AssertionError();
            }
            binValues[ jb ] = bv;
            total += value;
            max = Math.max( max, Math.abs( value ) );
        }

        /* Normalise. */
        double bw = binWidth_ / unit.getExtent();
        double scale = norm.getScaleFactor( total, max, bw, combiner_.getType(),
                                            cumul.isCumulative() );
        if ( scale != 1.0 ) {
            for ( int ib = 0; ib < nbin; ib++ ) {
                binValues[ ib ] *= scale;
            }
        }
        final double stotal = scale * total;

        /* For a cumulative result, bins which never had values accumulated
         * into them, and hence did not appear in the bin map,
         * will (probably) have non-zero values that must be returned.
         * Step up from the lowest to highest known bin value in steps of 1. */
        if ( cumul.isCumulative() ) {
            int ixlo = binIndices[ 0 ];
            int ixhi = binIndices[ nbin - 1 ];

            /* If requested, extend the range so that maximum-value bins
             * go all the way to the edge of the plot region. */
            if ( range != null ) {
                ixlo = Math.min( ixlo, mapper_.getBinIndex( range[ 0 ] ) - 1 );
                ixhi = Math.max( ixhi, mapper_.getBinIndex( range[ 1 ] ) + 1 );
            }
            final int ixMin = ixlo;
            final int ixMax = ixhi;
            return new Iterator<Bin>() {
                int index = ixMin;
                int ib = index < binIndices[ 0 ] ? -1 : 0;
                public boolean hasNext() {
                    return index < ixMax;
                }
                public Bin next() {
                    final double value;
                    if ( ib < 0 ) {
                        value = cumul.isReverse() ? stotal : 0;
                    }
                    else if ( ib >= nbin ) {
                        value = cumul.isReverse() ? 0 : binValues[ nbin - 1 ];
                    }
                    else {
                        value = binValues[ ib ];
                    }
                    index++;
                    if ( ib == nbin - 1 ||
                         ib < nbin -1 && index == binIndices[ ib + 1 ] ) {
                        ib++;
                    }
                    return createBin( index, value );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /* For a non-cumulative result, we just need to return one bin for
         * each map entry. */
        else {
            return new Iterator<Bin>() {
                int ib = 0;
                public boolean hasNext() {
                    return ib < nbin;
                }
                public Bin next() {
                    if ( ib < nbin ) {
                        Bin bin = createBin( binIndices[ ib ],
                                             binValues[ ib ] );
                        ib++;
                        return bin;
                    }
                    else {
                        throw new NoSuchElementException();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Iterates over all the bins defined by this bin bag in a given
     * data interval.  The contents of each bin, if any, are irrelevant
     * to this operation.
     *
     * @param   lo   lower bound of interest
     * @param   hi   upper bound of interest
     * @return   iterator in sequence over 2-element (low,high) bin range
     *           arrays that together cover the supplied (lo,hi) range
     */
    public Iterator<double[]> barIterator( double lo, double hi ) {
        final int ibin0 = mapper_.getBinIndex( lo );
        final int ibin1 = mapper_.getBinIndex( hi );
        return new Iterator<double[]>() {
            int ib = ibin0;
            public boolean hasNext() {
                return ib <= ibin1;
            }
            public double[] next() {
                if ( ib <= ibin1 ) {
                    return mapper_.getBinLimits( ib++ );
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns the bin width in scale units used by this histogram model.
     *
     * @return  bin width in scale units
     */
    public double getBinWidth() {
        return binWidth_;
    }

    /**
     * Returns this bin bag's combiner.
     *
     * @return   combination mode
     */
    public Combiner getCombiner() {
        return combiner_;
    }

    /**
     * Returns the number of non-empty bins in this bag.
     *
     * @return   bin count
     */
    public int getBinCount() {
        return valueMap_.size();
    }

    /**
     * Adds the contents of another compatible BinBag to this one.
     * The effect is as if all the data submitted to the other bag
     * had been submitted to this one as well.
     * The effect on the supplied <code>other</code> is undefined.
     *
     * @param   other   compatible accumulator
     */
    public void add( BinBag other ) {
        for ( Map.Entry<Integer,Combiner.Container> entry :
              other.valueMap_.entrySet() ) {
            Integer key = entry.getKey();
            Combiner.Container otherContainer = entry.getValue();
            Combiner.Container thisContainer = valueMap_.get( key );
            if ( thisContainer == null ) {
                valueMap_.put( key, otherContainer );
            }
            else {
                thisContainer.add( otherContainer );
            }
        }
    }

    /**
     * Indicates whether the bin boundaries and aggregation mode
     * used by this object are the same as a given bin set specification.
     *
     * @param   scale      axis scaling
     * @param   binWidth   width of each bin; this is additive for linear
     *                     and multiplicative for logarithmic scaling
     * @param   binPhase   determines sub-bin boundary shifts along axis
     *                     normally in range 0..1
     * @param   combiner   aggregation mode
     * @return  true iff a BinBag constructed using the given parameters
     *          would have the same behaviour as this one
     */
    public boolean matches( Scale scale, double binWidth, double binPhase,
                            Combiner combiner ) {
        return scale.equals( scale_ )
            && binWidth == binWidth_
            && binPhase == binPhase_
            && combiner.equals( combiner_ );
    }

    /**
     * Constructs a bin for this object.
     *
     * @param   binIndex  bin index relating to this bag's mapper
     * @param   binValue  value reported for the bin
     */
    private Bin createBin( int binIndex, final double binValue ) {
        final double[] limits = mapper_.getBinLimits( binIndex );
        return new Bin() {
            public double getXMin() {
                return limits[ 0 ];
            }
            public double getXMax() {
                return limits[ 1 ];
            }
            public double getY() {
                return binValue;
            }
            public String toString() {
                return limits[ 0 ] + ".." + limits[ 1 ] + ": " + binValue;
            }
        };
    }

    /**
     * Describes the extent of a bin and the value it contains.
     */
    public interface Bin {

        /**
         * Returns the lower bound of this bin.
         *
         * @return  axis minimum of bin
         */
        public double getXMin();

        /**
         * Returns the upper bound of this bin.
         *
         * @return  axis maximum of bin
         */
        public double getXMax();

        /**
         * Returns the value so far accumulated into this bin.
         *
         * @return   bin value
         */
        public double getY();
    }
}
