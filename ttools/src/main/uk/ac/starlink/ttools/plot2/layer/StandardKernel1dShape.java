package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Implementation class for Kernel1dShapes based on evaluating
 * symmetric functions over a limited extent.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2015
 */
@Equality
public abstract class StandardKernel1dShape implements Kernel1dShape {

    private final String name_;
    private final String description_;
    private final double normExtent_;
    private final boolean isSquare_;

    /** Rectangular kernel shape. */
    public static final StandardKernel1dShape SQUARE =
        new StandardKernel1dShape( "square", "Uniform value: f(x)=1, |x|=0..1",
                                   1.0, true ) {
            public double evaluate( double x ) { 
                return 1.0;
            }
        };

    /** Linear (triangular) kernel shape. */
    public static final StandardKernel1dShape LINEAR =
        new StandardKernel1dShape( "linear", "Triangle: f(x)=1-|x|, |x|=0..1",
                                   1.0, false ) {
            public double evaluate( double x ) {
                return 1.0 - x;
            }
        };

    /** Epanechnikov (parabola) kernel shape. */
    public static final StandardKernel1dShape EPANECHNIKOV =
        new StandardKernel1dShape( "Epanechnikov",
                                   "Parabola: f(x)=1-x*x, |x|=0..1",
                                   1.0, false ) {
            public double evaluate( double x ) {
                return 1.0 - x * x;
            }
        };

    /** Cosine kernel shape. */
    public static final StandardKernel1dShape COS =
        new StandardKernel1dShape( "cos",
                                   "Cosine: f(x)=cos(x*pi/2), |x|=0..1",
                                            1.0, false ) {
            public double evaluate( double x ) {
                return Math.cos( 0.5 * Math.PI * x );
            }
        };

    /** Cosine squared kernel shape. */
    public static final StandardKernel1dShape COS2 =
        new StandardKernel1dShape( "cos2",
                                   "Cosine squared: "
                                 + "f(x)=cos^2(x*pi/2), |x|=0..1",
                                   1.0, false ) {
            public double evaluate( double x ) {
                double c = Math.cos( 0.5 * Math.PI * x );
                return c * c;
            }
        };

    /**
     * Delta function kernel.
     * Convolution of a function with this kernel leaves it unaffected.
     */
    public static final Kernel1d DELTA = new Kernel1d() {
        public double[] convolve( double[] in ) {
            return in;
        }
        public int getExtent() {
            return 0;
        }
        public boolean isSquare() {
            return true;
        }
    };

    private static final Kernel1dShape[] STANDARD_OPTIONS = {
        SQUARE, LINEAR, EPANECHNIKOV, COS, COS2,
        createTruncatedGaussian( 3 ),
        createTruncatedGaussian( 6 ),
    };

    /**
     * Constructor.
     *
     * @param  name  kernel shape name
     * @param  description   short description
     * @param  normExtent   kernel extent for unit nominal width
     * @param  isSquare   true iff kernel is considered non-smooth
     */
    protected StandardKernel1dShape( String name, String description,
                                     double normExtent, boolean isSquare ) {
        name_ = name;
        description_ = description;
        normExtent_ = normExtent;
        isSquare_ = isSquare;
    }

    /**
     * Returns the point value of the function defining this shape
     * at a point a given absolute fraction of the nominal width
     * from the center.
     * Calling this method for values of <code>x</code> out of the range
     * <code>0&lt;=x&lt;=getNormalisedExtent()</code> has an undefined
     * effect; the function value is assumed symmetric and zero for
     * larger absolute values.
     *
     * @param   x   normalised absolute distance in range 0..normExtent
     * @return    function value at <code>x</code>
     */
    protected abstract double evaluate( double x );

    /**
     * Returns the extent of a kernel with this shape of unit nominal width.
     * The value of the {@link #evaluate evaluate(x)} method for 
     * <code>x</code> greater than the value returned from this method
     * is taken to be zero.
     */
    public double getNormalisedExtent() {
        return normExtent_;
    }

    /**
     * Indicates whether this shape has features which are intentionally
     * non-smooth and should be portrayed as such.
     * This non-smoothness applies either within the extent or at its edge.
     *
     * @return   true iff there are non-smooth features that should be visible
     */
    public boolean isSquare() {
        return isSquare_;
    }

    /**
     * Returns a one-word name for this shape.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description for this shape.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    public Kernel1d createFixedWidthKernel( double width ) {
        if ( width < 0 ) {
            throw new IllegalArgumentException( "negative width" );
        }
        else if ( width == 0 ) {
            return DELTA;
        }
        else {
            double[] levels = getFixedWidthLevels( width );
            return createSymmetricNormalisedKernel( levels, isSquare() );
        }
    }

    public Kernel1d createMeanKernel( double width ) {
        if ( width < 0 ) {
            throw new IllegalArgumentException( "negative width" );
        }
        else if ( width == 0 ) {
            return DELTA;
        }
        else {
            double[] levels = getFixedWidthLevels( width );
            return createSymmetricMeanKernel( levels, isSquare() );
        }
    }

    public Kernel1d createKnnKernel( double k, boolean isSymmetric,
                                     int minWidth, int maxWidth ) {
        if ( ! ( k >= 0 ) ) {
            throw new IllegalArgumentException( "negative knn" );
        }
        else if ( minWidth < 0 ) {
            throw new IllegalArgumentException( "negative minimum width" );
        }
        else if ( minWidth > maxWidth ) {
            throw new IllegalArgumentException( "min/max wrong way round" );
        }
        else if ( k == 0 || minWidth == maxWidth ) {
            return createFixedWidthKernel( minWidth );
        }
        else {
            return new KnnKernel( this, k, isSymmetric, minWidth, maxWidth );
        }
    }

    /**
     * Returns per-pixel levels corresponding to the shape of this kernel
     * for a given half-width.
     *
     * @param  width  half-width
     * @return  level array
     */
    private double[] getFixedWidthLevels( double width ) {
        double normExtent = getNormalisedExtent();
        double ext = normExtent * width;
        int nlevel = (int) Math.ceil( ext );
        if ( nlevel == ext && evaluate( normExtent ) != 0 ) {
            nlevel++;
        }
        double[] levels = new double[ nlevel ];
        double xscale = 1.0 / width;
        assert nlevel * xscale / normExtent >= 0.99999999;
        for ( int i = 0; i < nlevel; i++ ) {
            double x = i * xscale;
            assert x >= 0 && x <= normExtent;
            levels[ i ] = evaluate( x );
        }
        return levels;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns an array of the generally recommended kernel shape options.
     *
     * @return  kernel shape options
     */
    public static Kernel1dShape[] getStandardOptions() {
        return STANDARD_OPTIONS.clone();
    }

    /**
     * Returns a kernel shape based on the Gaussian function with
     * truncation at a given number of standard deviations.
     *
     * @param  truncSigma  number of sigma at which to truncate the kernel
     * @return   new kernel shape
     */
    public static StandardKernel1dShape
            createTruncatedGaussian( final double truncSigma ) {
        String strunc = (int) truncSigma == truncSigma
                      ? Integer.toString( (int) truncSigma )
                      : Double.toString( truncSigma );
        String name = "gauss" + strunc;
        String description = "Gaussian truncated at " + truncSigma + " sigma: "
                           + "f(x)=exp(-x*x/2), |x|=0.." + strunc;
        return new StandardKernel1dShape( name, description,
                                          truncSigma, false ) {
            public double evaluate( double x ) {
                return Math.exp( - 0.5 * x * x );
            }
        };
    }

    /**
     * Creates a symmetric normalised kernel based on a fixed array of
     * function values.  The <code>levels</code> array gives a list of
     * the values at x=0, 1 (and -1), 2 (and -2), ....
     *
     * @param  levels  kernel function values on 1d grid starting from 0
     * @param  isSquare  true iff the kernel is considered non-smooth
     * @return   new kernel
     */
    public static Kernel1d createSymmetricNormalisedKernel( double[] levels,
                                                            boolean isSquare ) {
        if ( levels.length <= 1 ) {
            return DELTA;
        }
        int offset = levels.length - 1;
        double[] weights = new double[ 2 * offset + 1 ];
        for ( int il = 0; il < levels.length; il++ ) {
            weights[ offset + il ] = levels[ il ];
            weights[ offset - il ] = levels[ il ];
        }
        double sum = 0;
        for ( int iw = 0; iw < weights.length; iw++ ) {
            sum += weights[ iw ];
        }
        double scale = 1.0 / sum;
        for ( int iw = 0; iw < weights.length; iw++ ) {
            weights[ iw ] *= scale;
        }
        FixedKernel kernel = new FixedKernel( weights, offset, isSquare );
        assert kernel.getExtent() == levels.length - 1;
        return kernel;
    }

    /**
     * Creates a symmetric averabing kernel based on a fixed array of
     * function values.  The <code>levels</code> array gives a list of
     * the values at x=0, 1 (and -1), 2 (and -2), ....
     *
     * @param  levels  kernel function values on 1d grid starting from 0
     * @param  isSquare  true iff the kernel is considered non-smooth
     * @return   new kernel
     */
    public static Kernel1d createSymmetricMeanKernel( double[] levels,
                                                      boolean isSquare ) {
        if ( levels.length <= 1 ) {
            return DELTA;
        }
        int offset = levels.length - 1;
        double[] weights = new double[ 2 * offset + 1 ];
        for ( int il = 0; il < levels.length; il++ ) {
            weights[ offset + il ] = levels[ il ];
            weights[ offset - il ] = levels[ il ];
        }
        MeanKernel kernel = new MeanKernel( weights, offset, isSquare );
        assert kernel.getExtent() == levels.length - 1;
        return kernel;
    }

    /**
     * Returns the value of the argument, or zero if it's NaN.
     *
     * @param   d  value
     * @return  non-NaN value
     */
    private static double definiteValue( double d ) {
        return Double.isNaN( d ) ? 0 : d;
    }

    /**
     * Kernel implementation based on an array given gridded function values.
     * This is not necessarily normalised or symmetric.
     */
    private static class FixedKernel implements Kernel1d {

        private final double[] weights_;
        private final int offset_;
        private final boolean isSquare_;

        /**
         * Constructor.
         *
         * @param   weights  function values on 1d grid
         * @param   offset  index into weight corresponding to x=0
         * @param  isSquare  true iff the kernel is considered non-smooth
         */
        public FixedKernel( double[] weights, int offset, boolean isSquare ) {
            weights_ = weights.clone();
            offset_ = offset;
            isSquare_ = isSquare;
        }

        public int getExtent() {
            return Math.max( offset_, weights_.length - 1 - offset_ );
        }

        public double[] convolve( double[] in ) {
            int ns = in.length;
            int nw = weights_.length;
            double[] out = new double[ ns ];
            for ( int is = 0; is < ns; is++ ) {
                double val = in[ is ]; 
                if ( ! Double.isNaN( val ) ) {
                    int iw0 = Math.max( 0, offset_ - is );
                    int iw1 = Math.min( nw, ns + offset_ - is );
                    for ( int iw = iw0; iw < iw1; iw++ ) { 
                        int ix = is + iw - offset_;
                        out[ is + iw - offset_ ] += weights_[ iw ] * val;
                    }
                }
            }
            return out;
        }

        public boolean isSquare() {
            return isSquare_;
        }

        @Override
        public int hashCode() {
            int code = 9902;
            code = 23 * code + Arrays.hashCode( weights_ );
            code = 23 * code + offset_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedKernel ) {
                FixedKernel other = (FixedKernel) o;
                return Arrays.equals( this.weights_, other.weights_ )
                    && this.offset_ == other.offset_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Averaging kernel implementation based ona an array of given gridded
     * function values.  This is not necessarily symmetric.
     */
    private static class MeanKernel implements Kernel1d {

        private final double[] weights_;
        private final int offset_;
        private final boolean isSquare_;

        /**
         * Constructor.
         *
         * @param   weights  function values on 1d grid
         * @param   offset  index into weight corresponding to x=0
         * @param  isSquare  true iff the kernel is considered non-smooth
         */
        public MeanKernel( double[] weights, int offset, boolean isSquare ) {
            weights_ = weights.clone();
            offset_ = offset;
            isSquare_ = isSquare;
        }

        public int getExtent() {
            return Math.max( offset_, weights_.length - 1 - offset_ );
        }

        public double[] convolve( double[] in ) {
            int ns = in.length;
            int nw = weights_.length;
            double[] dsums = new double[ ns ];
            double[] wsums = new double[ ns ];
            for ( int is = 0; is < ns; is++ ) {
                double val = in[ is ];
                if ( ! Double.isNaN( val ) ) {
                    int iw0 = Math.max( 0, offset_ - is );
                    int iw1 = Math.min( nw, ns + offset_ - is );
                    for ( int iw = iw0; iw < iw1; iw++ ) {
                        int ix = is + iw - offset_;
                        double w = weights_[ iw ];
                        wsums[ ix ] += w;
                        dsums[ ix ] += w * val;
                    }
                }
            }
            double[] out = new double[ ns ];
            for ( int is = 0; is < ns; is++ ) {
                double sw = wsums[ is ];
                double sd = dsums[ is ];
                out[ is ] = sw == 0 ? Double.NaN : sd / sw;
            }
            return out;
        }

        public boolean isSquare() {
            return isSquare_;
        }

        @Override
        public int hashCode() {
            int code = 2209;
            code = 23 * code + Arrays.hashCode( weights_ );
            code = 23 * code + offset_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MeanKernel ) {
                MeanKernel other = (MeanKernel) o;
                return Arrays.equals( this.weights_, other.weights_ )
                    && this.offset_ == other.offset_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * K-Nearest-Neighbours adaptive kernel implementation.
     */
    private static class KnnKernel implements Kernel1d {

        private final StandardKernel1dShape kshape_;
        private final double k_;
        private final boolean isSymmetric_;
        private final int minWidth_;
        private final int maxWidth_;
        private final double[][] weightArrays_;
        private final int maxExtent_;

        /**
         * Constructor.
         *
         * @param  kshape   kernel shape
         * @param  k   number of nearest neighbours within function width
         * @param  isSymmetric  true for bidirectional KNN, false for unidir
         * @param  minWidth    lower limit for function width
         * @param  maxWidth   upper limit for function width
         */
        KnnKernel( StandardKernel1dShape kshape, double k, boolean isSymmetric,
                   int minWidth, int maxWidth ) {
            kshape_ = kshape;
            k_ = k;
            isSymmetric_ = isSymmetric;
            minWidth_ = minWidth;
            maxWidth_ = maxWidth;
            weightArrays_ = new double[ maxWidth_ - minWidth_ + 1 ][];
            for ( int iw = minWidth_; iw <= maxWidth_; iw++ ) {
                weightArrays_[ iw - minWidth_ ] =
                    getNormalisedWeightArray( kshape, iw );
            }
            maxExtent_ = kshape.createFixedWidthKernel( maxWidth ).getExtent();
        }

        public int getExtent() {
            return maxExtent_;
        }

        public boolean isSquare() {
            return kshape_.isSquare();
        }

        public double[] convolve( double[] in ) {
            int ns = in.length;
            double[] out = new double[ ns ];
            for ( int is = 0; is < ns; is++ ) {
                final int pw;
                final int mw;
                if ( isSymmetric_ ) {
                    pw = bidirectionalKnnWidth( in, is, maxWidth_ );
                    mw = pw;
                }
                else {
                    pw = unidirectionalKnnWidth( in, is, true,
                                                 Math.min( maxWidth_,
                                                           ns - is ) );
                    mw = unidirectionalKnnWidth( in, is, false,
                                                 Math.min( maxWidth_, is ) );
                }
                int pWidth = Math.max( minWidth_, pw );
                int mWidth = Math.max( minWidth_, mw );
                double[] pWeights = weightArrays_[ pWidth - minWidth_ ];
                double[] mWeights = weightArrays_[ mWidth - minWidth_ ];
                double val0 = in[ is ];
                double oval = Double.isNaN( val0 )
                            ? 0.0
                            : 0.5 * ( pWeights[ 0 ] + mWeights[ 0 ] ) * val0;
                int pnw = Math.min( pWeights.length, ns - is );
                int mnw = Math.min( mWeights.length, is );
                for ( int js = 1; js < pnw; js++ ) {
                    double val = in[ is + js ];
                    if ( ! Double.isNaN( val ) ) {
                        oval += pWeights[ js ] * val;
                    }
                }
                for ( int js = 1; js < mnw; js++ ) {
                    double val = in[ is - js ];
                    if ( ! Double.isNaN( val ) ) {
                        oval += mWeights[ js ] * val;
                    }
                }
                out[ is ] = oval;
            }
            return out;
        }

        @Override
        public int hashCode() {
            int code = 4254352;
            code = 23 * code + kshape_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) k_ );
            code = 23 * code + ( isSymmetric_ ? 11 : 13 );
            code = 23 * code + minWidth_;
            code = 23 * code + maxWidth_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof KnnKernel ) {
                KnnKernel other = (KnnKernel) o;
                return this.kshape_.equals( other.kshape_ )
                    && this.k_ == other.k_
                    && this.isSymmetric_ == other.isSymmetric_
                    && this.minWidth_ == other.minWidth_
                    && this.maxWidth_ == other.maxWidth_;
            }
            else {
                return false;
            }
        }

        /**
         * Returns the k-nearest-neighbours width for a given point
         * in a sample grid, looking in one direction.
         *
         * @param  data  histogram data
         * @param  js   test index into data array
         * @param  isPositive  true to look in direction of increasing index,
         *                     false for decreasing
         * @param  maxWidth  maximum acceptable result (used if k not reached)
         * @return   unidirectional width for k nearest neighbours,
         *           limited to maxWidth
         */
        private int unidirectionalKnnWidth( double[] data, int js,
                                            boolean isPositive, int maxWidth ) {
            int step = isPositive ? +1 : -1;
            double sum = 0;
            for ( int i = 0; i < maxWidth; i++ ) {
                sum += definiteValue( data[ js ] );
                if ( sum >= k_ ) {
                    return i;
                }
                js += step;
            }
            return maxWidth;
        }

        /**
         * Returns the k-nearest-neighbours width for a given point
         * in the sample grid, looking in both directions.
         *
         * @param  data  histogram data
         * @param  js   test index into data array
         * @param  maxWidth  maximum acceptable result (used if k not reached)
         * @return   bidirectional width for k nearest neighbours,
         *           limited to maxWidth
         */
        private int bidirectionalKnnWidth( double[] data, int js,
                                           int maxWidth ) {
            double sum = definiteValue( data[ js ] );
            for ( int i = 1; i < maxWidth; i++ ) {
                int ks = js - i;
                int ls = js + i;
                if ( ks >= 0 ) {
                    sum += definiteValue( data[ ks ] );
                }
                if ( ls < data.length ) {
                    sum += definiteValue( data[ ls ] );
                }
                if ( sum >= k_ ) {
                    return i;
                }
            }
            return maxWidth;
        }

        /**
         * Creates a unidirectional weight array for a given integral
         * characteristic width.
         *
         * @param  kshape  kernel shape
         * @param  width   function characteristic width
         * @return   array of weights, element zero is weight at point
         */
        private static double[]
                getNormalisedWeightArray( StandardKernel1dShape kshape,
                                          int width ) {
            if ( width == 0 ) {
                return new double[] { 1.0 };
            }
            double normExtent = kshape.getNormalisedExtent();
            double ext = normExtent * width;
            int nw = (int) Math.ceil( ext );
            if ( nw == ext && kshape.evaluate( normExtent ) != 0 ) {
                nw++;
            }
            double[] weights = new double[ nw ];
            double xscale = 1.0 / width;
            double total = 0;
            for ( int i = 0; i < nw; i++ ) {
                double x = i * xscale;
                assert x >= 0 && x <= normExtent;
                double f = kshape.evaluate( x );
                weights[ i ] = f;
                total += f * ( i == 0 ? 1 : 2 );
            }
            double scale = 1.0 / total;
            for ( int i = 0; i < nw; i++ ) {
                weights[ i ] *= scale;
            }
            return weights;
        }
    }
}
