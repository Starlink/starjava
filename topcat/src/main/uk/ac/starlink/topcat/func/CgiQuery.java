package uk.ac.starlink.topcat.func;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    /**
     * Constructs a CGI query with no arguments.
     * 
     * @param  base  base part of the CGI URL (the bit before the '?')
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
            switch ( c ) {
                case ' ':
                case '%':
                case '?':
                case '&':
                    sbuf_.append( '%' )
                         .append( Integer.toHexString( (int) c ) );
                    break;
                default:
                    sbuf_.append( c );
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

    /**
     * Sends this query and writes the result to a temporary file.
     *
     * @param  extension  file extension of file to write to
     * @return  file to which query result was written
     */
    public File executeAsLocalFile( String extension ) throws IOException {
        File file = File.createTempFile( "tcQuery", extension );
        file.deleteOnExit();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream( toURL().openStream() );
            out = new BufferedOutputStream( new FileOutputStream( file ) );
            for ( int b; ( b = in.read() ) >= 0; ) {
                out.write( b );
            }
        }
        finally {
            if ( in != null ) {
                in.close();
            }
            if ( out != null ) {
                out.close();
            }
        }
        return file;
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
