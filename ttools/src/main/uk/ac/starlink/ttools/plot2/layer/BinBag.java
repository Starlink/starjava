package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Data model for a one-dimensional histogram.
 * Linear and logarithmic axis scaling are supported.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
public class BinBag {

    private final boolean log_;
    private final double binWidth_;
    private final double binPhase_;
    private final BinMapper mapper_;
    private final Map<Integer,Value> valueMap_;

    /**
     * Constructor.
     * Notional bin boundaries are for <code>log=false</code>:
     * <pre>
     *   binWidth*(0+binPhase), binWidth*(1+binPhase), ...
     * </pre>
     * and for <code>log=true</code>:
     * <pre>
     *   binWidth**(0+binPhase), binWidth**(1+binPhase), ...
     * </pre>
     *
     * <p>The <code>point</code> parameter is used internally to determine
     * the zero point of the bins.  In principle this should make no
     * difference to behaviour, but in case that the data is situated
     * a very long way from 1,  setting it close to
     * the actual data point locations may avoid rounding errors.
     *
     * @param   log   false for linear axis scaling, true for logarithmic
     * @param   binWidth   width of each bin; this is additive for linear
     *                     and multiplicative for logarithmic scaling
     * @param   binPhase   determines sub-bin boundary shifts along axis,
     *                     normally in range 0..1
     * @param   point   representative point on axis near which bins are
     *                  situated
     */
    public BinBag( boolean log, double binWidth, double binPhase,
                   double point ) {
        log_ = log;
        binWidth_ = binWidth;
        binPhase_ = binPhase;
        double ref = getRef( log, binWidth, binPhase, point );
        mapper_ = log ? new LogBinMapper( binWidth, ref )
                      : new LinearBinMapper( binWidth, ref );
        valueMap_ = new HashMap<Integer,Value>();
    }

    /**
     * Adds a value to the bin in which a given point falls.
     * Checking is performed; if the value is unsuitable
     * (for instance infinite) it will be ignored.
     *
     * @param  point  axis coordinate
     * @param  inc   value to accumulate onto bin value
     */
    public void addToBin( double point, double inc ) {
        if ( ! Double.isNaN( point ) && ! Double.isInfinite( point ) &&
             ( ( ! log_ ) || point > 0 ) ) {
            int ix = mapper_.getBinIndex( point );
            Value val = valueMap_.get( ix );
            if ( val == null ) {
                val = new Value();
                valueMap_.put( ix, val );
            }
            val.value_ += inc;
        }
    }

    /**
     * Returns a sorted iterator over all bins with non-zero values.
     *
     * @param   cumulative  true for bins of a cumulative histogram
     * @param   normalised  true for bins of a normalised histogram (sum to 1)
     * @return  sorted iterator over bins
     */
    public Iterator<Bin> binIterator( boolean cumulative, boolean normalised ) {

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
        for ( int ib = 0; ib < nbin; ib++ ) {
            double value = valueMap_.get( binIndices[ ib ] ).value_;
            binValues[ ib ] = cumulative ? total + value : value;
            total += value;
        }

        /* If normalised values are requested rescale them using the
         * final total.  In the cumulative case fix it so that the
         * final value is normalised to 1, and in the non-cumulative
         * case so that the area covered by the bars is 1. */
        if ( normalised ) {
            double scale1 = total;
            if ( ! cumulative ) {
                scale1 *= log_ ? Math.log( binWidth_ ) : binWidth_;
            }
            double scale = 1.0 / scale1;
            for ( int ib = 0; ib < nbin; ib++ ) {
                binValues[ ib ] *= scale;
            }
        }

        /* For a cumulative result, bins which never had values accumulated
         * into them, and hence did not appear in the bin map,
         * will (probably) have non-zero values that must be returned.
         * Step up from the lowest to highest known bin value in steps of 1. */
        if ( cumulative ) {
            return new Iterator<Bin>() {
                int ib = 0;
                int index = binIndices[ 0 ];
                public boolean hasNext() {
                    return ib < nbin;
                }
                public Bin next() {
                    if ( ib < nbin ) {
                        Bin bin = createBin( index++, binValues[ ib ] );
                        if ( index >= binIndices[ ib ] ) {
                            ib++;
                        }
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
     * Indicates whether the bin boundaries used by this object are the
     * same as a given bin set specification.
     *
     * @param   log  false for linear scaling, true for logarithmic
     * @param   binWidth   width of each bin; this is additive for linear
     *                     and multiplicative for logarithmic scaling
     * @param   binPhase   determines sub-bin boundary shifts along axis
     *                     normally in range 0..1
     * @return  true iff a BinBag constructed using the given parameters
     *          would have the same bin boundaries as this one
     */
    public boolean matches( boolean log, double binWidth, double binPhase ) {
        return log == log_
            && binWidth == binWidth_
            && binPhase == binPhase_;
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
     * Determines a bin base reference point with a phase equivalent to
     * the given one, but which is close to a supplied point.
     *
     * @param  logFlag  false for linear scaling, true for logarithmic
     * @param  width    bin width
     * @param  phase    bin phase
     * @param  point    representative point
     * @return   axis position of a bin boundary
     *           for the given <code>width</code> and <code>phase</code>,
     *           but close to <code>point</code>
     */
    private static double getRef( boolean logFlag, double width, double phase,
                                  double point ) {
       phase = phase % 1;
       if ( phase < 0 ) {
           phase += 1;
       }
       assert phase >= 0 && phase < 1 : phase;
       if ( logFlag ) {
           if ( point <= 0 ) {
               point = 1;
           }
           int n = (int) Math.floor( Math.log( point ) / Math.log( width ) );
           double ref = Math.pow( width, n + phase );
           assert Math.abs( Math.log( ref / point ) ) <= width;
           return ref;
       }
       else {
           int n = (int) Math.floor( point / width );
           double ref = ( n + phase ) * width;
           assert Math.abs( ref - point ) <= width;
           return ref;
       }
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

    /**
     * Maps axis values to bin indices.
     */
    private interface BinMapper {

        /**
         * Returns the bin index for a given value.
         *
         * @param   value  axis value
         * @return  bin index
         */
        int getBinIndex( double value );

        /**
         * Returns the bin limits for a given bin index.
         *
         * @param   index  bin index
         * @return   (lower,upper) bin limits
         */
        double[] getBinLimits( int index );
    }

    /**
     * BinMapper implementation for linear axis scaling.
     */
    private static class LinearBinMapper implements BinMapper {
        private final double width_;
        private final double width1_;
        private final double floor_;

        /**
         * Constructor.
         *
         * @param  width  additive bin width
         * @param  floor  lower bound of bin zero
         */
        LinearBinMapper( double width, double floor ) {
            width_ = width;
            width1_ = 1.0 / width;
            floor_ = floor;
        }

        public int getBinIndex( double value ) {
            return (int) Math.floor( ( value - floor_ ) * width1_ );
        }

        public double[] getBinLimits( int index ) {
            double lo = floor_ + index * width_;
            return new double[] { lo, lo + width_ };
        }
    }

    /**
     * BinMapper implementation for logarithmic axis scaling.
     */
    private static class LogBinMapper implements BinMapper {
        private final double width_;
        private final double floor_;
        private final double logWidth1_;
        private final double floor1_;

        /**
         * Constructor.
         *
         * @param  width  multiplicative bin width
         * @param  floor  lower bound of bin zero
         */
        LogBinMapper( double width, double floor ) {
            width_ = width;
            floor_ = floor;
            logWidth1_ = 1. / Math.log( width );
            floor1_ = 1. / floor;
        }

        public int getBinIndex( double value ) {
            return (int) Math.floor( Math.log( value * floor1_ ) * logWidth1_ );
        }

        public double[] getBinLimits( int index ) {
            double lo = floor_ * Math.pow( width_, index );
            return new double[] { lo, lo * width_ };
        }
    }

    /**
     * Holds a single mutable double value.  Used for accumulating bin sums.
     */
    private static class Value {
        double value_;
    }
}
