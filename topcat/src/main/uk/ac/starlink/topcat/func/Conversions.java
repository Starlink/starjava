// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Functions for coverting between strings and numeric values.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Conversions {

    /**
     * Private constructor prevents instantiation.
     */
    private Conversions() {
    }

    /**
     * Turns a numeric value into a string.
     *
     * @param  value  numeric value
     * @return  a string representation of <code>value</code>
     */
    public static String toString( double value ) {
        return value == (double) (long) value ? Long.toString( (long) value )
                                              : Double.toString( value );
    }

    /**
     * Attempts to interpret a string as a byte (8-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static byte parseByte( String str ) {
        return Byte.parseByte( str );
    }

    /**
     * Attempts to interpret a string as a short (16-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static short parseShort( String str ) {
        return Short.parseShort( str );
    }

    /**
     * Attempts to interpret a string as an int (32-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static int parseInt( String str ) {
        return Integer.parseInt( str );
    }

    /**
     * Attempts to interpret a string as a long (32-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static long parseLong( String str ) {
        return Long.parseLong( str );
    }

    /**
     * Attempts to interpret a string as a float (32-bit floating point) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static float parseFloat( String str ) {
        return Float.parseFloat( str );
    }

    /**
     * Attempts to interpret a string as a double (64-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static double parseDouble( String str ) {
        return Double.parseDouble( str );
    }

    /**
     * Attempts to convert the numeric argument to a
     * byte (8-bit signed integer) result.
     * If it is out of range, a blank value will result.
     *
     * @param  value  numeric value for conversion
     * @return  <code>value</code> converted to type byte
     */
    public static byte toByte( double value ) {
        if ( value < Byte.MIN_VALUE || 
             value > Byte.MAX_VALUE ||
             Double.isNaN( value ) ) {
            throw new NumberFormatException();
        }
        else {
            return (byte) value;
        }
    }

    /**
     * Attempts to convert the numeric argument to a
     * short (16-bit signed integer) result.
     * If it is out of range, a blank value will result.
     *
     * @param  value  numeric value for conversion
     * @return  <code>value</code> converted to type short
     */
    public static short toShort( double value ) {
        if ( value < Short.MIN_VALUE || 
             value > Short.MAX_VALUE ||
             Double.isNaN( value ) ) {
            throw new NumberFormatException();
        }
        else {
            return (short) value;
        }
    }

    /**
     * Attempts to convert the numeric argument to an
     * int (32-bit signed integer) result.
     * If it is out of range, a blank value will result.
     *
     * @param  value  numeric value for conversion
     * @return  <code>value</code> converted to type int
     */
    public static int toInteger( double value ) {
        if ( value < Integer.MIN_VALUE ||
             value > Integer.MAX_VALUE ||
             Double.isNaN( value ) ) {
            throw new NumberFormatException();
        }
        else {
            return (int) value;
        }
    }

    /**
     * Attempts to convert the numeric argument to a
     * long (64-bit signed integer) result.
     * If it is out of range, a blank value will result.
     *
     * @param  value  numeric value for conversion
     * @return  <code>value</code> converted to type long 
     */
    public static long toLong( double value ) {
        if ( value < Long.MIN_VALUE ||
             value > Long.MAX_VALUE ||
             Double.isNaN( value ) ) {
            throw new NumberFormatException();
        }
        else {
            return (long) value;
        }
    }

    /**
     * Attempts to convert the numeric argument to a
     * float (32-bit floating point) result.
     * If it is out of range, a blank value will result.
     *
     * @param  value  numeric value for conversion
     * @return  <code>value</code> converted to type float 
     */
    public static float toFloat( double value ) {
        if ( value < -Float.MAX_VALUE || value > Float.MAX_VALUE ) {
            return Float.NaN;
        }
        else {
            return (float) value;
        }
    }

    /**
     * Converts the numeric argument to a
     * double (64-bit signed integer) result.
     *
     * @param  value  numeric value for conversion
     * @return  <code>value</code> converted to type double 
     */
    public static double toDouble( double value ) {
        return value;
    }
}
