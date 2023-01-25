package uk.ac.starlink.ttools.plot;

import java.util.Arrays;

/**
 * Provides round numbers.
 * Static instances are provided for rounding numbers to be used in a
 * linear or a logarithmic context.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2005
 */
public abstract class Rounder {

    /** Number rounder for linear scaling.  All values are &gt;0. */
    public static final Rounder LINEAR = new LinearRounder();

    /** Number rounder for logarithmic scaling.  All values are &gt;1. */
    public static final Rounder LOG = new LogRounder();

    /** Number rounder for time intervals in seconds.  All values are &gt;0. */
    public static final Rounder TIME_SECOND = new SecondRounder();

    private static final double LOG10 = Math.log( 10. );

    /**
     * Returns a round number near the given value.
     *
     * @param  value  input value
     * @return  round number near <code>value</code>
     */
    public abstract double round( double value );

    /**
     * Returns the next round number larger than the given value.
     *
     * @param  value  input value
     * @return  round number a bit larger than <code>value</code>
     */
    public abstract double nextUp( double value );

    /**
     * Returns the next round number smaller than the given value.
     *
     * @param  value  input value
     * @return   round number a bit smaller than the given value
     */
    public abstract double nextDown( double value );

    /**
     * Implements rounding for a linear context.
     */
    private static class LinearRounder extends Rounder {

        private final double[] roundMantissas_ = new double[] {
            1.0, 2.0, 2.5, 5.0, 10.0,
        };

        private LinearRounder() {
            /* check */
            double[] roundMantissas = roundMantissas_.clone();
            Arrays.sort( roundMantissas );
            if ( roundMantissas[ 0 ] != 1.0 ||
                 roundMantissas[ roundMantissas.length - 1 ] != 10.0 ) {
                throw new AssertionError();
            }
        }

        public double round( double value ) {
            return getNearestNumber( value ).getValue();
        }

        public double nextUp( double value ) {
            RoundNumber num = getNearestNumber( value * 1.01 );
            double value1 = num.getValue();
            if ( value1 > value ) {
                return value1;
            }
            else {
                int index = Arrays.binarySearch( roundMantissas_,
                                                 num.mantissa_ );
                assert roundMantissas_[ index ] == num.mantissa_;
                if ( ++index > roundMantissas_.length ) {
                    num.multiplier_ *= 10.;
                    index = 1;
                }
                num.mantissa_ = roundMantissas_[ index ];
                return num.getValue();
            }
        }

        public double nextDown( double value ) {
            RoundNumber num = getNearestNumber( value * 0.99 );
            double value1 = num.getValue();
            if ( value1 < value ) {
                return value1;
            }
            else {
                int index = Arrays.binarySearch( roundMantissas_,
                                                 num.mantissa_ );
                assert roundMantissas_[ index ] == num.mantissa_;
                if ( --index < 0 ) {
                    num.multiplier_ *= 0.1;
                    index = roundMantissas_.length - 2;
                }
                num.mantissa_ = roundMantissas_[ index ];
                return num.getValue();
            }
        }

        /**
         * Returns RoundNumber object close to a given value.
         *
         * @param  value  numeric starting value
         * @return   RoundNumber near <code>value</code>
         */
        private RoundNumber getNearestNumber( double value ) {
            if ( value <= 0.0 ) {
                throw new IllegalArgumentException( value + 
                                                  " < 0 (out of range)" );
            }
            double exponent = Math.floor( Math.log( value ) / LOG10 );
            double multiplier = Math.pow( 10, exponent );
            double mantissa = value / multiplier;
            assert mantissa >= 0.999 && mantissa <= 10.001;
            double roundedMantissa;
            for ( int i = 1; i < roundMantissas_.length; i++ ) {
                double r0 = roundMantissas_[ i - 1 ];
                double r1 = roundMantissas_[ i ];
                if ( mantissa >= r0 && mantissa <= r1 ) {
                    double d0 = mantissa - r0;
                    double d1 = r1 - mantissa;
                    return new RoundNumber( d0 < d1 ? r0 : r1, multiplier );
                }
            }
            assert false : new RoundNumber( mantissa, multiplier );
            return new RoundNumber( 1.0, multiplier );
        }

        /**
         * Encapsulates a round number for LinearRounder class.
         */
        private static class RoundNumber {
            double mantissa_;
            double multiplier_;
            RoundNumber( double mantissa, double multiplier ) {
                mantissa_ = mantissa;
                multiplier_ = multiplier;
            }
            double getValue() {
                return mantissa_ * multiplier_;
            }
            public String toString() {
                return mantissa_ + "*" + multiplier_;
            }
        }
    }

    /**
     * Implements number rounding for a logarithmic context.
     * The implementation of this is a bit suboptimal at the moment,
     * gaps should probably be larger faster for value >> 1.
     */
    private static class LogRounder extends Rounder {

        public double round( double value ) {
            return getNearestNumber( value ).getValue();
        }

        public double nextUp( double value ) {
            RoundNumber num = getNearestNumber( value + Double.MIN_VALUE );
            while ( num.getValue() <= value ) {
                num.label_++;
            }
            return num.getValue();
        }

        public double nextDown( double value ) {
            RoundNumber num = getNearestNumber( value - Double.MIN_VALUE );
            while ( num.getValue() >= value ) {
                num.label_--;
            }
            return num.getValue();
        }

        /**
         * Returns RoundNumber object close to a given value.
         *
         * @param  value  numeric starting value
         * @return   RoundNumber near <code>value</code>
         */
        private RoundNumber getNearestNumber( double value ) {
            if ( value <= 1.0 ) {
                throw new IllegalArgumentException( value + " <= 1" );
            }
            else if ( value >= 1.75 ) {
                return new RoundNumber( (int) Math.round( value ) );
            }
            else {
                RoundNumber num = new RoundNumber( 2 );
                while ( value < num.getValue() ) {
                    num.label_--;
                }
                return num;
            }
        }

        /**
         * Encapsulates a round number for LogRounder class.
         */
        private static class RoundNumber {
            int label_;
            RoundNumber( int label ) {
                label_ = label;
            }
            double getValue() {
                return label_ > 1 ? (double) label_
                                  : 1. + Math.pow( 2., label_ - 2 );
            }
        }
    }

    /**
     * Implements number rounding for linear units of seconds.
     * Round numbers are human-friendly units, like multiples of
     * minutes, hours, years etc.
     */
    private static class SecondRounder extends Rounder {
        private static final long SECOND = 1;
        private static final long MIN = 60;
        private static final long HOUR = 60 * MIN;
        private static final long DAY = 24 * HOUR;
        private static final long WEEK = 7 * DAY;
        private static final long YEAR = 365 * DAY + DAY / 4;
        private final double[] periods_;
        private final int np_;
        private final double pmin_;
        private final double pmax_;

        /**
         * Constructor.
         */
        public SecondRounder() {
            double[] periods = new double[] {
                1, 2, 5, 10, 30,
                MIN, 2 * MIN, 5 * MIN, 10 * MIN, 15 * MIN, 30 * MIN,
                HOUR, 2 * HOUR, 4 * HOUR, 6 * HOUR, 12 * HOUR,
                DAY, 2 * DAY, 4 * DAY, WEEK, 2 * WEEK,
                YEAR / 12, YEAR / 4, YEAR / 2,
                YEAR, 2 * YEAR, 5 * YEAR, 10 * YEAR,
            };
            periods_ = periods.clone();
            np_ = periods_.length;
            pmin_ = periods[ 0 ];
            pmax_ = periods[ np_ - 1 ];
            Arrays.sort( periods_ );
            assert Arrays.equals( periods_, periods );
        }

        public double nextUp( double value ) {
            int pt = Arrays.binarySearch( periods_, value );
            if ( pt == np_ - 1 || pt == - (np_ + 1) ) {
                assert value >= pmax_;
                return YEAR * LINEAR.nextUp( value / YEAR );
            }
            else if ( pt == -1 ) {
                assert value < pmin_;
                return SECOND * LINEAR.nextUp( value / SECOND );
            }
            else if ( pt >= 0 ) {
                assert value >= pmin_ && value <= pmax_;
                return periods_[ pt + 1 ];
            }
            else {
                assert value > pmin_ && value < pmax_;
                return periods_[ - pt - 1 ];
            }
        }

        public double nextDown( double value ) {
            int pt = Arrays.binarySearch( periods_, value );
            if ( pt == 0 || pt == -1 ) {
                assert value <= pmin_;
                return SECOND * LINEAR.nextDown( value / SECOND );
            }
            else if ( pt == - (np_ + 1) ) {
                assert value > pmax_;
                return YEAR * LINEAR.nextDown( value / YEAR );
            }
            else if ( pt > 0 ) {
                assert value >= pmin_ && value <= pmax_;
                return periods_[ pt - 1 ];
            }
            else {
                assert value > pmin_ && value < pmax_;
                return periods_[ - pt - 2 ];
            }
        }

        public double round( double value ) {
            double up = nextUp( value );
            double down = nextDown( value );
            return Math.log( up ) + Math.log( down ) > 2 * Math.log( value )
                 ? down : up;
        }
    }
}
