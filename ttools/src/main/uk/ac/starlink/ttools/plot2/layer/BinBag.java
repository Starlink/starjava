package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<Integer,Bin> binMap_;

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
        binMap_ = new HashMap<Integer,Bin>();
    }

    /**
     * Adds a value to the bin in which a given point falls.
     *
     * @param  point  axis coordinate
     * @param  inc   value to accumulate onto bin value
     */
    public void addToBin( double point, double inc ) {
        if ( ! Double.isNaN( point ) && ! Double.isInfinite( point ) ) {
            int ix = mapper_.getBinIndex( point );
            Bin bin = binMap_.get( ix );
            if ( bin == null ) {
                double[] limits = mapper_.getBinLimits( ix );
                bin = new Bin( limits[ 0 ], limits[ 1 ] );
                binMap_.put( ix, bin );
            }
            bin.add( inc );
        }
    }

    /**
     * Returns a sorted collection of all bins which have had values
     * added to them.
     *
     * @return  sorted list of bins
     */
    public Collection<Bin> getBins() {
        List<Bin> list = new ArrayList<Bin>( binMap_.values() );
        Collections.sort( list, new Comparator<Bin>() {
            public int compare( Bin b1, Bin b2 ) {
                return (int) Math.signum( b1.xmin_ - b2.xmin_ );
            }
        } );
        return list;
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
    public static class Bin {
        private final double xmin_;
        private final double xmax_;
        private double value_;

        /**
         * Constructor.
         *
         * @param  xmin  bin lower bound
         * @param  xmax  bin upper bound
         */
        private Bin( double xmin, double xmax ) {
            xmin_ = xmin;
            xmax_ = xmax;
        }

        /**
         * Returns the lower bound of this bin.
         *
         * @return  axis minimum of bin
         */
        public double getXMin() {
            return xmin_;
        }

        /**
         * Returns the upper bound of this bin.
         *
         * @return  axis maximum of bin
         */
        public double getXMax() {
            return xmax_;
        }

        /**
         * Returns the value so far accumulated into this bin.
         *
         * @return   bin value
         */
        public double getY() {
            return value_;
        }

        /**
         * Accumulates a value into this bin.
         *
         * @param   inc  value to add to bin
         */
        private void add( double inc ) {
            value_ += inc;
        }
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
}
