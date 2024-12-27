package uk.ac.starlink.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for constructing CGI query strings.
 * The name is somewhat out of date or misleading;
 * the intention is to provide URLs with a query part that can be
 * interpreted as an application/x-www-form-urlencoded string.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class CgiQuery {

    private final StringBuffer sbuf_;
    private String extraChars_;
    private int narg;

    /** Legal characters for query part of a URI - see RFC 2396. */
    public static final String QUERY_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "0123456789" +
        "-_.!~*'()";

    /**
     * Constructs a CGI query.
     * The submitted <code>base</code> argument may optionally be a
     * partially-formed CGI-query, that is, one ending in a '?'
     * and zero or more '&amp;name=value' pairs.
     * 
     * @param  base  base part of the CGI URL
     * @throws  IllegalArgumentException  if <code>base</code> is not a legal
     *          base URL
     */
    public CgiQuery( String base ) {
        base = base.trim();
        try {
            URLUtils.newURL( base );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Not a url: " + base )
                 .initCause( e );
        }
        sbuf_ = new StringBuffer( base );
        extraChars_ = "";
    }
  
    /**
     * Provides a list of characters that will be passed as parameter or
     * value content through without percent-encoding.
     * The default behaviour is quite conservative,
     * but in many cases this can be relaxed without causing trouble.
     *
     * @param  extraChars   string each of whose characters
     *                      will not be percent-encoded
     */
    public void allowUnencodedChars( String extraChars ) {
        extraChars_ = extraChars;
    }

    /**
     * Adds an integer argument to this query.
     * For convenience the return value is this query.
     *
     * @param  name  argument name
     * @param  value  value for the argument
     * @return  this query
     */
    public CgiQuery addArgument( String name, long value ) {
        return addArgument( name, Long.toString( value ) );
    }

    /**
     * Adds a floating point argument to this query.
     * For convenience the return value is this query.
     *
     * @param  name  argument name
     * @param  value  value for the argument
     * @return  this query
     */
    public CgiQuery addArgument( String name, double value ) {
        return addArgument( name, formatDouble( value ) );
    }

    /**
     * Adds a single-precision floating point argument to this query.
     * For convenience the return value is this query.
     *
     * @param  name  argument name
     * @param  value  value for the argument
     * @return  this query
     */
    public CgiQuery addArgument( String name, float value ) {
        return addArgument( name, formatFloat( value ) );
    }

    /**
     * Adds a string argument to this query.
     * For convenience the return value is this query.
     *
     * @param  name  argument name
     * @param  value  unescaped value for the argument
     * @return  this query
     */
    public CgiQuery addArgument( String name, String value ) {
        if ( narg++ == 0 ) {
            char lastChar = sbuf_.charAt( sbuf_.length() - 1 );
            if ( lastChar != '?' && lastChar != '&' ) {
                sbuf_.append( sbuf_.indexOf( "?" ) >= 0 ? '&' : '?' );
            }
        }
        else {
            sbuf_.append( '&' );
        }
        sbuf_.append( name )
             .append( '=' );
        if ( value != null ) {
            for ( int i = 0; i < value.length(); i++ ) {
                char c = value.charAt( i );
                if ( QUERY_CHARS.indexOf( c ) >= 0 ||
                     extraChars_.indexOf( c ) >= 0 ) {
                    sbuf_.append( c );
                }
                else if ( c >= 0x10 && c <= 0x7f ) {
                    sbuf_.append( '%' )
                         .append( Integer.toHexString( (int) c ) );
                }
                else {
                    throw new IllegalArgumentException( "Bad character in \"" +
                                                        value + "\"" );
                }
            }
        }
        return this;
    }

    /**
     * Returns this query as a URL.
     *
     * @return  query URL
     */
    public URL toURL() {
        try {
            return URLUtils.newURL( sbuf_.toString() );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalStateException( e );
        }
    }

    @Override
    public boolean equals( Object o ) {
        return o instanceof CgiQuery 
            && o.toString().equals( toString() );
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns this query as a string.
     *
     * @return  query string
     */
    public String toString() {
        return sbuf_.toString();
    }

    /**
     * Formats a double precision value.
     *
     * @param   value  value
     * @return   string representation
     */
    public static String formatDouble( double value ) {
        return formatDouble( value, 16, 32 );
    }

    /**
     * Formats a single precision value.
     *
     * @param   value  value
     * @return   string representation
     */
    public static String formatFloat( float value ) {
        return formatDouble( (double) value, 7, 32 );
    }

    /**
     * Formats a floating point value.
     * It will be done in fixed point format if it can be done within the
     * given number of characters, else exponential notation.
     *
     * @param   value  value
     * @param   nsf   number of significant figures
     * @param   maxleng  maximum length of string - if longer than this, 
     *          will return to exponential notation
     * @return  fixed format string representation
     */
    public static String formatDouble( double value, int nsf, int maxleng ) {
        String sval = Double.toString( value );
        if ( sval.indexOf( 'E' ) < 0 ) {
            return sval;
        }
        else {
            int log10 = log10( value );
            StringBuffer fbuf = new StringBuffer( "0." );
            for ( int i = 0; i < nsf - log10; i++ ) {
                fbuf.append( '0' );
            }
            DecimalFormat format = new DecimalFormat( fbuf.toString() );
            format.setDecimalFormatSymbols( new DecimalFormatSymbols( Locale
                                                                     .US ) );
            String fval = format.format( value );
            fval = fval.replaceFirst( "0+$", "" );
            if ( fval.length() <= maxleng ) {
                return fval;
            }
            else {
                return sval;
            }
        }
    }

    /**
     * Returns approximate logarithm to base 10 of the value.
     *
     * @param  value  value
     * @return  approximate log to base 10
     */
    private static int log10( double value ) {
        return (int) 
               Math.round( Math.log( Math.abs( value ) ) / Math.log( 10 ) );
    }
}
