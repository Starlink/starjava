// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.Tables;

/**
 * String manipulation and query functions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Strings {

    private static Map patterns = new HashMap();

    /**
     * Private constructor prevents instantiation.
     */
    private Strings() {
    }

    /**
     * Concatenates multiple values into a string.
     * In some cases the same effect can be achieved by
     * writing <code>s1+s2+...</code>, but this method makes sure that
     * values are converted to strings, with the blank value invisible.
     *
     * @example  <code>concat("blue", "moon") = "bluemoon"</code>
     * @example  <code>concat("1", 2, 3, "4") = "1234"</code>
     * @example  <code>concat("Astro", null, "Physics") = "AstroPhysics"</code>
     *
     * @param  strings  one or more strings
     * @return  concatenation of input strings, without separators
     */
    public static String concat( Object... strings ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Object s : strings ) {
            if ( s != null ) {
                sbuf.append( s );
            }
        }
        return sbuf.toString();
    }

    /**
     * Joins multiple values into a string, with a given
     * separator between each pair.
     *
     * @example  <code>join("&lt;-&gt;", "alpha", "beta", "gamma")
     *                 = "alpha&lt;-&gt;beta&lt;-&gt;gamma"</code>
     * @example  <code>join(" ", 1, "brown", "mouse")
     *                 = "1 brown mouse"</code>
     *
     * @param  separator  string to insert between adjacent words
     * @param  words   one or more values to join
     * @return  input values joined together with <code>separator</code>
     */
    public static String join( String separator, Object... words ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < words.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( separator );
            }
            sbuf.append( words[ i ] );
        }
        return sbuf.toString();
    }

    /**
     * Determines whether two strings are equal.
     * Note you should use this function instead of <code>s1==s2</code>,
     * which can (for technical reasons) return false even if the
     * strings are the same.
     *
     * @param s1  first string
     * @param s2  second string
     * @return  true if s1 and s2 are both blank, or have the same content
     */
    public static boolean equals( String s1, String s2 ) {
        boolean b1 = Tables.isBlank( s1 );
        boolean b2 = Tables.isBlank( s2 );
        if ( b1 && b2 ) {
            return true;
        }
        else if ( b1 || b2 ) {
            return false;
        }
        else {
            return s1.equals( s2 );
        }
    }

    /**
     * Determines whether two strings are equal apart from possible
     * upper/lower case distinctions.
     *
     * @example   <code>equalsIgnoreCase("Cygnus", "CYGNUS") = true</code>
     * @example   <code>equalsIgnoreCase("Cygnus", "Andromeda") = false</code>
     *
     * @param s1  first string
     * @param s2  second string
     * @return  true if s1 and s2 are both blank, or have the same content
     *          apart from case folding
     */
    public static boolean equalsIgnoreCase( String s1, String s2 ) {
        boolean b1 = Tables.isBlank( s1 );
        boolean b2 = Tables.isBlank( s2 );
        if ( b1 && b2 ) {
            return true;
        }
        else if ( b1 || b2 ) {
            return false;
        }
        else {
            return s1.equalsIgnoreCase( s2 );
        }
    }

    /**
     * Determines whether a string starts with a certain substring.
     *
     * @example  <code>startsWith("CYGNUS X-1", "CYG") = true</code>
     *
     * @param  whole  the string to test
     * @param  start  the sequence that may appear at the start of 
     *                <code>whole</code>
     * @return true if the first few characters of <code>whole</code> are
     *              the same as <code>start</code>
     */
    public static boolean startsWith( String whole, String start ) {
        return whole != null && whole.startsWith( start );
    }

    /**
     * Determines whether a string ends with a certain substring.
     *
     * @example  <code>endsWith("M32", "32") = true</code>
     *
     * @param  whole  the string to test
     * @param  end    the sequence that may appear at the end of 
     *                <code>whole</code>
     * @return true if the last few characters of <code>whole</code> are
     *              the same as <code>end</code>
     */
    public static boolean endsWith( String whole, String end ) {
        return whole != null && whole.endsWith( end );
    }

    /**
     * Determines whether a string contains a given substring.
     *
     * @example   <code>contains("Vizier", "izi") = true</code>
     *
     * @param   whole  the string to test
     * @param   sub   the sequence that may appear within <code>whole</code>
     * @return  true   if the sequence <code>sub</code> appears within 
     *                 <code>whole</code>
     */
    public static boolean contains( String whole, String sub ) {
        return whole != null && whole.indexOf( sub ) >= 0;
    }

    /**
     * Returns the length of a string in characters.
     *
     * @example  <code>length("M34") = 3</code>
     *
     * @param   str  string
     * @return  number of characters in <code>str</code>
     */
    public static int length( String str ) {
        return str == null ? 0 : str.length();
    }

    /**
     * Splits a string into an array of space-separated words.
     * One or more spaces separates each word from the next.
     * Leading and trailing spaces are ignored.
     *
     * <p>The result is an array of strings, and if you want to use the
     * individual elements you need to use square-bracket indexing,
     * with <code>[0]</code> representing the first object
     *
     * @example  <code>split("211:54:01 +29:33:41")</code>
     *           gives a 2-element array,
     *           first element is <code>"211:54:01"</code> and
     *           second element is <code>"+29:33:41"</code>.
     * @example  <code>split("  cat  dog  cow ")[1] = "dog"</code>
     *
     * @param   words   string with embedded spaces delimiting the words
     * @return  array of the separate words;
     *          you can extract the individual words from the result using
     *          square bracket indexing
     */
    public static String[] split( String words ) {
        return trim( words ).split( " +" );
    }

    /**
     * Splits a string into an array of words separated by a given
     * regular expression.
     *
     * <p>The result is an array of strings, and if you want to use the
     * individual elements you need to use square-bracket indexing,
     * with <code>[0]</code> representing the first object
     *
     * @example  <code>split("cat, dog, cow", ", *")</code>
     *           gives a 3-element string array.
     * @example  <code>split("23.0, 45.92", ", ")[0] = "23.0"</code>
     * @example  <code>parseDouble(split("23.0, 45.92", ", ")[0]) = 23</code>
     *
     * @param  words  string with multiple parts
     * @param  regex  regular expression delimiting the different words in
     *                the <code>words</code> parameter
     * @return  array of the separate words;
     *          you can extract the individual words from the result using
     *          square bracket indexing
     */
    public static String[] split( String words, String regex ) {
        return words.split( regex );
    }

    /**
     * Tests whether a string matches a given regular expression.
     *
     * @example  <code>matches("Hubble", "ub") = true</code>
     *
     * @param  str  string to test
     * @param  regex  regular expression string
     * @return  true if <code>regex</code> matches <code>str</code> anywhere
     */
    public static boolean matches( String str, String regex ) {
        return str != null && getPattern( regex ).matcher( str ).find();
    }

    /**
     * Returns the first grouped expression matched in a string defined
     * by a regular expression.  A grouped expression is one enclosed
     * in parentheses.
     *
     * @example <code>matchGroup("NGC28948b","NGC([0-9]*)") = "28948"</code>
     *
     * @param  str  string to match against
     * @param  regex  regular expression containing a grouped section
     * @return  contents of the matched group 
     *          (or null, if <code>regex</code> didn't match <code>str</code>)
     */
    public static String matchGroup( String str, String regex ) {
        if ( str != null ) {
            Matcher matcher = getPattern( regex ).matcher( str );
            if ( matcher.find() ) {
                return matcher.group( 1 );
            }
        }
        return null;
    }

    /**
     * Replaces the first occurrence of a regular expression in a string with
     * a different substring value.
     *
     * @example  
     *     <code>replaceFirst("Messier 61", "Messier ", "M-") = "M-61"</code>
     *
     * @param  str  string to manipulate
     * @param  regex  regular expression to match in <code>str</code>
     * @param  replacement  replacement string
     * @return  same as <code>str</code>, but with the first match (if any) of 
     *          <code>regex</code> replaced by <code>replacement</code>
     */
    public static String replaceFirst( String str, String regex, 
                                       String replacement ) {
        return str == null 
             ? null
             : getPattern( regex ).matcher( str )
              .replaceFirst( replacement == null ? "" : replacement );
    }

    /**
     * Replaces all occurrences of a regular expression in a string with
     * a different substring value.
     *
     * @example <code>replaceAll("1-2--3---4","--*","x") = "1x2x3x4"</code>
     *
     * @param  str  string to manipulate
     * @param  regex  regular expression to match in <code>str</code>
     * @param  replacement  replacement string
     * @return  same as <code>str</code>, but with all matches of 
     *          <code>regex</code> replaced by <code>replacement</code>
     */
    public static String replaceAll( String str, String regex, 
                                     String replacement ) {
        return str == null 
             ? null
             : getPattern( regex ).matcher( str )
              .replaceAll( replacement == null ? "" : replacement );
    }

    /**
     * Returns the last part of a given string.
     * The substring begins with the character at the specified
     * index and extends to the end of this string.
     *
     * @example  <code>substring("Galaxy", 2) = "laxy"</code>
     *
     * @param  str  the input string
     * @param  startIndex  the beginning index, inclusive
     * @return  last part of <code>str</code>, omitting the first 
     *          <code>startIndex</code> characters
     */
    public static String substring( String str, int startIndex ) {
        return str == null || str.length() < startIndex 
             ? null 
             : str.substring( startIndex );
    }

    /**
     * Returns a substring of a given string.
     * The substring begins with the character at <code>startIndex</code>
     * and continues to the character at index <code>endIndex-1</code>
     * Thus the length of the substring is <code>endIndex-startIndex</code>.
     *
     * @example   <code>substring("Galaxy", 2, 5) = "lax"</code>
     *
     * @param  str  the input string
     * @param  startIndex the beginning index, inclusive
     * @param  endIndex  the end index, inclusive
     * @return   substring of <code>str</code>
     */
    public static String substring( String str, int startIndex, int endIndex ) {
        if ( str == null ) {
            return null;
        }
        else {
            int leng = str.length();
            return str.substring( Math.min( startIndex, leng ),
                                  Math.min( endIndex, leng ) );
        }
    }

    /**
     * Returns an uppercased version of a string.
     *
     * @example  <code>toUpperCase("Universe") = "UNIVERSE"</code>
     *
     * @param  str  input string
     * @return   uppercased version of <code>str</code>
     */
    public static String toUpperCase( String str ) {
        return str == null ? null : str.toUpperCase();
    }

    /**
     * Returns an lowercased version of a string.
     *
     * @example  <code>toLowerCase("Universe") = "universe"</code>
     *
     * @param  str  input string
     * @return   lowercased version of <code>str</code>
     */
    public static String toLowerCase( String str ) {
        return str == null ? null : str.toLowerCase();
    }

    /**
     * Trims whitespace from both ends of a string.
     *
     * @example  <code>trim("  some text  ") = "some text"</code>
     * @example  <code>trim("some     text") = "some     text"</code>
     * 
     * @param  str input string
     * @return   str with any spaces trimmed from start and finish
     */
    public static String trim( String str ) {
        return str == null ? null : str.trim();
    }

    /**
     * Takes an integer argument and returns a string representing the
     * same numeric value but padded with leading zeros to a specified
     * length.
     *
     * @example  <code>padWithZeros(23,5) = "00023"</code>
     *
     * @param  value  numeric value to pad
     * @param  ndigit   the number of digits in the resulting string
     * @return  a string evaluating to the same as <code>value</code> with
     *          at least <code>ndigit</code> characters
     */
    public static String padWithZeros( long value, int ndigit ) {
        String sval = Long.toString( value );
        int sl = sval.length();
        if ( sl < ndigit ) {
            char[] cbuf = new char[ ndigit - sl ];
            Arrays.fill( cbuf, '0' );
            sval = new String( cbuf ) + sval;
        }
        return sval;
    }

    /**
     * Returns a pattern for a given regular expression.
     * It caches patterns already used to avoid having to compile each time.
     *
     * @param  regex  regular expression
     * @return  pattern for <code>regex</code>
     */
    private static Pattern getPattern( String regex ) {
        Pattern pat = (Pattern) patterns.get( regex );
        if ( pat == null ) {
            pat = Pattern.compile( regex );
            patterns.put( regex, pat );
        }
        return pat;
    }

}
