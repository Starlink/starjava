// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Need threads.

/**
 * Functions for formatting numeric values.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Formats {

    /**
     * Thread-local copy of a FormatKit object.
     * It would be expensive to create new DecimalFormat objects every time
     * we needed them (probably).  But we can't use single static instances
     * of these, since they are not thread safe.
     * By having one per thread, we get the best of both worlds.
     */
    private final static ThreadLocal kitHolder_ = new ThreadLocal() {
        protected Object initialValue() {
            return new FormatKit();
        }
    };

    /**
     * Private constructor prevents instantiation.
     */
    private Formats() {
    }

    /**
     * Turns a floating point value into a string with a given number of
     * decimal places using standard settings.
     *
     * @example  <code>formatDecimal(PI,0) = "3."</code>
     * @example  <code>formatDecimal(0,10) = ".0000000000"</code>
     * @example  <code>formatDecimal(E*10,3) = "27.183"</code>
     *
     * @param   value   value to format
     * @param   dp      number of decimal places (digits after the decmal point)
     * @return  formatted string
     */
    public static String formatDecimal( double value, int dp ) {
        return getDpFormat( dp, false ).format( value );
    }

    /**
     * Turns a floating point value into a string using current locale settings.
     * For instance if language is set to French, decimal points will
     * be represented as a comma "," instead of a full stop ".".
     * Otherwise behaves the same as the corresponding 
     * <code>formatDecimal</code> function.
     *
     * @example  <code>formatDecimal(PI,0) = "3,"</code>
     * @example  <code>formatDecimal(0,10) = ",0000000000"</code>
     * @example  <code>formatDecimal(E*10,3) = "27,183"</code>
     *
     * @param   value   value to format
     * @param   dp      number of decimal places (digits after the decmal point)
     * @return  formatted string
     */
    public static String formatDecimalLocal( double value, int dp ) {
        return getDpFormat( dp, true ).format( value );
    }

    /**
     * Turns a floating point value into a formatted string using standard
     * settings.
     * The <code>format</code> string is as defined by Java's 
     * <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/text/DecimalFormat.html"
     *    ><code>java.text.DecimalFormat</code></a> class.
     *
     * @example   <code>formatDecimal(99, "#.000") = "99.000"</code>
     * @example   <code>formatDecimal(PI, "+0.##;-0.##") = "+3.14"</code>
     *
     * @param  value   value to format
     * @param  format  format specifier
     * @return formatted string
     */
    public static String formatDecimal( double value, String format ) {
        return getFormat( format, false ).format( value );
    }

    /**
     * Turns a floating point value into a formatted string using current
     * locale settings.
     * For instance if language is set to French, decimal points will
     * be represented as a comma "," instead of a full stop ".".
     * Otherwise behaves the same as the corresponding 
     * <code>formatDecimal</code> function.
     *
     * @example   <code>formatDecimal(99, "#.000") = "99,000"</code>
     * @example   <code>formatDecimal(PI, "+0.##;-0.##") = "+3,14"</code>
     *
     * @param  value   value to format
     * @param  format  format specifier
     * @return formatted string
     */
    public static String formatDecimalLocal( double value, String format ) {
        return getFormat( format, true ).format( value );
    }

    /**
     * Returns a DecimalFormat object for a given formatting specifier string.
     * The DecimalFormat is an old one if this thread has already used
     * one for the same specifier - otherwise a new one is created.
     *
     * @param  format  format specifier string
     * @return  format object
     */
    private static DecimalFormat getFormat( String format, boolean local ) {
        Map fmap = local ? getKit().localFormatMap_
                         : getKit().englishFormatMap_;
        if ( ! fmap.containsKey( format ) ) {
            DecimalFormat dfmt;
            Locale locale = local ? Locale.getDefault() : Locale.ENGLISH;
            NumberFormat nfmt = NumberFormat.getInstance( locale );
            if ( nfmt instanceof DecimalFormat ) {
                dfmt = ((DecimalFormat) nfmt);
                dfmt.applyPattern( format );
            }
            else {
                dfmt = new DecimalFormat( format );
            }
            fmap.put( format, dfmt );
        }
        return (DecimalFormat) fmap.get( format );
    }

    /**
     * Returns a DecimalFormat object which will display a given number
     * of decimal places.
     * The DecimalFormat is an old one if this thread has already used
     * one for the same specifier - otherwise a new one is created.
     *
     * @param   dp  number of digits after the decimal point
     * @return   format object
     */
    private static DecimalFormat getDpFormat( int dp, boolean local ) {
        DecimalFormat[] dpFormats = getKit().dpFormats_;
        if ( dp >= dpFormats.length ) {
            dpFormats = new DecimalFormat[ dp + 1 ];
            getKit().dpFormats_ = dpFormats;
            for ( int i = 0; i < dp + 1; i++ ) {
                String format = ".";
                for ( int j = 0; j < i; j++ ) {
                    format += "0";
                }
                dpFormats[ i ] = getFormat( format, local );
            }
        }
        return dpFormats[ dp ];
    }

    /**
     * Returns a format kit private to the calling thread.
     *
     * @return  format kit
     */
    private static FormatKit getKit() {
        return (FormatKit) kitHolder_.get();
    }

    /**
     * Resets the format kit - must be called if locale is changed for
     * local formatting methods to work correctly.  Only currently 
     * intended for testing use.
     */
    static void reset() {
        kitHolder_.set( new FormatKit() );
    }

    /**
     * Helper class which contains all the items that are potentially
     * expensive to produce but cannot be shared by different threads.
     * An instance of this class is managed by a ThreadLocal.
     */
    private static class FormatKit {
        final Map localFormatMap_ = new HashMap();
        final Map englishFormatMap_ = new HashMap();
        DecimalFormat[] dpFormats_ = new DecimalFormat[ 0 ];
    }
}
