package uk.ac.starlink.topcat.func;

/**
 * Contains functions for coverting between strings and numeric values.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Conversions {

    /**
     * Turns a numeric value into a string.
     *
     * @param  value  numeric value
     * @return  a string representation of <tt>value</tt>
     */
    public static String toString( double value ) {
        return Double.toString( value );
    }

    /**
     * Attempts to interpret a string as a byte (8-bit signed integer) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <tt>str>
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
     * @return  byte value of <tt>str>
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
     * @return  byte value of <tt>str>
     */
    public static int parseInt( String str ) {
        return Integer.parseInt( str );
    }

    /**
     * Attempts to interpret a string as a float (32-bit floating point) value.
     * If the input string can't be interpreted in this way, a blank 
     * value will result.
     *
     * @param  str  string containing numeric representation
     * @return  byte value of <tt>str>
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
     * @return  byte value of <tt>str>
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
     * @return  value converted to type byte
     */
    public byte toByte( double value ) {
        if ( value < Byte.MIN_VALUE || value > Byte.MAX_VALUE ) {
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
     * @return  value converted to type short
     */
    public short toShort( double value ) {
        if ( value < Short.MIN_VALUE || value > Byte.MAX_VALUE ) {
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
     * @return  value converted to type int
     */
    public int toInteger( double value ) {
        if ( value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ) {
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
     * @return  value converted to type long 
     */
    public long toLong( double value ) {
        if ( value < Long.MIN_VALUE || value > Long.MAX_VALUE ) {
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
     * @return  value converted to type float 
     */
    public float toFloat( double value ) {
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
     * If it is out of range, a blank value will result.
     *
     * @param  value  numeric value for conversion
     * @return  value converted to type double 
     */
    public double toDouble( double value ) {
        return value;
    }

}
