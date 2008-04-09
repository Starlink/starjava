// The doc comments in this class are processed to produce user-visible 
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Standard arithmetic functions including things like rounding, 
 * sign manipulation, and maximum/minimum functions.
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
    public static double max(double a, double b) {
        return Math.max( a, b );
    }

    /**
     * Returns the greater of two floating point values, ignoring blanks.
     * If the arguments have the same value, the result is that same value.
     * If one argument is blank, the result is the other one.
     * If both arguments are blank, the result is blank.
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
    public static double min(double a, double b) {
        return Math.min( a, b );
    }

    /**
     * Returns the smaller of two floating point values, ignoring blanks.
     * If the arguments have the same value, the result is that same value.
     * If one argument is blank, the result is the other one.
     * If both arguments are blank, the result is blank.
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
}
