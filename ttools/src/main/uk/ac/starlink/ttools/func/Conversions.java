// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.DoubleList;

/**
 * Functions for converting between strings and numeric values.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Conversions {

    private static final Pattern INT_REGEX =
        Pattern.compile( "(?<![-+.0-9a-zA-Z])"
                       + "[+-]?([0-9]+)"
                       + "(?![-+.0-9a-zA-Z])" );
     
    private static final Pattern FLOAT_REGEX =
        Pattern.compile( "(?<![-+.0-9a-zA-Z])"
                       +  "([+-]?([0-9]*\\.[0-9]+|[0-9]+\\.?)"
                       +  "([eE][-+]?[0-9]{1,3})?"
                       +  "|none"
                       +  "|null"
                       +  "|nan"
                       +  ")"
                       + "(?![-+.0-9a-zA-Z])",
                         Pattern.CASE_INSENSITIVE );

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
     * Turns a byte value into a string.
     *
     * @param  byteVal  byte numeric value
     * @return  a string representation of <code>byteVal</code>
     */
    public static String toString( byte byteVal ) {
        // In the absence of this method evaluating the expression
        // "toString(byteVal)" gives a CompilationException:
        //    Ambiguity detected between "toString(double)" and "toString(char)"
        //    on invocation "toString(byte)".
        return Byte.toString( byteVal );
    }

    /**
     * Turns a boolean value into a string.
     *
     * @param  booleanVal  boolean value (true or false)
     * @return  a string representation of <code>booleanVal</code>
     *          ("<code>true</code>" or "<code>false</code>")
     */
    public static String toString( boolean booleanVal ) {
        return Boolean.toString( booleanVal );
    }

    /**
     * Turns any object value into a string.
     * As applied to existing string values this isn't really useful,
     * but it means that you can apply <code>toString</code>
     * to any object value without knowing its type
     * and get a useful return from it.
     *
     * @param  objVal  non-primitive value
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
     * Attempts to interpret a string as an array of integer values.
     * An ad-hoc algorithm is used that tries to extract a list of
     * integers from a string; a comma- or space-separated list of
     * integer values will work, and other formats may or may not.
     *
     * <p>The details of this function's behaviour may change
     * in future releases.
     *
     * @example <code>parseInts("9 8 -23") = [9, 8, -23]</code>
     * @example <code>parseInts("tiddly-pom") = []</code>
     *
     * @param  str  string containing a list of integer values
     * @return  array of integer values
     */
    public static int[] parseInts( String str ) {
        IntList ilist = new IntList( 48 );
        Matcher matcher = INT_REGEX.matcher( str );
        while ( matcher.find() ) {
            String match = str.substring( matcher.start(), matcher.end() );
            try {
                ilist.add( Integer.parseInt( match ) );
            }
            catch ( NumberFormatException e ) {
                // not integer after all
            }
        }
        return ilist.toIntArray();
    }

    /**
     * Attempts to interpret a string as an array of floating point values.
     * An ad-hoc algorithm is used that tries to extract a list of
     * numeric values from a string; a comma- or space-separated list of
     * floating point values will work, and other formats may or may not.
     *
     * <p>This function can be used as a hacky way to extract the
     * numeric values from an STC-S
     * (for instance ObsCore/EPNcore <code>s_region</code>) string.
     *
     * <p>The details of this function's behaviour may change
     * in future releases.
     *
     * @example <code>parseDoubles("1.3, 99e1, NaN, -23")
     *              = [1.3, 990.0, NaN, -23.0]</code>
     * @example <code>parseDoubles("Polygon ICRS 0.8 2.1 9.0 2.1 6.2 8.6")
     *              = [0.8, 2.1, 9.0, 2.1, 6.2, 8.6]</code>
     * @example <code>parseDoubles("La la la") = []</code>
     *
     * @param  str  string containing a list of floating point values
     * @return  array of floating point values
     */
    public static double[] parseDoubles( String str ) {
        DoubleList dlist = new DoubleList( 48 );
        Matcher matcher = FLOAT_REGEX.matcher( str );
        while ( matcher.find() ) {
            String match = str.substring( matcher.start(), matcher.end() );
            char c1 = match.charAt( 0 );
            if ( c1 == 'n' || c1 == 'N' ) {
                assert "nan".equalsIgnoreCase( match ) ||
                       "none".equalsIgnoreCase( match ) ||
                       "null".equalsIgnoreCase( match );
                dlist.add( Double.NaN );
            }
            else {
                try {
                    dlist.add( Double.parseDouble( match ) );
                }
                catch ( NumberFormatException e ) {
                    // not numeric after all
                }
            }
        }
        return dlist.toDoubleArray();
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
