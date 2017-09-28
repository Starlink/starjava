// The doc comments in this class are processed to produce user-visible 
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Standard arithmetic functions including things like rounding, 
 * sign manipulation, and maximum/minimum functions.
 * Phase folding operations, and a convenient form of the modulus operation
 * on which they are based, are also provided.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Arithmetic {

    /**
     * Private constructor prevents instantiation.
     */
    private Arithmetic() {
    }

    /**
     * Rounds a value up to an integer value.
     * Formally, returns the smallest (closest to negative infinity)
     * integer value that is not less than the argument.
     *
     * @param   x   a value.
     * @return  <code>x</code> rounded up
     */
    public static int roundUp( double x ) {
        return (int) Math.ceil( x );
    }

    /**
     * Rounds a value down to an integer value.
     * Formally, returns the largest (closest to positive infinity)
     * integer value that is not greater than the argument.
     *
     * @param  x  a value
     * @return  <code>x</code> rounded down
     */
    public static int roundDown( double x ) {
        return (int) Math.floor( x );
    }

    /**
     * Rounds a value to the nearest integer.
     * Formally, 
     * returns the integer that is closest in value
     * to the argument. If two integers are
     * equally close, the result is the even one.
     *
     * @param   x   a floating point value.
     * @return   <code>x</code> rounded to the nearest integer
     */
    public static int round( double x ) {
        return (int) Math.rint( x );
    }

    /**
     * Rounds a value to a given number of decimal places.
     * The result is a <code>float</code> (32-bit floating point value),
     * so this is only suitable for relatively low-precision values.
     * It's intended for truncating the number of apparent significant
     * figures represented by a value which you know has been obtained
     * by combining other values of limited precision.
     * For more control, see the functions in the <code>Formats</code> class.
     *
     * @example   <code>roundDecimal(PI,2) = 3.14f</code>
     *
     * @param  x   a floating point value
     * @param  dp  number of decimal places (digits after the decimal point)
     *         to retain
     * @return  floating point value close to <code>x</code> but with a 
     *          limited apparent precision
     */
    public static float roundDecimal( double x, int dp ) {
        double factor = Math.pow( 10, dp );
        return (float) ( Math.rint( x * factor ) / factor );
    }

    /**
     * Returns the absolute value of an integer value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     *
     * @param   x   the argument whose absolute value is to be determined
     * @return  the absolute value of the argument.
     */
    public static int abs(int x) {

        /* Math.abs(Integer.MIN_VALUE) returns Integer.MIN_VALUE.
         * Since this unlikely result is likely to be confusing for a user,
         * we return a blank result instead. */
        if ( x == Integer.MIN_VALUE ) {
            throw new NullPointerException();
        }
        return Math.abs( x );
    }

    /**
     * Returns the absolute value of a floating point value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     *
     * @param   x   the argument whose absolute value is to be determined
     * @return  the absolute value of the argument.
     */
    public static double abs(double x) {
        return Math.abs( x );
    }

    /**
     * Returns the greater of two integer values.
     * If the arguments have the same value, the result is that same value.
     *
     * <p>Multiple-argument maximum functions are also provided in the
     * <code>Arrays</code> and <code>Lists</code> packages.
     *
     * @param   a   an argument.
     * @param   b   another argument.
     * @return  the larger of <code>a</code> and <code>b</code>.
     */
    public static int max(int a, int b) {
        return Math.max( a, b );
    }

    /**
     * Returns the greater of two floating point values.
     * If the arguments have the same value, the result is that same
     * value. If either value is blank, then the result is blank.
     *
     * @param   a   an argument.
     * @param   b   another argument.
     * @return  the larger of <code>a</code> and <code>b</code>.
     */
    public static double maxNaN(double a, double b) {
        return Math.max( a, b );
    }

    /**
     * Returns the greater of two floating point values, ignoring blanks.
     * If the arguments have the same value, the result is that same value.
     * If one argument is blank, the result is the other one.
     * If both arguments are blank, the result is blank.
     *
     * <p>Multiple-argument maximum functions are also provided in the
     * <code>Arrays</code> and <code>Lists</code> packages.
     *
     * @param   a  an argument
     * @param   b  another argument
     * @return  the larger non-blank value of <code>a</code> and <code>b</code>
     */
    public static double maxReal(double a, double b) {
        if ( Double.isNaN( a ) ) {
            return b;
        }
        else if ( Double.isNaN( b ) ) {
            return a;
        }
        else {
            return Math.max( a, b );
        }
    }

    /**
     * Returns the smaller of two integer values.
     * If the arguments have the same value, the result is that same value.
     *
     * <p>Multiple-argument minimum functions are also provided in the
     * <code>Arrays</code> and <code>Lists</code> packages.
     *
     * @param   a   an argument.
     * @param   b   another argument.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     */
    public static int min(int a, int b) {
        return Math.min( a, b );
    }

    /**
     * Returns the smaller of two floating point values. 
     * If the arguments have the same value, the result is that same value.
     * If either value is blank, then the result is blank.
     *
     * @param   a   an argument.
     * @param   b   another argument.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     */
    public static double minNaN(double a, double b) {
        return Math.min( a, b );
    }

    /**
     * Returns the smaller of two floating point values, ignoring blanks.
     * If the arguments have the same value, the result is that same value.
     * If one argument is blank, the result is the other one.
     * If both arguments are blank, the result is blank.
     *
     * <p>Multiple-argument minimum functions are also provided in the
     * <code>Arrays</code> and <code>Lists</code> packages.
     *
     * @param   a   an argument
     * @param   b   another argument
     * @return  the larger non-blank value of <code>a</code> and <code>b</code>
     */
    public static double minReal( double a, double b ) {
        if ( Double.isNaN( a ) ) {
            return b;
        }
        else if ( Double.isNaN( b ) ) {
            return a;
        }
        else {
            return Math.min( a, b );
        }
    }

    /**
     * Returns the non-negative remainder of <code>a/b</code>.
     * This is a modulo operation, but differs from the expression
     * <code>a%b</code> in that the answer is always &gt;=0
     * (as long as <code>b</code> is not zero).
     *
     * @example  <code>modulo(14, 5) = 4</code>
     * @example  <code>modulo(-14, 5) = 1</code>
     * @example  <code>modulo(2.75, 0.5) = 0.25</code>
     *
     * @param  a  dividend
     * @param  b  divisor
     * @return   non-negative remainder when
     *           dividing <code>a</code> by <code>b</code>
     */
    public static double mod( double a, double b ) {
        double mod = a % b;
        return mod >= 0 ? mod : mod + Math.abs( b );
    }

    /**
     * Returns the phase of a value within a period.
     *
     * <p>For positive period, the returned value is in the range [0,1).
     *
     * @example   <code>phase(7, 4) = 0.75</code>
     * @example   <code>phase(-1000.5, 2.5) = 0.8</code>
     * @example   <code>phase(-3300, 33) = 0</code>
     *
     * @param  t  value
     * @param  period   folding period
     * @return   mod(t,period)/period
     */
    public static double phase( double t, double period ) {
        return mod( t, period ) / period;
    }

    /**
     * Returns the phase of an offset value within a period.
     * The reference value <code>t0</code> corresponds to phase zero.
     *
     * <p>For positive period, the returned value is in the range [0,1).
     *
     * @example  <code>phase(5003,100,0) = 0.03</code>
     * @example  <code>phase(5003,100,2) = 0.01</code>
     * @example  <code>phase(5003,100,4) = 0.99</code>
     *
     * @param  t  value
     * @param  period   folding period
     * @param  t0  reference value, corresponding to phase zero
     * @return   phase(t-t0, period)
     */
    public static double phase( double t, double period, double t0 ) {
        return mod( t - t0, period ) / period;
    }

    /**
     * Returns the offset phase of an offset value within a period.
     * The reference value <code>t0</code> corresponds to integer phase
     * value, and the phase offset <code>phase0</code> determines the
     * starting value for the phase range.
     *
     * <p>For positive period, the returned value is in the range
     * [<code>phase0</code>,<code>phase0+1</code>).
     *
     * @example   <code>phase(23,10,1,99) = 99.2</code>
     * @example   <code>phase(8.6125,0.2,0.0125,-0.3) = 0</code>
     * @example   <code>phase(8.6125,0.2,0.1125,-0.7) = -0.5</code>
     *
     * @param  t  value
     * @param  period   folding period
     * @param  t0   reference value, corresponding to phase zero
     * @param  phase0  offset for phase
     * @return  offset phase
     */
    public static double phase( double t, double period, double t0,
                                double phase0 ) {
        return mod( t - t0 - phase0 * period, period ) / period + phase0; 
    }
}
