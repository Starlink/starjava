package uk.ac.starlink.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class for constructing CGI query strings.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class CgiQuery {

    private final StringBuffer sbuf_;
    private int narg;

    /** Legal characters for query part of a URI - see RFC 2396. */
    private final static String QUERY_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "0123456789" +
        "-_.!~*'()";

    /**
     * Constructs a CGI query with no arguments.
     * 
     * @param  base  base part of the CGI URL (the bit before the '?')
     * @throws  IllegalArgumentException  if <tt>base</tt> is not a legal
     *          base URL
     */
    public CgiQuery( String base ) {
        try {
            new URL( base );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Not a url: " + base )
                 .initCause( e );
        }
        if ( base.indexOf( '?' ) >= 0 ) {
            throw new IllegalArgumentException( "\"" + base + 
                                                "\" already contains a '?'" );
        }
        sbuf_ = new StringBuffer( base );
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
        return addArgument( name, Double.toString( value ) );
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
        sbuf_.append( narg++ == 0 ? '?' : '&' )
             .append( name )
             .append( '=' );
        for ( int i = 0; i < value.length(); i++ ) {
            char c = value.charAt( i );
            if ( QUERY_CHARS.indexOf( c ) >= 0 ) {
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
        return this;
    }

    /**
     * Returns this query as a URL.
     *
     * @return  query URL
     */
    public URL toURL() {
        try {
            return new URL( sbuf_.toString() );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError(); // I think, since base is a URL
        }
    }

    public boolean equals( Object o ) {
        return o instanceof CgiQuery 
            && o.toString().equals( toString() );
    }

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
}
