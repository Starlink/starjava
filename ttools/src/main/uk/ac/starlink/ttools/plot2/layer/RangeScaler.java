package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot.Range;

/**
 * Scales values to the interval 0..1.  Values outside the input range
 * are clipped.  NaN values stay NaN.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2013
 */
public abstract class RangeScaler {

    /**
     * Scale input value to unit interval.
     *
     * @param   dataValue  input data value
     * @return  scaled value which will be either in the range 0..1 or NaN
     */
    public abstract double scale( double dataValue );

    /**
     * Creates a new scaler instance from a Range.
     *
     * @param   logFlag  false for linear scaling, true for logarithmic
     * @param   flipFlag  false for forward sense, true for inverted
     * @param   range   data range which will be mapped to the interval 0..1
     * @return  new range scaler
     */
    public static RangeScaler createScaler( boolean logFlag, boolean flipFlag,
                                            Range range ) {
        double[] bounds = range.getFiniteBounds( logFlag );
        return createScaler( logFlag, flipFlag, bounds[ 0 ], bounds[ 1 ] );
    }

    /**
     * Creates a new scaler instance from explicit bounds.
     *
     * @param   logFlag  false for linear scaling, true for logarithmic
     * @param   flipFlag  false for forward sense, true for inverted
     * @param   lo   lower bound of data range
     * @param   hi   upper bound of data range
     * @return  new range scaler
     * @throws  IllegalArgumentException  unless hi &gt;= lo
     */
    public static RangeScaler createScaler( boolean logFlag, boolean flipFlag,
                                            final double lo, final double hi ) {
        if ( lo == hi ) {
            return new RangeScaler() {
                public double scale( double value ) {
                    if ( value < lo ) {
                        return 0;
                    }
                    else if ( value > hi ) {
                        return 1;
                    }
                    else if ( Double.isNaN( value ) ) {
                        return Double.NaN;
                    }
                    else {
                        assert value == lo;
                        assert value == hi;
                        return 0.5;
                    }
                }
            };
        }
        if ( ! ( hi > lo ) ) {
            throw new IllegalArgumentException();
        }
        if ( logFlag ) {
            final double base1 = 1.0 / ( flipFlag ? hi : lo );
            final double scale1 =
                1.0 / ( Math.log( flipFlag ? lo / hi : hi / lo ) );
            return new RangeScaler() {
                public double scale( double value ) {
                    if ( value <= lo ) {
                        return 0;
                    }
                    else if ( value >= hi ) {
                        return 1;
                    }
                    else {
                        return Math.log( value * base1 ) * scale1;
                    }
                }
            };
        }
        else {
            final double base = flipFlag ? hi : lo;
            final double scale1 = 1.0 / ( flipFlag ? lo - hi : hi - lo );
            return new RangeScaler() {
                public double scale( double value ) {
                    if ( value <= lo ) {
                        return 0;
                    }
                    else if ( value >= hi ) {
                        return 1;
                    }
                    else {
                        return ( value - base ) * scale1;
                    }
                }
            };
        }
    }
}
