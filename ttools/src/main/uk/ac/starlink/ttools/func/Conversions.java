// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import uk.ac.starlink.table.Tables;

/**
 * Functions for converting between strings and numeric values.
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
     * @param  fpVal  floating point numeric value
     * @return  a string representation of <code>fpVal</code>
     */
    public static String toString( double fpVal ) {
        if ( Tables.isBlank( fpVal ) ) {
            return null;
        }
        else {
            return fpVal == (double) (long) fpVal
                 ? Long.toString( (long) fpVal )
                 : Double.toString( fpVal );
        }
    }

    /**
     * Turns an integer numeric value into a string.
     *
     * @param  intVal  integer numeric value
     * @return  a string representation of <code>intVal</code>
     */
    public static String toString( long intVal ) {
        return Long.toString( intVal );
    }

    /**
     * Turns a single character value into a string.
     *
     * @param  charVal  character numeric value
     * @return  a string representation of <code>charVal</code>
     */
    public static String toString( char charVal ) {
        return Character.toString( charVal );
    }

    /**
     * Turns any value into a string.
     * As applied to existing string values this isn't really useful,
     * but it means that you can apply <code>toString()</code>
     * to any value without knowing its type
     * and get a useful return from it.
     *
     * @param  objVal  non-numeric value
     * @return  a string representation of <code>objVal</code>
     */
    public static String toString( Object objVal ) {
        return Tables.isBlank( objVal ) ? null : objVal.toString();
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
        return Byte.parseByte( str.trim() );
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
        return Short.parseShort( str.trim() );
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
        return Integer.parseInt( str.trim() );
    }

    /**
     * Attempts to interpret a string as a long (64-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <code>str</code>
     */
    public static long parseLong( String str ) {
        return Long.parseLong( str.trim() );
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
        return Float.parseFloat( str.trim() );
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
        return Double.parseDouble( str.trim() );
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

    /**
     * Converts the integer argument to hexadecimal form.
     *
     * @example  <code>toHex(42) = "2a"</code>
     *
     * @param  value  integer value
     * @return  hexadecimal representation of <code>value</code>
     */
    public static String toHex( long value ) {
        return Long.toHexString( value );
    }

    /**
     * Converts a string representing a hexadecimal number to its
     * integer value.
     *
     * @example   <code>fromHex("2a") = 42</code>
     *
     * @param  hexVal  hexadecimal representation of value
     * @return   integer value represented by <code>hexVal</code>
     */
    public static int fromHex( String hexVal ) {
        return Integer.parseInt( hexVal.trim(), 16 );
    }
}
