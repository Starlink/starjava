package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.DoubleUnaryOperator;
import uk.ac.starlink.ttools.func.Maths;

/**
 * Implementation class for functions to do with Scaling instances.
 *
 * @author   Mark Taylor
 * @since    21 Mar 2019
 */
public class Scalings {

    private static final Object SQRT_TYPE = new Object();
    private static final Object SQR_TYPE = new Object();
    private static final Object ACOS_TYPE = new Object();
    private static final Object COS_TYPE = new Object();
    private static final double AUTO_DELTA = 0.0625;
    private static final double UNSCALE_TOL = 0.0001;
    private static final double UNSCALE_MAXIT = 50;
    private static final Scaling.RangeScaling LINEAR =
        createLinearScaling( "Linear" );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Private constructor prevents instantiation.
     */
    private Scalings() {
    }

    /**
     * Returns a ranger suitable for use with all of a given list of scalings.
     * If a ranger is obtained from this method and used to create a
     * {@link Span}, that span's {@link Span#createScaler createScaler} method
     * can be used with any of the scalings presented here.
     *
     * @param   scalings  list of scalings required for compatibility;
     *                    null values are permitted, and add no constraints
     * @return  suitable ranger
     */
    public static Ranger createRanger( Scaling[] scalings ) {
        boolean hasRange = false;
        boolean hasHisto = false;
        boolean hasOther = false;
        for ( Scaling scaling : scalings ) {
            if ( scaling instanceof Scaling.RangeScaling ) {
                hasRange = true;
            }
            else if ( scaling instanceof Scaling.HistogramScaling ) {
                hasHisto = true;
            }
            else if ( scaling != null ) {
                logger_.warning( "Unknown scaling type: " + scaling );
                hasOther = true;
            }
        }
        if ( hasOther ) {
            return new BasicRanger( true );
        }
        else if ( hasHisto ) {
            return new HistoRanger( 100000, 1000 );
        }
        else {
            return new BasicRanger( false );
        }
    }

    /**
     * Determines whether all of a list of scalings can be used to
     * create Scaler objects from given span instances.
     *
     * @param   scalings  scaling instances for which scalers may be required
     * @param   dataSpan  span obtained from ranging data
     * @param   fixSpan   span obtained by direct user input of bounds
     * @return   true iff spans are sufficient,
     *           false if new span instances are going to be needed
     */
    public static boolean canScale( Scaling[] scalings, Span dataSpan,
                                    Span fixSpan ) {
        boolean hasRange = false;
        boolean hasHisto = false;
        boolean hasOther = false;
        for ( Scaling scaling : scalings ) {
            if ( scaling instanceof Scaling.RangeScaling ) {
                hasRange = true;
            }
            else if ( scaling instanceof Scaling.HistogramScaling ) {
                hasHisto = true;
            }
            else if ( scaling != null ) {
                assert false;
                logger_.warning( "Unknown scaling type: " + scaling );
                hasOther = true;
            }
        }
        if ( hasOther ) {
            return false;
        }
        if ( hasHisto ) {
            if ( ! isFiniteSpan( dataSpan ) ||
                 ! HistoRanger.canScaleHistograms( dataSpan ) ) {
                return false;
            }
        }
        return isFiniteSpan( dataSpan ) || isFiniteSpan( fixSpan );
    }

    /**
     * Constructs the linear scaling instance.
     *
     * @param  name  scaling name
     * @return  linear scaling
     */
    static Scaling.RangeScaling createLinearScaling( String name ) {
        return new ClippedScaling( name, "Linear scaling", false ) {
            final Scaling type = this;
            public Scaler createClippedScaler( final double lo, double hi ) {
                final double scale = 1.0 / ( hi - lo );
                return new DefaultScaler( false, lo, hi, type ) {
                    public double scaleValue( double val ) {
                        return ( val - lo ) * scale;
                    }
                };
            }
        };
    }

    /**
     * Constructs the logarithmic scaling instance.
     *
     * @param  name  scaling name
     * @return  logarithmic scaling
     */
    static Scaling.RangeScaling createLogScaling( String name ) {
        return new ClippedScaling( name, "Logarithmic scaling", true ) {
            final Scaling type = this;
            public Scaler createClippedScaler( double lo, double hi ) {
                final double xlo;
                if ( lo > 0 ) {
                    xlo = lo;
                }
                else if ( hi > 1 ) {
                    xlo = 1;
                }
                else if ( hi > 0 ) {
                    xlo = hi * 0.001;
                }
                else {
                    xlo = .1;
                    hi = 10;
                }
                final double base1 = 1.0 / xlo;
                final double scale = 1.0 / ( Math.log( hi ) - Math.log( xlo ) );
                return new DefaultScaler( true, xlo, hi, type ) {
                    public double scaleValue( double val ) {
                        return val > 0 ? Math.log( val * base1 ) * scale
                                       : 0;
                    }
                };
            }
        };
    }

    /**
     * Constructs the Auto scaling instance.
     * This is based on the Asinh function for x&gt;=0,
     * parameterised such that the difference between output colours
     * at the bottom end of the range should be visible.
     *
     * @param  name  scaling name
     * @return  auto scaling
     */
    static Scaling.RangeScaling createAutoScaling( final String name ) {
        final Scaling.RangeScaling asinh =
            createAutoScaling( "Asinh-auto", AUTO_DELTA );
        final double minSpan = 1.0 / AUTO_DELTA + 1;
        final String descrip = "asinh-based scaling with default parameters";
        return new Scaling.RangeScaling() {
            public String getName() {
                return name;
            }
            public String getDescription() {
                return descrip;
            }
            public boolean isLogLike() {
                return false;
            }
            public Scaler createScaler( double lo, double hi ) {
                return asinh.createScaler( lo, Math.max( hi, lo + minSpan ) );
            }
        };
    }

    /**
     * Constructs the square root scaling instance.
     *
     * @param  name  scaling name
     * @return  square root scaling
     */
    static Scaling.RangeScaling createSqrtScaling( String name ) {
        return new ReScaling( name, "Square root scaling", LINEAR,
                              new DefaultScaler( true, 0, 1, SQRT_TYPE ) {
                                  public double scaleValue( double val ) {
                                      return Math.sqrt( val );
                                  }
                              } );
    }

    /**
     * Constructs the asinh scaling instance.
     * This one is a bit strange, since zero input is constrained to equal
     * zero output.
     *
     * @param  name  scaling name
     * @return  asinh scaling
     */
    static Scaling.RangeScaling createAsinhScaling( String name ) {
        return new ClippedScaling( name, "Asinh scaling", false ) {
            final Scaling type = this;
            public Scaler createClippedScaler( double lo, double hi ) {
                double amax = Math.max( Math.abs( lo ), Math.abs( hi ) );

                /* Fix the scaling so that the linear part of the range
                 * occupies something like 1/4 of the visible range.
                 * This is pretty ad-hoc, at time of writing I don't know
                 * whether it's a good choice numerically. */
                double a1 = Math.sinh( 4.0 ) / amax;
                DoubleUnaryOperator f = d -> Maths.asinh( a1 * d );
                double min = f.applyAsDouble( lo );
                double max = f.applyAsDouble( hi );
                double range1 = 1.0 / ( max - min );
                return new DefaultScaler( false, lo, hi, type ) {
                    public double scaleValue( double val ) {
                        return ( f.applyAsDouble( val ) - min ) * range1;
                    }
                };
            }
        };
    }

    /**
     * Constructs the square scaling instance.
     *
     * @param  name  scaling name
     * @return  square scaling
     */
    static Scaling.RangeScaling createSquareScaling( String name ) {
        return new ReScaling( name, "Square scaling", LINEAR,
                              new DefaultScaler( true, 0, 1, SQR_TYPE ) {
                                  public double scaleValue( double val ) {
                                      return val * val;
                                  }
                              } );
    }

    /**
     * Constructs the Arccos scaling instance.
     * It's a sigmoid horizontal at each end.
     *
     * @param  name  scaling name
     * @return   arccos scaling
     */
    static Scaling.RangeScaling createAcosScaling( String name ) {
        return new ReScaling( name, "Arccos Scaling", LINEAR,
                              new DefaultScaler( false, 0, 1, ACOS_TYPE ) {
                                  public double scaleValue( double val ) {
                                      return Math.acos( 1 - 2 * val ) / Math.PI;
                                  }
                              } );
    }

    /**
     * Constructs the Cos scaling instance.
     * It's a sigmoid vertical at each end.
     *
     * @param  name  scaling name
     * @return   cos scaling
     */
    static Scaling.RangeScaling createCosScaling( String name ) {
        return new ReScaling( name, "Cos Scaling", LINEAR,
                new DefaultScaler( false, 0, 1, COS_TYPE ) {
                    public double scaleValue( double val ) {
                        return 0.5 * ( 1 + Math.cos( ( 1 + val ) * Math.PI ) );
                    }
                }
        );
    }

    /**
     * Constructs a histogram-like scaling instance.
     *
     * @param  name  scaling name
     * @param  isLogLike  true for logarithmic axis, false for linear
     */
    static Scaling.HistogramScaling
            createHistogramScaling( final String name,
                                    final boolean isLogLike ) {
        final String descrip = "Scaling follows data distribution, with "
                             + ( isLogLike ? "logarithmic" : "linear" )
                             + " axis";
        return new Scaling.HistogramScaling() {
            public String getName() {
                return name;
            }
            public String getDescription() {
                return descrip;
            }
            public boolean isLogLike() {
                return isLogLike;
            }
            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Constructs a scaling suitable for automatic density distributions.
     *
     * @param   name  scaling name
     * @param  delta  output difference for lower-end input unit difference
     * @return  scaling
     */
    public static Scaling.RangeScaling createAutoScaling( String name,
                                                          double delta ) {
        String descrip = "asinh-based scaling in which a unit difference "
                       + "at the bottom of the input scale "
                       + "translates to a difference of " + delta + " "
                       + "in the output";
        return new AsinhScaling( name, descrip, delta );
    }

    /**
     * Utility method to perform the inverse operation of Scaler.scaleValue.
     *
     * @param  scaler   scaler instance
     * @param  lo       lower bound of input data value
     * @param  hi       upper bound of input data value
     * @param  frac    required output value of scaleValue method,
     *                 must be in range 0..1
     * @return   value x that causes scaler.scaleValue(x)
     *           to return <code>frac</code>
     */
    public static double unscale( Scaler scaler, double lo, double hi,
                                  double frac ) {

        /* Use the bisection method here; it may be a bit less efficient
         * than the secant method, but it's more robust.
         * If the scaleValue method is well-behaved and monotonic, it's
         * bound to converge. */
        return unscaleBisect( scaler, lo, hi, frac );
    }

    /**
     * Numerically invert the scaler.scaleValue function using the bisection
     * method.
     *
     * @param  scaler   scaler instance
     * @param  lo       lower bound of input data value
     * @param  hi       upper bound of input data value
     * @param  frac    required output value of scaleValue method,
     *                 must be in range 0..1
     * @return   value x that causes scaler.scaleValue(x)
     *           to return <code>frac</code>
     */
    private static double unscaleBisect( Scaler scaler, double lo, double hi,
                                         double frac ) {
        int n = 0;
        double dLo = lo;
        double dHi = hi;
        double fTarget = frac;
        double fLo = scaler.scaleValue( dLo );
        double fHi = scaler.scaleValue( dHi );
        if ( fLo == frac ) {
            return dLo;
        }
        if ( fHi == frac ) {
            return dHi;
        }
        for ( int i = 0; i < UNSCALE_MAXIT; i++ ) {
            double dMid = 0.5 * ( dLo + dHi );
            double fMid = scaler.scaleValue( dMid );
            assert fMid >= 0 && fMid <= 1;
            assert dMid >= lo && dMid <= hi;
            if ( Math.abs( fMid - fTarget ) < UNSCALE_TOL ) {
                return dMid;
            }
            if ( ( fLo - fTarget ) / ( fMid - fTarget ) > 0 ) {
                fLo = fMid;
                dLo = dMid;
            }
            else {
                fHi = fMid;
                dHi = dMid;
            }
        }
        Level level = Level.INFO;
        if ( logger_.isLoggable( level ) ) {
            logger_.info( "Unscale did not converge after " + UNSCALE_MAXIT
                        + " iterations" );
        }
        return lo + frac * ( hi - lo );
    }

    /**
     * Numerically invert the scaler.scaleValue function using the secant
     * method.
     *
     * @param  scaler   scaler instance
     * @param  lo       lower bound of input data value
     * @param  hi       upper bound of input data value
     * @param  frac    required output value of scaleValue method,
     *                 must be in range 0..1
     * @return   value x that causes scaler.scaleValue(x)
     *           to return <code>frac</code>
     */
    private static double unscaleSecant( Scaler scaler, double lo, double hi,
                                         double frac ) {
        double fTarget = frac;
        double d1 = lo;
        double d2 = hi;
        double f1 = scaler.scaleValue( d1 );
        double f2 = scaler.scaleValue( d2 );
        for ( int i = 0; i < UNSCALE_MAXIT; i++ ) {
            double d0 = ( d2 * ( f1 - fTarget ) - d1 * ( f2 - fTarget ) )
                      / ( ( f1 - fTarget ) - ( f2 - fTarget ) );
            double f0 = scaler.scaleValue( d0 );
            if ( Math.abs( f0 - fTarget ) < UNSCALE_TOL ) {
                return d0;
            }
            d2 = d1;
            f2 = f1;
            d1 = d0;
            f1 = f0;
        }
        Level level = Level.INFO;
        if ( logger_.isLoggable( level ) ) {
            logger_.info( "Unscale did not converge after " + UNSCALE_MAXIT
                        + " iterations" );
        }
        return lo + frac * ( hi - lo );
    }

    /**
     * Indicates whether a given span has definite (rather than assumed)
     * upper and lower bounds.
     *
     * @param  span   object to test
     * @return  true iff upper and lower bounds are both finite values
     */
    private static boolean isFiniteSpan( Span span ) {
        if ( span != null ) {
            return PlotUtil.isFinite( span.getLow() )
                && PlotUtil.isFinite( span.getHigh() );
        }
        else {
            return false;
        }
    }

    /**
     * Partial scaling implementation.
     */
    private static abstract class DefaultScaling implements Scaling {
        final String name_;
        final String description_;
        final boolean isLogLike_;
        DefaultScaling( String name, String description, boolean isLogLike ) {
            name_ = name;
            description_ = description;
            isLogLike_ = isLogLike;
        }
        public String getName() {
            return name_;
        }
        public String getDescription() {
            return description_;
        }
        public boolean isLogLike() {
            return isLogLike_;
        }
        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Scaling implementation that uses the Asinh function to ensure
     * a fixed difference (delta) between unit values as the low end of the
     * input scale, tailing off to a finite (but maybe small) different
     * at the high end.
     * <ul>
     * <li>s.scaleValue(lo)=0</li>
     * <li>s.scaleValue(lo+1)=delta</li>
     * <li>s.scaleValue(hi)=1</li>
     * </ul>
     */
    private static class AsinhScaling extends ClippedScaling {
        private final double delta_;

        /**
         * Constructor.
         *
         * @param  name  scaling name
         * @param  description  scaling description
         * @param  delta  output difference between unit input values at
         *                the bottom of the scale
         */
        AsinhScaling( String name, String description, double delta ) {
            super( name, description, false );
            delta_ = delta;
        }

        public Scaler createClippedScaler( final double lo, double hi ) {
            final AsinhScale zScaler = new AsinhScale( delta_, hi - lo );
            return new DefaultScaler( false, lo, hi, AsinhScaling.this ) {
                public double scaleValue( double val ) {
                    return zScaler.scaleValue( val - lo );
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 79982;
            code = 23 * code + Float.floatToIntBits( (float) delta_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof AsinhScaling
                && this.delta_ == ((AsinhScaling) o).delta_;
        }
    }

    /**
     * Applies a fixed scaling to output scalers generated by a base scaling.
     */
    private static class ReScaling extends DefaultScaling
                                   implements Scaling.RangeScaling {
        private final Scaling.RangeScaling baseScaling_;
        private final Scaler rescaler_;

        /**
         * Constructor.
         *
         * @param  name  scaling name
         * @param  description  scaling description
         * @param  baseScaling  basic scaling
         * @param  rescaler  applied to the output of the base scaler;
         *                   so it must map the range 0..1 to 0..1;
         *                   must also implement @Equality
         */
        public ReScaling( String name, String description,
                          Scaling.RangeScaling baseScaling, Scaler rescaler ) {
            super( name, description, baseScaling.isLogLike() );
            baseScaling_ = baseScaling;
            rescaler_ = rescaler;
        }

        public Scaler createScaler( double lo, double hi ) {
            final Scaler baseScaler = baseScaling_.createScaler( lo, hi );
            Object type = new ArrayList<Scaler>( Arrays.asList( new Scaler[] {
                rescaler_, baseScaler,
            } ) );
            return new DefaultScaler( baseScaling_.isLogLike(), lo, hi, type ) {
                public double scaleValue( double val ) {
                    return rescaler_.scaleValue( baseScaler.scaleValue( val ) );
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 33441;
            code = 23 * code + baseScaling_.hashCode();
            code = 23 * code + rescaler_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ReScaling ) {
                ReScaling other = (ReScaling) o;
                return this.baseScaling_ == other.baseScaling_
                    && this.rescaler_ == other.rescaler_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Partial scaling implementation that takes care of clipping input
     * values before handing them off to a supplied scaler.
     */
    private static abstract class ClippedScaling
            extends DefaultScaling
            implements Scaling.RangeScaling {

        /**
         * Constructor.
         *
         * @param  name  scaler name
         * @param  description  scaler description
         * @param  isLogLike  whether it resembles a logarithmic mapping
         */
        protected ClippedScaling( String name, String description,
                                  boolean isLogLike ) {
            super( name, description, isLogLike );
        }

        /**
         * Creates a scaler that can do the basic scaling as long as inputs
         * are in the given range.
         *
         * @param  lo  lower bound of scaler inputs,
         *             corresponds to output value of zero
         * @param  hi  upper bound of scaler inputs,
         *             corresponds to output value of one
         * @return  scaler implementation; it does not need to check that
         *          input scaleValue parameters are in range
         */
        protected abstract Scaler createClippedScaler( double lo, double hi );

        public Scaler createScaler( final double lo, final double hi ) {
            if ( lo < hi ) {
                final Scaler clipScaler = createClippedScaler( lo, hi );
                final double loOut = clipScaler.scaleValue( lo );
                final double hiOut = clipScaler.scaleValue( hi );
                return new DefaultScaler( isLogLike(), lo, hi, clipScaler ) {
                    public double scaleValue( double val ) {
                        if ( val <= lo ) {
                            return loOut;
                        }
                        else if ( val >= hi ) {
                            return hiOut;
                        }
                        else if ( Double.isNaN( val ) ) {
                            return Double.NaN;
                        }
                        else {
                            return clipScaler.scaleValue( val );
                        }
                    }
                };
            }
            else if ( lo == hi ) {
                final double midVal = lo;
                double elo = midVal * 0.999;
                double ehi = midVal * 1.001;
                Scaler clipScaler = createClippedScaler( elo, ehi );
                final double loOut = clipUnit( clipScaler.scaleValue( elo ) );
                final double hiOut = clipUnit( clipScaler.scaleValue( ehi ) );
                final double midOut = clipScaler.scaleValue( midVal );
                return new DefaultScaler( isLogLike(), elo, ehi, clipScaler ) {
                    public double scaleValue( double val ) {
                        if ( val < midVal ) {
                            return loOut;
                        }
                        else if ( val > midVal ) {
                            return hiOut;
                        }
                        else if ( val == midVal ) {
                            return midVal;
                        }
                        else {
                            return Double.NaN;
                        }
                    }
                };
            }
            else {
                throw new IllegalArgumentException( "! " + lo + " < " + hi );
            }
        }

        /**
         * Ensures that a given input value is in the range 0..1.
         *
         * @param  val  input value
         * @return  val clipped to the range 0..1
         */
        private static double clipUnit( double val ) {
            return Math.min( 1.0, Math.max( 0.0, val ) );
        }
    }

    /**
     * Convenience Scaler implementation.
     *
     * @param  isLogLike  whether this scaler is log-like
     * @param  lo   lower input bound
     * @param  hi   upper input bounds
     * @param  type  discriminating object used for Equality tests;
     *               typically the Scaling is a good choice,
     *               but any object that will indicate in/equality
     *               of scalers with the same lo/hi bounds will do
     */
    private static abstract class DefaultScaler implements Scaler {
        private final boolean isLogLike_;
        private final double lo_;
        private final double hi_;
        private final Object type_;
        DefaultScaler( boolean isLogLike, double lo, double hi, Object type ) {
            isLogLike_ = isLogLike;
            lo_ = lo;
            hi_ = hi;
            type_ = type;
        }
        public boolean isLogLike() {
            return isLogLike_;
        }
        public double getLow() {
            return lo_;
        }
        public double getHigh() {
            return hi_;
        }
        @Override
        public int hashCode() {
            int code = 990;
            code = 23 * code + Float.floatToIntBits( (float) lo_ );
            code = 23 * code + Float.floatToIntBits( (float) hi_ );
            code = 23 * code + type_.hashCode();
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DefaultScaler ) {
                DefaultScaler other = (DefaultScaler) o;
                return this.lo_ == other.lo_
                    && this.hi_ == other.hi_
                    && this.type_.equals( other.type_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Scaler which uses the asinh function.
     * It starts off linear and then transitions smoothly to logarithmic.
     */
    private static class AsinhScale {

        private final double u_;
        private final double v_;

        /**
         * Constructs an AsinhScale from constraints.
         *
         * @param   max is the maximum value to be scaled
         * @param  delta is the output for <code>c = 1</code>
         * @return   new scaler
         */
        AsinhScale( double delta, double max ) {
            this( calcCoeffs( delta, max ) );
            assert scaleValue( 0 ) == 0;
            assert PlotUtil.approxEquals( delta, scaleValue( 1 ) );
            assert scaleValue( max ) <= 1;
        }

        /**
         * Constructs an Asinh scaler from calculation coefficients.
         *
         * @param   coeffs  2-element array:
         *                  scaler of output, scaler of argument
         */
        private AsinhScale( double[] coeffs ) {
            u_ = coeffs[ 0 ];
            v_ = coeffs[ 1 ];
        }

        public double scaleValue( double c ) {
            return u_ * Maths.asinh( v_ * c );
        }

        /**
         * Calculates scaling coefficients for use of the asinh function
         * given the constraints on f(1), and f^-1(1).
         *
         * @param  max  scaled values will be in the range 0..max
         * @param  delta  output for <code>c = 1</code>
         * @return  2-element array: scaler of output, scaler of argument
         */
        private static double[] calcCoeffs( double delta, double max ) {

            /* Solve v numerically for sinh(delta*asinh(v*max))-v=0
             * using Newton's method.  There may be an analytic way of
             * doing this, but I couldn't work one out. */
            double v0 = 1;
            boolean done = false;
            while ( ! done ) {
                double[] derivs = calcDerivsV( v0, max, delta );
                double v1 = v0 - derivs[ 0 ] / derivs[ 1 ];
                done = Math.abs( v1 - v0 ) < 1e-14;
                v0 = v1;
            }
            double v = v0;

            /* Calculate u from v. */
            double u = 1.0 / Maths.asinh( v * max );
            return new double[] { u, v };
        }

        /**
         * Calculate zeroth and first derivatives of the function whose root
         * is the V coefficient for Newton's method.
         *
         * @param  v  approximation for V
         * @param   max is the maximum value to be scaled
         * @param  delta is the output for <code>c = 1</code>
         * @return  2-element array (zeroth deriv, first deriv)
         */
        private static double[] calcDerivsV( double v, double max,
                                             double delta ) {
            double d0 = Maths.sinh( delta * Maths.asinh( v * max ) ) - v;
            double d1 = Maths.cosh( delta * Maths.asinh( v * max ) )
                      * delta * max / Math.hypot( v * max, 1 ) - 1;
            return new double[] { d0, d1 };
        }
    }
}
