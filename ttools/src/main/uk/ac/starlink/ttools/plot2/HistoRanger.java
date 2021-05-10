package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Histogram-capable Ranger implementation.
 * Evenly sampled values from the input data stream are stored so that
 * scaling based on the detailed distribution of the data, rather than just
 * its minimum and maximum values, can be achieved.
 *
 * <p>This ranger is not intended to calculate arithmetically accurate
 * quantile values, but it's hopefully good enough to come up with colour maps.
 * The current implementation does a reasonable job of adaptive sampling,
 * but for large datasets (greater than the storage capacity)
 * it may be biassed, especially the {@link #add} method required for
 * range calculation in parallel.
 *
 * @author   Mark Taylor
 * @since    21 Mar 2019
 */
public class HistoRanger implements Ranger {

    private final int nStore_;
    private final int nQuantile_;
    private final double[] samples_;
    private final Distributor distributor_;

    /**
     * Constructor.
     * The number of samples collected and the number of quantiles
     * can both be configured.  The sample count defines the memory
     * footprint of this object, which ought to be short lived,
     * while the quantile count defines the memory footprint of any
     * span objects created, which may be long-lived.
     *
     * @param   nStore  maximum number of samples to store
     * @param   nQuantile  maximum number of quantiles on which to base
     *                     scaling
     */
    public HistoRanger( int nStore, int nQuantile ) {
        nStore_ = nStore;
        nQuantile_ = nQuantile;
        samples_ = new double[ nStore_ ];
        distributor_ = createDistributor( samples_ );
    }

    @Override
    public void submitDatum( double d ) {
        if ( PlotUtil.isFinite( d ) ) {
            distributor_.submit( d );
        }
    }

    @Override
    public void add( Ranger other ) {
        distributor_.add( ((HistoRanger) other).distributor_ );
    }

    @Override
    public Ranger createCompatibleRanger() {
        return new HistoRanger( nStore_, nQuantile_ );
    }

    @Override
    public Span createSpan() {

        /* Assemble an array of quantiles by sorting the sample buffer and
         * subsampling it.  We use this quantile buffer rather than the
         * full sample buffer to conserve memory (since the Span object
         * may live longer than this Ranger), and to be sure that later
         * calls to submitDatum can't scribble on the data structure.
         * Note we are anyway relying on the assumption that submitDatum
         * is not called while we do this sort/sample operation;
         * we could synchronise to guarantee that, but it might be
         * expensive in submitDatum, so we rely on the Ranger contract
         * which advises against such concurrent usage. */
        int nSamp = distributor_.getSampleCount();
        Arrays.sort( samples_, 0, nSamp );
        double[] sortedQuantiles =
            getSortedQuantiles( samples_, nSamp, nQuantile_ );

        /* Create a span on the basis of this quantile array. */
        int nq = sortedQuantiles.length;
        if ( nq > 2 && sortedQuantiles[ nq - 1 ] > sortedQuantiles[ 0 ] ) {
            return new HistoSpan( sortedQuantiles );
        }
        else {
            Ranger ranger = new BasicRanger( true );
            for ( int i = 0; i < nSamp; i++ ) {
                ranger.submitDatum( samples_[ i ] );
            }
            return ranger.createSpan();
        }
    }

    /**
     * Indicates whether a given span can be used to scale histograms.
     * Iff true, the span's {@link Span#createScaler} method will work
     * with a Scaling argument like {@link Scaling#HISTO}.
     * This method relies on this class's knowledge of its implementation
     * details.
     *
     * @param  span  span instance to test
     * @return   true iff span is histogram-capable
     */
    public static boolean canScaleHistograms( Span span ) {
        return span instanceof HistoSpan;
    }

    /**
     * Extract a quantile array from a sorted list of values.
     *
     * @param  sortedSamples  data values, sorted in ascending order
     * @param  nSample     number of values in sample array
     * @param  nQuantile   approximate number of values required in
     *                     output quantile array
     * @return  quantiles, sorted in ascending order;
     *          the length of this array will be roughly, but not exactly,
     *          <code>nQuantile</code>,
     */
    private static double[] getSortedQuantiles( double[] sortedSamples,
                                                int nSample, int nQuantile ) {
        int step = Math.max( 1, nSample / nQuantile );
        int nq = ( nSample + step - 1 ) / step;
        double[] quantiles = new double[ nq ];
        for ( int iq = 0; iq < nq; iq++ ) {
            quantiles[ iq ] = sortedSamples[ iq * step ];
        }
        return quantiles;
    }

    /**
     * Returns the index of an element in a sorted array
     * that is close to a given value.
     * This wraps Arrays.binarySearch so you don't need to muck about
     * with insertion points.
     * 
     * @param  sortedArray  array sorted in ascending order
     * @param  point    value of interest
     * @return  array index
     */
    private static int getArrayIndex( double[] sortedArray, double point ) {
        int ix = Arrays.binarySearch( sortedArray, point );
        return ix >= 0 ? ix : Math.min( -ix - 1, sortedArray.length - 1 );
    }

    /**
     * Returns a Distributor instance.
     *
     * @param  array   fixed length storage array
     * @return  new distributor (one use only)
     */
    static Distributor createDistributor( double[] array ) {
        return new DefaultDistributor( array );
    }

    /**
     * Defines how a stream of data samples is scattered into a fixed-length
     * array.  Ideally, the stream would be sub-sampled uniformly so that
     * every N'th item was stored, where N = nData/array.length, but nData
     * is not known up front, so some adaptive strategy is required.
     * The positions of the samples (in relation to their sequence in the
     * input stream) doesn't matter, except that they must fill the array
     * contiguously from the start.
     */
    interface Distributor {

        /**
         * Presents a value for possible storage.
         *
         * @param  val  data sample
         */
        void submit( double val );

        /**
         * Merges the contents of another compatible distributor into this one.
         *
         * @param  other  other compatible distributor
         */
        void add( Distributor other );

        /**
         * Indicates the number of array elements that have been filled.
         * All array elements from 0 to the return value of this method
         * should be considered as samples, and others should be ignored.
         *
         * @return  number of contiguous samples currently in output array
         */
        int getSampleCount();
    }

    /**
     * Distributor implementation used here.
     * This implementation stores every sample for the first nStore
     * submitted values, every other sample for the second nStore values,
     * and in general every N'th sample for the Nth nStore values.
     * For fewer than nStore samples, coverage is complete.
     * There are probably better algorithms (ones with more uniform
     * coverage) but this is simple and doesn't do too badly.
     *
     * <p>The add method is fairly sloppy.
     */
    private static class DefaultDistributor implements Distributor {

        private final double[] array_;
        private final int nStore_;
        private long iData_;
        private int iStore_;
        private int iStep_;
        private static final int NF = 100;

        /**
         * Constructor.
         *
         * @param  array  sample storage array
         */
        DefaultDistributor( double[] array ) {
            array_ = array;
            nStore_ = array.length;
            iStep_ = 1;
        }

        public void submit( double val ) {
            if ( iData_++ % iStep_ == 0 ) {
                if ( iStore_ < nStore_ ) {
                    array_[ iStore_ ] = val;
                    iStore_ += iStep_;
                }
                else {
                    iStep_++;
                    iStore_ = iStep_ / 2;
                }
            }
        }

        public int getSampleCount() {
            return iData_ < nStore_ ? (int) iData_ : nStore_;
        }

        /* Because of the statistical nature of the work and the
         * number of possibilities, it would be a lot of effort to
         * write tests for this method.  It has not been well tested.
         * I hope it works. */
        public void add( Distributor otherDist ) {
            DefaultDistributor other = (DefaultDistributor) otherDist;
            if ( other.nStore_ != nStore_ ) {
                throw new IllegalArgumentException( "Incompatible" );
            }
            long nd0 = iData_;
            long nd1 = other.iData_;

            /* No data in other, no work required. */
            if ( nd1 == 0 ) {
                return;
            }

            /* No data in this, just copy state of other to this one. */
            else if ( nd0 == 0 ) {
                iData_ = other.iData_;
                iStore_ = other.iStore_;
                iStep_ = other.iStep_;
                System.arraycopy( other.array_, 0, array_, 0,
                                  other.getSampleCount() );
                return;
            }

            /* The sample array has enough space to store all the data
             * from both distributors.  Copy all the data into this one. */
            else if ( nd0 + nd1 < nStore_ ) {
                assert iStep_ == 1 && other.iStep_ == 1;
                System.arraycopy( other.array_, 0, array_, iStore_,
                                  other.iStore_ );
                iStore_ += other.iStore_;
                iData_ += other.iData_;
                return;
            }

            /* Otherwise, it's more complicated; we have to copy a
             * fraction of the samples from the other distributor to
             * this one, in general overwriting part of this one's
             * sample array. */
            else {

                /* First work out what fraction of the other distributor's
                 * samples should be used. */
                int nf = NF;
                double frac1 = nd1 / (double) ( nd0 + nd1 );

                /* Define a random mask with a proportion of true elements 
                 * corresponding to the amount of samples to be copied.
                 * If all the elements are going to be false, don't do any 
                 * more work. */
                if ( nf * frac1 < 1 ) {
                    return;
                }
                boolean[] mask = createRandomMask( nf, frac1 );

                /* If there's enough empty space in this distributor's
                 * sample array to copy all the subsample from the other one's,
                 * then just add it at the end. */
                if ( frac1 * nd1 < nStore_ - nd0 ) {
                    int ns = other.getSampleCount();
                    for ( int i = 0; i < ns && iStore_ < nStore_; i++ ) {
                        if ( mask[ i % nf ] ) {
                            array_[ iStore_++ ] = other.array_[ i ];
                        }
                    }
                }

                /* Otherwise, overwrite this one's sample array with
                 * a proportion of the values from the other one.
                 * The implementation is sloppy here - it doesn't get
                 * the proportion right for the (normal) case in which
                 * the sample array has not been filled an exact number
                 * of times. */
                else {
                    int ns = Math.min( getSampleCount(),
                                       other.getSampleCount() );
                    for ( int i = 0; i < ns; i++ ) {
                        if ( mask[ i % nf ] ) {
                            array_[ i ] = other.array_[ i ];
                        }
                    }
                }
            }
        }
    }

    /**
     * Produces a boolean array with a given proportion of the elements
     * set to true.  The true values are scattered randomly.
     *
     * @param  nf  required array size; not expected to be huge
     * @param  p   proportion of true values
     * @return  nf-element mask array
     */
    private static boolean[] createRandomMask( int nf, double p ) {
        List<Integer> ilist = new ArrayList<Integer>();
        for ( int i = 0; i < nf; i++ ) {
            ilist.add( Integer.valueOf( i ) );
        }
        Collections.shuffle( ilist,
                             new Random( Double.doubleToLongBits( p ) ) );
        boolean[] mask = new boolean[ nf ];
        for ( int i = 0; i < p * nf; i++ ) {
            mask[ i ] = true;
        }
        return mask;
    }

    /**
     * Span implementation for use with HistoRanger.
     * It is based on an N-element array of quantiles, where element i
     * is the i'th N-quantile.
     */
    private static class HistoSpan implements Span {
        private final double[] sortedQuantiles_;
        private final int dataHash_;
        private final double lo_;
        private final double hi_;
        private final int ilo_;
        private final int ihi_;

        /**
         * Constructs a span given the quantile array.
         *
         * @param  sortedQuantiles  sorted array of quantiles
         */
        HistoSpan( double[] sortedQuantiles ) {
            this( sortedQuantiles, Arrays.hashCode( sortedQuantiles ),
                  sortedQuantiles[ 0 ],
                  sortedQuantiles[ sortedQuantiles.length - 1 ] );
        }

        /**
         * Constructs a span given all members.
         * The dataHash is supplied since it may be somewhat expensive
         * to calculate, and the equals and hashCode methods may be called
         * often.
         *
         * @param  sortedQuantiles  sorted array of quantiles
         * @param  datahash   <code>Arrays.hashCode(sortedQuantiles)</code>
         * @param  lo      lower data bound
         * @param  hi      upper data bound
         */
        HistoSpan( double[] sortedQuantiles, int dataHash,
                   double lo, double hi ) {
            sortedQuantiles_ = sortedQuantiles;
            dataHash_ = dataHash;
            lo_ = lo;
            hi_ = hi;
            int nq = sortedQuantiles_.length;
            ilo_ = getArrayIndex( sortedQuantiles_, lo );
            ihi_ = getArrayIndex( sortedQuantiles_, hi );
        }

        public double getLow() {
            return lo_;
        }

        public double getHigh() {
            return hi_;
        }

        public double[] getFiniteBounds( boolean isPositive ) {
            final double lo;
            if ( isPositive && lo_ <= 0 ) {
                int jlo = Arrays.binarySearch( sortedQuantiles_, ilo_, ihi_,
                                               Double.MIN_VALUE );
                if ( jlo == - sortedQuantiles_.length - 1 ) {
                    lo = Double.NaN;
                }
                else if ( jlo < 0 ) {
                    lo = sortedQuantiles_[ -jlo - 1 ];
                }
                else {
                    lo = sortedQuantiles_[ jlo ];
                }
            }
            else {
                lo = lo_;
            }
            return BasicRanger.calculateFiniteBounds( lo, hi_, isPositive );
        }

        public HistoSpan limit( double lo, double hi ) {
            return new HistoSpan( sortedQuantiles_, dataHash_,
                                  Double.isNaN( lo ) ? lo_ : lo,
                                  Double.isNaN( hi ) ? hi_ : hi );
        }

        public Scaler createScaler( Scaling scaling, Subrange dataclip ) {

            /* If the scaling is histogram-like, produce a custom Scaler
             * instance based on the quantile array. */
            if ( scaling instanceof Scaling.HistogramScaling ) {
                boolean isLog = scaling.isLogLike();
                final HistoSpan clipspan;
                if ( Subrange.isIdentity( dataclip ) ) {
                    clipspan = this;
                }
                else {

                    /* Deal with a non-identity clip by just scaling the
                     * data min/max range.  It would be nice to interpret
                     * this as scale adjustment of the quantiles instead,
                     * but currently behaviour elsewhere in the plotting
                     * framework means that doesn't work out. */
                    double[] cliprange =
                        PlotUtil.scaleRange( lo_, hi_, dataclip, isLog );
                    clipspan = limit( cliprange[ 0 ], cliprange[ 1 ] );
                }
                double[] clipbounds = clipspan.getFiniteBounds( isLog );
                double lo = clipbounds[ 0 ];
                double hi = clipbounds[ 1 ];
                int ilo = getArrayIndex( sortedQuantiles_, lo );
                int ihi = getArrayIndex( sortedQuantiles_, hi );

                /* If the quantile range is big enough, return a scaler based
                 * on it. */
                if ( ihi - ilo > 2 ) {
                    return new HistoScaler( ilo, ihi, lo, hi, sortedQuantiles_,
                                            dataHash_, isLog );
                }

                /* If there are too few quantiles on which to base the ranging,
                 * or something else looks strange, just fall back to a
                 * basic scaling. */
                else {
                    return ( isLog ? Scaling.LOG : Scaling.LINEAR )
                          .createScaler( lo, hi );
                }
            }

            /* This span can scale range-like scalings as well - delegate
             * to the implemntation in BasicRanger. */
            if ( scaling instanceof Scaling.RangeScaling ) {
                return BasicRanger
                      .createRangeScaler( (Scaling.RangeScaling) scaling,
                                          dataclip, this );
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int hashCode() {
            int code = 887267;
            code = 23 * code + dataHash_;
            code = 23 * code + Float.floatToIntBits( (float) lo_ );
            code = 23 * code + Float.floatToIntBits( (float) hi_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HistoSpan ) {
                HistoSpan other = (HistoSpan) o;
                return this == other
                    || ( this.lo_ == other.lo_
                      && this.hi_ == other.hi_
                      && this.dataHash_ == other.dataHash_
                      && Arrays.equals( this.sortedQuantiles_,
                                        other.sortedQuantiles_ ) );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Scaler implementation based on a quantile array.
     */
    private static class HistoScaler implements Scaler {
        final int ilo_;
        final int ihi_;
        final double lo_;
        final double hi_;
        final double[] sortedQuantiles_;
        final int dataHash_;
        final boolean isLogLike_;
        final double nq1_;
        final double loval_;
        final double hival_;

        /**
         * The dataHash is supplied since it may be somewhat expensive
         * to calculate, and the equals and hashCode methods may be called
         * often.
         *
         * @param  ilo  closest index in quantile array for scale lower bound
         * @param  ihi  closest index in quantile array for scale upper bound
         * @param  lo   scale lower bound
         * @param  hi   scale upper bound
         * @param  sortedQuantiles  quantile array
         * @param  datahash   <code>Arrays.hashCode(sortedQuantiles)</code>
         * @param  isLogLike  true for log-like scaling
         */
        HistoScaler( int ilo, int ihi, double lo, double hi,
                     double[] sortedQuantiles, int dataHash,
                     boolean isLogLike ) {
            ilo_ = ilo;
            ihi_ = ihi;
            lo_ = lo;
            hi_ = hi;
            sortedQuantiles_ = sortedQuantiles;
            dataHash_ = dataHash;
            isLogLike_ = isLogLike;
            nq1_ = 1.0 / ( ihi_ - ilo_ );
            loval_ = Math.max( lo_, sortedQuantiles[ 0 ] );
            hival_ = Math.min( hi_,
                               sortedQuantiles[ sortedQuantiles.length - 1 ] );
        }

        public double getLow() {
            return lo_;
        }

        public double getHigh() {
            return hi_;
        }

        public boolean isLogLike() {
            return isLogLike_;
        }

        public double scaleValue( double val ) {

            /* Clip. */
            if ( val <= loval_ ) {
                return 0;
            }
            else if ( val >= hival_ ) {
                return 1;
            }
            else if ( Double.isNaN( val ) ) {
                return Double.NaN;
            }
            else {

                /* Determine where the value sits in the quantile array. */
                int j = Arrays.binarySearch( sortedQuantiles_,
                                             ilo_, ihi_, val );

                /* Found exact value. */
                if ( j >= 0 ) {
                    assert j >= ilo_ && j <= ihi_;
                    return ( j - ilo_ ) * nq1_;
                }

                /* Found insertion point; do linear interpolation between
                 * bounding values. */
                else {
                    int jb = -j - 1;
                    int ja = jb - 1;
                    double va = sortedQuantiles_[ ja ];
                    double vb = sortedQuantiles_[ jb ];
                    double v = (ja + ( val - va ) / ( vb - va ) - ilo_) * nq1_;
                    return Math.max( 0, Math.min( 1, v ) );
                }
            }
        }

        @Override
        public int hashCode() {
            int code = 44223;
            code = 23 * code + ilo_;
            code = 23 * code + ihi_;
            code = 23 * code + Float.floatToIntBits( (float) lo_ );
            code = 23 * code + Float.floatToIntBits( (float) hi_ );
            code = 23 * code + dataHash_;
            code = 23 * code + ( isLogLike_ ? 99 : 101 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HistoScaler ) {
                HistoScaler other = (HistoScaler) o;
                return this.ilo_ == other.ilo_
                    && this.ihi_ == other.ihi_
                    && this.lo_ == other.lo_
                    && this.hi_ == other.hi_
                    && this.dataHash_ == other.dataHash_
                    && this.isLogLike_ == other.isLogLike_
                    && Arrays.equals( this.sortedQuantiles_,
                                      other.sortedQuantiles_ );
            }
            else {
                return false;
            }
        }
    }
}
