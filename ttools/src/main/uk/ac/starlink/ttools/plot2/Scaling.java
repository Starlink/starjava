package uk.ac.starlink.ttools.plot2;

import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.func.Maths;
import uk.ac.starlink.ttools.plot.Range;

/**
 * Defines a policy for scaling values to a fixed interval.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2015
 */
@Equality
public abstract class Scaling {

    private final String name_;
    private final String description_;
    private final boolean isLogLike_;

    /** Linear scaling. */
    public static final Scaling LINEAR = createLinearScaling( "Linear" );

    /** Logarithmic scaling. */
    public static final Scaling LOG = createLogScaling( "Log" );

    /** Square root scaling. */
    public static final Scaling SQRT = createSqrtScaling( "Sqrt" );

    /** Square scaling. */
    public static final Scaling SQUARE = createSquareScaling( "Square" );

    /** Asinh-based scaling with default parameters. */
    public static final Scaling AUTO = createAutoScaling( "Auto" );

    private static final double AUTO_DELTA = 0.0625;
    private static final double UNSCALE_TOL = 0.0001;
    private static final double UNSCALE_MAXIT = 50;
    private static final Scaling[] STRETCHES = new Scaling[] {
        LOG, LINEAR, SQRT, SQUARE,
    };
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Constructor.
     *
     * @param  name  scaling name
     * @param  description  short description of scaling rule
     * @param  isLogLike  whether the scaling is logarithmic,
     *                    for instance should be displayed on a log axis
     */
    protected Scaling( String name, String description, boolean isLogLike ) {
        name_ = name;
        description_ = description;
        isLogLike_ = isLogLike;
    }

    /**
     * Returns the name of this scaling.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description of this scaling.
     *
     * @return  short text description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Indicates whether this scaling is logarithmic.
     * If so, it should be displayed on logarithmic axis,
     * and can't cope with negative values.
     * 
     * @return   true for basically logarithmic,
     *           false of (perhaps distorted) linear
     */
    public boolean isLogLike() {
        return isLogLike_;
    }

    /**
     * Returns a scaler instance that can scale input values in a given range.
     * The given bounds define the range of input values that will be
     * mapped to the fixed (0..1) output range.  Input values outside
     * that range will in general result in clipping, so for the
     * returned scaler <code>s</code>:
     * <pre>
     *    s.scaleValue(x) == s.scaleValue(lo) for x&lt;lo
     *    s.scaleValue(x) == s.scaleValue(hi) for x&gt;hi
     * </pre>
     *
     * @param  lo  lower bound of unclipped input data value
     * @param  hi  upper bound of unclipped input data value
     * @return  instance
     */
    public abstract Scaler createScaler( double lo, double hi );

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns a list of standard options for colour map stretch.
     *
     * @return  standard stretch options
     */
    public static final Scaling[] getStretchOptions() {
        return STRETCHES.clone();
    }

    /**
     * Adjusts a scaling by applying a fractional subrange to the
     * scaler inputs <em>before</em> scaling is applied,
     * so that the input range is subranged, rather than the output range.
     * For linear scaling the two things would be the same,
     * but not in general.
     *
     * @param  scaling  base scaling
     * @param  subrange   fractional subrange to apply to input values
     * @return  subranged scaling
     */
    public static Scaling subrangeScaling( Scaling scaling,
                                           Subrange subrange ) {
        return Subrange.isIdentity( subrange )
             ? scaling
             : new SubrangeScaling( scaling, subrange );
    }

    /**
     * Utility method to return a scaler based on a Range object.
     *
     * @param  scaling  scaling
     * @param  range  value range
     * @return   scaler
     */
    public static Scaler createRangeScaler( Scaling scaling, Range range ) {
        double[] bounds = range.getFiniteBounds( scaling.isLogLike() );
        return scaling.createScaler( bounds[ 0 ], bounds[ 1 ] );
    }

    /**
     * Constructs the linear scaling instance.
     *
     * @param  name  scaling name
     * @return  linear scaling
     */
    private static Scaling createLinearScaling( String name ) {
        return new ClippedScaling( name, "Linear scaling", false ) {
            public Scaler createClippedScaler( final double lo, double hi ) {
                final double scale = 1.0 / ( hi - lo );
                return new Scaler() {
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
    private static Scaling createLogScaling( String name ) {
        return new ClippedScaling( name, "Logarithmic scaling", true ) {
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
                return new Scaler() {
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
     * This is based on the Asinh function, parameterised such that the
     * difference between output colours at the bottom end of the range
     * should be visible.
     *
     * @param  name  scaling name
     * @return  auto scaling
     */
    private static Scaling createAutoScaling( String name ) {
        final Scaling asinh = createAsinhScaling( "Asinh-auto", AUTO_DELTA );
        final double minSpan = 1.0 / AUTO_DELTA + 1;
        return new Scaling( name,
                            "asinh-based scaling with default parameters",
                            false ) {
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
    private static Scaling createSqrtScaling( String name ) {
        return new ReScaling( name, "Square root scaling", LINEAR,
                              new Scaler() {
                                  public double scaleValue( double val ) {
                                      return Math.sqrt( val );
                                  }
                              } );
    }

    /**
     * Constructs the squre scaling instance.
     *
     * @param  name  scaling name
     * @return  square scaling
     */
    private static Scaling createSquareScaling( String name ) {
        return new ReScaling( name, "Square scaling", LINEAR,
                              new Scaler() {
                                  public double scaleValue( double val ) {
                                      return val * val;
                                  }
                              } );
    }

    /**
     * Constructs an asinh-based scaling.
     *
     * @param   name  scaling name
     * @param  delta  output difference for lower-end input unit difference
     * @return  scaling
     */
    public static Scaling createAsinhScaling( String name, double delta ) {
        String descrip = "asinh-based scaling in which a unit difference "
                       + "at the bottom of the input scale "
                       + "translates to a difference of " + delta + " " 
                       + "in the output";
        return new AsinhScaling( name, descrip, delta );
    }

    /**
     * Performs the inverse operation of Scaler.scaleValue.
     *
     * @param  scaler   scaler instance
     * @param  lo       lower bound of input data value
     * @param  hi       upper bound of input data value
     * @param  frac    required output value of scaleValue method,
     *                 must be in range 0..1
     * @return   value x that causes scaler.scaleValue(x)
     *           to return <code>frac</code>
     */
    static double unscale( Scaler scaler, double lo, double hi, double frac ) {

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
            final Scaler zScaler = new AsinhScaler( delta_, hi - lo );
            return new Scaler() {
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
    private static class ReScaling extends Scaling {
        private final Scaling baseScaling_;
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
                          Scaling baseScaling, Scaler rescaler ) {
            super( name, description, baseScaling.isLogLike() );
            baseScaling_ = baseScaling;
            rescaler_ = rescaler;
        }

        public Scaler createScaler( double lo, double hi ) {
            final Scaler baseScaler = baseScaling_.createScaler( lo, hi );
            return new Scaler() {
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
     * Applies a fixed subrange to the input values on scaling.
     */
    private static class SubrangeScaling extends Scaling {
        private final Scaling baseScaling_;
        private final Subrange subrange_;

        /**
         * Constructor.
         *
         * @param   baseScaling  base scaling
         * @param   subrange  subrange to apply to input values before scaling
         */
        SubrangeScaling( Scaling baseScaling, Subrange subrange ) {
            super( baseScaling.getName() + "-sub",
                   baseScaling.getDescription() + ", subrange: " + subrange,
                   baseScaling.isLogLike() );
            baseScaling_ = baseScaling;
            subrange_ = subrange;
        }

        public Scaler createScaler( double lo, double hi ) {
            Scaler fullScaler = baseScaling_.createScaler( lo, hi );
            double subLo = unscale( fullScaler, lo, hi, subrange_.getLow() );
            double subHi = unscale( fullScaler, lo, hi, subrange_.getHigh() );
            return subLo < subHi ? baseScaling_.createScaler( subLo, subHi )
                                 : baseScaling_.createScaler( subHi, subLo );
        }

        @Override
        public int hashCode() {
            int code = 688923;
            code = 23 * code + baseScaling_.hashCode();
            code = 23 * code + subrange_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SubrangeScaling ) {
                SubrangeScaling other = (SubrangeScaling) o;
                return this.baseScaling_.equals( other.baseScaling_ )
                    && this.subrange_.equals( other.subrange_ );
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
    private static abstract class ClippedScaling extends Scaling {

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
                return new Scaler() {
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
                return new Scaler() {
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
     * Scaler which uses the asinh function.
     * It starts off linear and then transitions smoothly to logarithmic.
     */
    private static class AsinhScaler implements Scaler {

        private final double u_;
        private final double v_;

        /**
         * Constructs an AsinhScaler from constraints.
         *
         * @param   max is the maximum value to be scaled
         * @param  delta is the output for <code>c = 1</code>
         * @return   new scaler
         */
        AsinhScaler( double delta, double max ) {
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
        private AsinhScaler( double[] coeffs ) {
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
