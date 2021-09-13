package uk.ac.starlink.ttools.plot2;

import java.util.logging.Logger;

/**
 * Ranger implementation that just keeps track of high and low values.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2019
 */
public class BasicRanger implements Ranger {

    private final boolean isBestEfforts_;
    private double lo_;
    private double hi_;
    private double loPos_;
    private double hiPos_;
    private boolean hasData_;
    private boolean hasPos_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.
     *
     * @param  isBestEfforts  whether best efforts ranging should be done
     *                        without complaint for inappropriate scalings
     */
    public BasicRanger( boolean isBestEfforts ) {
        isBestEfforts_ = isBestEfforts;
        lo_ = Double.NaN;
        hi_ = Double.NaN;
        loPos_ = Double.NaN;
        hiPos_ = Double.NaN;
        hasData_ = false;
        hasPos_ = false;
    }

    public void submitDatum( double datum ) {
        if ( PlotUtil.isFinite( datum ) ) {
            if ( hasData_ ) {
                if ( datum < lo_ ) {
                    lo_ = datum;
                }
                else if ( datum > hi_ ) {
                    hi_ = datum;
                }
            }
            else {
                hasData_ = true;
                lo_ = datum;
                hi_ = datum;
            }
            if ( datum > 0.0 ) {
                if ( hasPos_ ) {
                    if ( datum < loPos_ ) {
                        loPos_ = datum;
                    }
                    else if ( datum > hiPos_ ) {
                        hiPos_ = datum;
                    }
                }
                else {
                    hasPos_ = true;
                    loPos_ = datum;
                    hiPos_ = datum;
                }
            }
        }
    }

    public void add( Ranger other ) {
        BasicRanger bother = (BasicRanger) other;
        submitDatum( bother.lo_ );
        submitDatum( bother.hi_ );
        submitDatum( bother.loPos_ );
        submitDatum( bother.hiPos_ );
    }

    public Ranger createCompatibleRanger() {
        return new BasicRanger( isBestEfforts_ );
    }

    public Span createSpan() {
        return new BasicSpan( isBestEfforts_, lo_, hi_, loPos_, hiPos_ );
    }

    /**
     * Returns a 2-element array giving definite lower and upper bounds
     * based on known lower and upper values.  The upper bound will be
     * strictly greater than the lower bound.  Optionally, both bounds
     * can be required to be strictly greater than zero.
     * If the input values are insufficient to determine such return values,
     * some reasonable defaults will be made up.
     *
     * @param   lo  input lower bound, may be NaN
     * @param   hi  input upper bound, may be NaN
     * @param   isPositive  if true, output bounds must be positive
     * @return   2-element array giving (lo,hi)
     */
    public static double[] calculateFiniteBounds( double lo, double hi,
                                                  boolean isPositive ) {
        lo = isPositive && lo <= 0 ? Double.NaN : lo;
        hi = isPositive && hi <= 0 ? Double.NaN : hi;
        if ( lo > hi ) {
            throw new IllegalArgumentException();
        }
        else if ( lo < hi ) {
            return new double[] { lo, hi };
        }
        else if ( lo == hi ) {
            return isPositive ? new double[] { lo * 0.9, hi * 1.1 }
                              : new double[] { lo - 1, hi + 1 };
        }
        else if ( ! Double.isNaN( hi ) ) {
            return isPositive ? new double[] { hi * 0.001, hi }
                              : new double[] { hi - 1, hi };
        }
        else if ( ! Double.isNaN( lo ) ) {
            return isPositive ? new double[] { lo, lo * 1000 }
                              : new double[] { lo, lo + 1 };
        }
        else {
            return isPositive ? new double[] { 1, 10 }
                              : new double[] { 0, 1 };
        }
    }

    /**
     * Returns a scaler based on a RangeScaling.
     *
     * @param  rscaling  scaling of range type
     * @param  dataclip  input data range adjustment
     * @param  span    data range information
     * @return   new scaler
     */
    public static Scaler createRangeScaler( Scaling.RangeScaling rscaling,
                                            Subrange dataclip, Span span ) {
        double[] bounds = span.getFiniteBounds( rscaling.isLogLike() );
        double lo = bounds[ 0 ];
        double hi = bounds[ 1 ];
        Scaler scaler0 = rscaling.createScaler( lo, hi );
        if ( Subrange.isIdentity( dataclip ) ) {
            return scaler0;
        }
        else {
            double sublo =
                Scalings.unscale( scaler0, lo, hi, dataclip.getLow() );
            double subhi =
                Scalings.unscale( scaler0, lo, hi, dataclip.getHigh() );
            double[] bounds1 = sublo < subhi
                             ? new double[] { sublo, subhi }
                             : new double[] { subhi, sublo };
            return rscaling.createScaler( bounds1[ 0 ], bounds1[ 1 ] );
        }
    }

    /**
     * Span implementation for use with BasicRanger.
     */
    private static class BasicSpan implements Span {

        final boolean isBestEfforts_;
        final double lo_;
        final double hi_;
        final double loPos_;
        final double hiPos_;

        /**
         * Constructor.
         *
         * @param  isBestEfforts  whether best efforts ranging should be done
         *                        without complaint for inappropriate scalings
         * @param  lo   lowest known value
         * @param  hi   highest known value
         * @param  loPos  lowest known positive definite value
         * @param  hiPos  highest known positive definite value
         */
        BasicSpan( boolean isBestEfforts,
                   double lo, double hi, double loPos, double hiPos ) {
            isBestEfforts_ = isBestEfforts;
            lo_ = lo;
            hi_ = hi;
            loPos_ = loPos;
            hiPos_ = hiPos;
            assert ! ( loPos_ > hiPos_ );
        }

        public double getLow() {
            return lo_;
        }

        public double getHigh() {
            return hi_;
        }

        public double[] getFiniteBounds( boolean isPositive ) {
            return calculateFiniteBounds( isPositive ? loPos_ : lo_,
                                          isPositive ? hiPos_ : hi_,
                                          isPositive );
        }

        public Scaler createScaler( Scaling scaling, Subrange dataclip ) {
            final Scaling.RangeScaling rscaling;
            if ( scaling instanceof Scaling.RangeScaling ) {
                rscaling = (Scaling.RangeScaling) scaling;
            }
            else {
                if ( ! isBestEfforts_ ) {
                    String msg = "Inadequate span " + this
                               + " used for scaling " + scaling;
                    assert false : msg;
                    logger_.warning( msg );
                }
                rscaling = scaling.isLogLike() ? Scaling.LOG : Scaling.LINEAR;
            }
            return createRangeScaler( rscaling, dataclip, this );
        }

        public Span limit( double lo, double hi ) {
            if ( lo > hi ) {
                throw new IllegalArgumentException( "Bad range: "
                                                  + lo + " .. " + hi );
            }
            double lo1 = lo_;
            double hi1 = hi_;
            double loPos1 = loPos_;
            double hiPos1 = hiPos_;
            if ( ! Double.isNaN( lo ) && ! Double.isInfinite( lo ) ) {
                lo1 = lo;
                if ( ! ( lo <= 0.0 ) ) {
                    loPos1 = lo;
                }
                if ( hi1 < lo_ ) {
                    hi1 = lo1;
                    hiPos1 = loPos1;
                }
            }
            if ( ! Double.isNaN( hi ) && ! Double.isInfinite( hi ) &&
                 ! ( hi < lo1 ) ) {
                hi1 = hi;
                if ( ! ( hi <= 0.0 ) ) {
                    hiPos1 = hi;
                }
                if ( lo_ > hi1 ) {
                    lo1 = hi1;
                    loPos1 = hiPos1;
                }
            }
            return new BasicSpan( isBestEfforts_, lo1, hi1, loPos1, hiPos1 );
        }

        @Override
        public int hashCode() {
            int code = 22348;
            code = 23 * code + Float.floatToIntBits( (float) lo_ );
            code = 23 * code + Float.floatToIntBits( (float) hi_ );
            code = 23 * code + Float.floatToIntBits( (float) loPos_ );
            code = 23 * code + Float.floatToIntBits( (float) hiPos_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof BasicSpan ) {
                BasicSpan other = (BasicSpan) o;
                return PlotUtil.doubleEquals( this.lo_, other.lo_ )
                    && PlotUtil.doubleEquals( this.hi_, other.hi_ )
                    && PlotUtil.doubleEquals( this.loPos_, other.loPos_ )
                    && PlotUtil.doubleEquals( this.hiPos_, other.hiPos_ );
            }
            else {
                return false;
            }
        }
    }
}
