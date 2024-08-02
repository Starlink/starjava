package uk.ac.starlink.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides convenience methods for resolving URLs.
 * This class provides some static methods for turning strings into URLs.
 * This tends to be a bit of a pain in java, since you have to watch
 * out for MalformedURLExceptions all over and work out what the
 * context is.  The methods provided here assume that if a string 
 * looks like a URL it is one, if it doesn't it's a file name, and
 * if it's not absolute or resolved against a given context 
 * it is relative to the current directory.
 * From the point of view of a user providing text to an application,
 * or an XML document providing an href, this is nearly always 
 * what is wanted.  The strategy can lead to surprising situations
 * in the case that wacky URL protocols are used; for instance if 
 * <code>makeURL</code> is called on the string "gftp://host/file" and
 * no gftp handler is installed, it will be interpreted as a file-protocol
 * URL referring to the (presumably non-existent) file "gftp://host/file".
 * In this case the eventual upshot will presumably be a file-not-found
 * type error rather than a MalformedURLException type error getting
 * presented to the user.  Users of this class should be of the opinion
 * that this is not a particularly bad thing.
 * <p>
 * The <code>systemId</code> strings used by {@link javax.xml.transform.Source}s
 * have similar semantics to the strings which this class converts
 * to URLs or contexts.
 * <p>
 * This class assumes that the "file:" protocol is legal for URLs, 
 * and will throw AssertionErrors if this turns out not to be the case.
 * 
 * @author   Mark Taylor (Starlink)
 * @author   Norman Gray (Starlink)
 */
public class URLUtils {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /* Set up a URL representing the default context (the current directory). */
    private static final URI defaultContext;
    static {
        URI dc;
        try {
            dc = new URI( "file:." );
        }
        catch ( URISyntaxException e ) {
            assert false;
            dc = null;
        }
        defaultContext = dc;
    }
    private static final Pattern FILE_URL_REGEX =
        Pattern.compile( "(file:)(/*)(.*)" );

    /**
     * Private constructor prevents instantiation.
     */
    private URLUtils() {
    }

    /**
     * Drop-in replacement for the deprecated
     * {@link java.net.URL#URL(String)} constructor.
     *
     * <p>All URL constructors are deprecated since Java 20
     * because of issues with parsing and validation.
     * This utility method provides a way for code to avoid deprecation
     * warnings.  It may not do much to solve the underlying problems,
     * and might introduce some new ones, but code that is having problems
     * here can be adapted to handle URL creation more carefully;
     * such approaches, according to the JDK documentation, should generally
     * be URI-based.  Other utility methods may be added here in future
     * as required.
     *
     * <p>As far as I can tell, most of the difficulties arising with
     * URL parsing that have led to the deprecation relate to relatively
     * strange URLs, so that "normal" http/https/file-protocol URL strings
     * passed to this method should behave the same as if passed
     * to the deprecated constructor.
     * However, there may be changes of behaviour when it comes to
     * constructions like embedded spaces in paths or special characters
     * in query parts etc.
     *
     * <p>Note that passing a string to this method which is not a valid URI,
     * for instance because it contains unescaped illegal characters like "[",
     * will fail, unlike the call to <code>new URL()</code>.
     * In such cases a <code>MalformedURLException</code> will be thrown
     * (which is really the result of a <code>URISyntaxException</code>).
     *
     * @param  spec  textual representation of URL
     * @return  URL, not null
     * @throws MalformedURLException  in case of syntax error
     */
    public static URL newURL( String spec ) throws MalformedURLException {
        URI uri;
        try {
            uri = new URI( spec );
        }
        catch ( URISyntaxException e ) {
            throw (MalformedURLException)
                  new MalformedURLException( "Bad URI: " + spec )
                 .initCause( e );
        }
        if ( uri.isAbsolute() ) {
            return uri.toURL();
        }
        else {
            throw new MalformedURLException( "No scheme for URL: " + spec );
        }
    }

    /**
     * Obtains a URL from a string.  If the String has the form of a URL,
     * it is turned directly into a URL.  If it does not, it is treated as
     * a filename, and turned into a file-protocol URL.  In the latter
     * case a relative or absolute filename may be used.  If it is 
     * null or a blank string (or something else equally un-filename like?)
     * then null is returned.
     *
     * @param   location   a string representing the location of a resource
     * @return  a URL representing the location of the resource
     */
    public static URL makeURL( String location ) {
        if ( location == null || location.trim().length() == 0 ) {
            return null;
        }
        try {
            URI uri = new URI( location );
            if ( uri.isAbsolute() ) {
                return uri.toURL();
            }
        }
        catch ( URISyntaxException | MalformedURLException e ) {
        }

        /* It's not a valid URI or doesn't have a valid URL scheme,
         * interpret it as a filename. */
        try {
            return new URI( "file:" + location ).toURL();
        }
        catch ( MalformedURLException | URISyntaxException e ) {
            return null;
        }
    }

    /**
     * Obtains a URL from a string in a given context.
     * The string <code>context</code> is turned into a URL as per 
     * the {@link #makeURL(String)} method, unless it is null or
     * the empty string, in which case it is treated as a reference
     * to the current directory.
     * The string <code>location</code> is then turned into a URL in
     * the same way as using {@link #makeURL(String)}, except that
     * if it represents a relative path it is resolved in the context
     * of <code>context</code>, taking its protocol and/or relative position
     * from it.
     *
     * @param   context   a string representing the context within which
     *                    <code>location</code> is to be resolved
     * @param   location   a string representing the location of a resource
     * @return  a URL representing the location of the resource
     */
    public static URL makeURL( String context, String location ) {
        URI contextURI;
        if ( context == null || context.trim().length() == 0 ) {
            contextURI = defaultContext;
        }
        else {
            try {
                URL contextURL = makeURL( context );
                contextURI = contextURL != null ? contextURL.toURI() : null;
            }
            catch ( URISyntaxException e ) {
                contextURI = null;
            }
        }
        if ( contextURI != null ) {
            URI locURI;
            try {
                locURI = new URI( location );
            }
            catch ( URISyntaxException e ) {
                locURI = null;
            }
            String loc;
            if ( locURI != null && locURI.isAbsolute() &&
                 locURI.getScheme().equals( contextURI.getScheme() ) ) {
                String locFrag = locURI.getFragment();
                loc = locURI.getSchemeSpecificPart()
                    + ( locFrag == null ? "" : ( "#" + locFrag ) );
            }
            else {
                loc = location;
            }
            try {
                return contextURI.resolve( loc ).toURL();
            }
            catch ( MalformedURLException | IllegalArgumentException e ) {
            }
        }
        return makeURL( location );
    }

    /**
     * Returns an Error which can be thrown when you can't make a URL even
     * though you know you're using the "file:" protocol.  Although this
     * is permitted by the URL class, we consider ourselves to be on
     * an irretrievably broken system if it happens.
     */
    private static AssertionError 
            protestFileProtocolIsLegal( MalformedURLException e ) {
        AssertionError ae = 
            new AssertionError( "Illegal \"file:\" protocol in URL??" );
        ae.initCause( e );
        return ae;
    }

    /**
     * Turns a URL into a URI.
     *
     * <p>Since URIs are syntactically and semantically a superset of
     * URLs, this conversion should not cause any errors.  If,
     * however, the input URL is malformed in rather extreme ways,
     * then the URI construction will fail.  These ways include (but
     * are not necesssarily limited to) the features discussed in
     * {@link java.net.URI#URI(String,String,String,String,String)},
     * namely that a scheme is present, but with a relative path, or
     * that it has a registry-based authority part.
     *
     * <p>Because of the way the class does the conversion, the method
     * will itself resolve some malformations of URLs.  You should not rely
     * on this, however, firstly because the method might in principle
     * change, but mostly because you should avoid creating such
     * malformed URLs in the first place.
     *
     * <p>The most common source of malformed URLs is that of
     * <code>file</code> URLs which have inadequately escaped
     * (windows) drive letters or spaces in the name: such URLs should
     * be constructed using the {@link java.io.File#toURI} or {@link
     * java.io.File#toURL} methods.  Such URLs will be escaped by
     * this method.
     *
     * @param url a URL to be converted.  If this is null, then the
     *        method returns null 
     * @return the input URL as a URI, or null if the input was null
     * @throws MalformedURLException if the URI cannot be constructed
     *        because the input URL turns out to be malformed
     */
    public static URI urlToUri( URL url ) 
            throws MalformedURLException {
        /*
         * Weaknesses: this method doesn't cope with URIs which have
         * a scheme plus a relative path, or registry-based authorities.
         * Ought it to?  In the absence of specific use-cases,
         * probably not, but we should note that this might be
         * reasonable and be prepared to revisit it.
         */

        if (url == null)
            return null;

        try {
            return new URI(url.getProtocol(),
                           url.getAuthority(),
                           url.getPath(),
                           url.getQuery(),
                           url.getRef() // ie, fragment
                           );
        } catch (java.net.URISyntaxException e) {
            // The input URL was malformed, so indicate that
            MalformedURLException newEx
                    = new MalformedURLException("URL " + url
                                                + " was malformed");
            newEx.initCause(e);
            throw newEx;
        }
    }

    /**
     * Constructs a legal URL for a given File.
     * Unlike java, this gives you a URL which conforms to RFC1738 and
     * looks like "<code>file://localhost/abs-path</code>" rather than 
     * "<code>file:abs-or-rel-path</code>".
     *
     * @param   file   file
     * @return   URL
     * @see   "RFC 1738"
     */
    public static URL makeFileURL( File file ) {
        try {
            return fixURL( file.toURI().toURL() );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError();
        }
    }

    /**
     * Fixes file: URLs which don't have enough slashes in them.
     * Java generates invalid URLs of the form 
     * "<code>file:abs-or-rel-path</code>"
     * when it should generate "<code>file://localhost/abs-path</code>".
     *
     * @param   url  input URL
     * @return  fixed URL
     * @see   "RFC 1738"
     */
    public static URL fixURL( URL url ) {
        Matcher matcher = FILE_URL_REGEX.matcher( url.toString() );
        if ( matcher.matches() ) {
            String scheme = matcher.group( 1 );
            String slashes = matcher.group( 2 );
            String path = matcher.group( 3 );
            assert "file:".equals( scheme );
            try {
                switch ( slashes.length() ) {
                    case 0:
                        return fixURL( new File( path ).getAbsoluteFile()
                                                       .toURI().toURL() );
                    case 1:
                        return new URI( scheme + "//localhost" 
                                               + slashes + path ).toURL();
                    case 2:
                        return url;
                    default:
                        return url;
                }
            }
            catch ( MalformedURLException | URISyntaxException e ) {
                assert false;
                return url;
            }
        }
        else {
            return url;
        }
    }

    /**
     * Attempts to determine whether two URLs refer to the same resource.
     * Not likely to be foolproof, but slightly smarter than using 
     * <code>equals</code>.
     *
     * @param  url1  first URL
     * @param  url2  second URL
     * @return   true if <code>url1</code> and <code>url2</code> appear to
     *           refer to the same resource
     */
    public static boolean sameResource( URL url1, URL url2 ) {
        if ( url1 == null && url2 == null ) {
            return true;
        }
        else if ( url1 == null || url2 == null ) {
            return false;
        }
        else if ( url1.equals( url2 ) ) {
            return true;
        }
        else if ( url1.getProtocol().equals( "file" ) &&
                  url2.getProtocol().equals( "file" ) ) {
            String[] strings = { url1.toString(), url2.toString() };
            for ( int i = 0; i < 2; i++ ) {
                strings[ i ] =
                    strings[ i ].replaceFirst( "^file:/*(localhost)?/*", "" );
            }
            return strings[ 0 ].equals( strings[ 1 ] );
        }
        else {
            return false;
        }
    }

    /**
     * Locates the local file, if any, represented by a URL.
     * If the URL string uses the "file:" protocol, and has no query or anchor
     * parts, the filename will be extracted and the corresponding file
     * returned.  Otherwise, null is returned.
     *
     * @param   url  URL string
     * @return   local file referenced by <code>url</code>, or null
     */
    public static File urlToFile( String url ) {
        if ( url == null || url.trim().length() == 0 ) {
            return null;
        }
        URI u;
        try {
            u = new URI( url );
        }
        catch ( URISyntaxException e ) {
            return null;
        }
        if ( "file".equals( u.getScheme() ) && u.getRawFragment() == null
                                            && u.getRawQuery() == null ) {
            String path = u.getPath();
            String filename = File.separatorChar == '/'
                            ? path
                            : path.replace( '/', File.separatorChar );
            return new File( filename );
        }
        return null;
    }

    /**
     * Compares two URLs.  This does approximatly the same job as
     * the URL.equals() method, but it avoids the possible network accesses
     * associated with that implementation, and copes with null values.
     *
     * @param   url1  first URL
     * @param   url2  second URL
     * @return  true iff both are the same, or both are null
     */
    public static boolean urlEquals( URL url1, URL url2 ) {
        if ( url1 == null ) {
            return url2 == null;
        }
        else {
            return url2 != null
                && url1.toString().equals( url2.toString() );
        }
    }

    /**
     * Takes a URLConnection and repeatedly follows 3xx redirects
     * until a non-redirect status is achieved.  Infinite loops are defended
     * against.  The Accept-Encoding header, if present, is propagated
     * to redirect targets.
     *
     * <p>Note that the
     * {@link java.net.HttpURLConnection#setInstanceFollowRedirects}
     * method does something like this, but it refuses to redirect
     * between different URL protocols, for security reasons
     * (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571).
     * Considering similar arguments, this method will direct HTTP-&gt;HTTPS,
     * but not vice versa.
     *
     * @param  conn   initial URL connection
     * @param  redirCodes  list of HTTP codes for which redirects should
     *                     be followed; if null all suitable 3xx redirections
     *                     will be followed (301, 302, 303, 307, 308)
     * @return   target URL connection
     *           (if no redirects, the same as <code>hconn</code>)
     * @see  <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-redirection-3xx"
     *          >RFC 9110 Sec 15.4</a>
     */
    public static URLConnection followRedirects( URLConnection conn,
                                                 int[] redirCodes )
            throws IOException {
        if ( ! ( conn instanceof HttpURLConnection ) ) {
            return conn;
        }
        HttpURLConnection hconn = (HttpURLConnection) conn;
        Set<URL> urlSet = new HashSet<URL>();
        urlSet.add( hconn.getURL() );
        while ( isRedirect( hconn.getResponseCode(), redirCodes ) ) {
            int hcode0 = hconn.getResponseCode();
            URL url0 = hconn.getURL();
            String loc = hconn.getHeaderField( "Location" );
            if ( loc == null || loc.trim().length() == 0 ) {
                throw new IOException( "No Location field for " + hcode0
                                     + " response" );
            }
            URL url1;
            try {
                url1 = new URI( loc ).toURL();
            }
            catch ( MalformedURLException | URISyntaxException e ) {
                throw (IOException)
                      new IOException( "Bad Location field for " + hcode0
                                     + " response from " + url0 )
                     .initCause( e );
            }
            if ( ! urlSet.add( url1 ) ) {
                throw new IOException( "Recursive " + hcode0 + " redirect at "
                                     + url1 );
            }
            String proto0 = url0.getProtocol().toLowerCase();
            String proto1 = url1.getProtocol().toLowerCase();
            if ( "https".equals( proto0 ) && ! "https".equals( proto1 ) ) {
                throw new IOException( "Refuse to redirect " + proto0
                                     + " URL to " + proto1
                                     + " (" + url0 + " -> " + url1 + ")" );
            }
            logger_.config( "HTTP " + hcode0 + " redirect to " + url1 );
            URLConnection conn1 = url1.openConnection();
            if ( ! ( conn1 instanceof HttpURLConnection ) ) {
                return conn1;
            }
            HttpURLConnection hconn1 = (HttpURLConnection) conn1;

            /* Propagate any Accept-Encoding header, which may have been
             * added by hand to the initial connection, to the redirect
             * target, otherwise it will get lost. */
            String acceptEncoding =
                hconn.getRequestProperty( ContentCoding.ACCEPT_ENCODING );
            if ( acceptEncoding != null ) {
                hconn1.setRequestProperty( ContentCoding.ACCEPT_ENCODING,
                                          acceptEncoding );
            }

            /* Codes 307 and 308 do not permit changing the request method
             * from POST to GET.  Since we're not transferring everything
             * else over this might not work properly, so this method might
             * need more work if POST-bearing 307/308 codes are actually
             * encountered, but at least this ensures that we don't change
             * methods in a way that is specifically prohibited.
             * See RFC7538, RFC9110. */
            if ( hcode0 == 307 || hcode0 == 308 ) {
                String method0 = hconn.getRequestMethod();
                if ( "POST".equals( method0 ) ) {
                    hconn1.setRequestMethod( method0 );
                }
            }

            /* Prepare to iterate. */
            hconn = hconn1;
        }
        return hconn;
    }

    /**
     * Indicates whether an HTTP response code should be interpreted
     * as a request to redirect.
     *
     * @param  hcode  code to test
     * @param  redirCodes  list of HTTP codes for which redirects should
     *                     be followed; if null all suitable 3xx redirections
     *                     will be followed (301, 302, 303, 307, 308)
     * @return  true iff hcode represents a redirect
     */
    private static boolean isRedirect( int hcode, int[] redirCodes ) {
        int[] rcodes = redirCodes == null
                     ? new int[] { 301, 302, 303, 307, 308 }
                     : redirCodes;
        for ( int i = 0; i < rcodes.length; i++ ) {
            if ( hcode == rcodes[ i ] ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to install additional URL protocol handlers suitable 
     * for astronomy applications.  Currently installs handlers which 
     * can supply MySpace connections using either "<code>ivo:</code>" or 
     * "<code>myspace:</code>" protocols.
     */
    public static void installCustomHandlers() {

        /* See if the system property which customises URL protocol handling
         * has been set.  If so, don't attempt to mess about further with
         * the configuration. */
        String pkgProp = "java.protocol.handler.pkgs";
        boolean hasPkgProp;
        try {
            hasPkgProp = System.getProperty( pkgProp, "" ).length() > 0;
        }
        catch ( SecurityException e ) {
            hasPkgProp = false;
        }
        if ( hasPkgProp ) {
            logger_.config( pkgProp + " is set - don't further configure " 
                          + "URL protocol handlers" );
            return;
        }

        /* Set up a handler factory which deals with myspace.  This is
         * equivalent to setting the java.protocol.handler.pkgs system
         * property to "uk.ac.starlink.astrogrid.protocols", but the 
         * latter can only be done before starting up the JVM. */
        Map<String,String> handlerMap = new HashMap<String,String>();
        String[] protos = new String[] { "ivo", "myspace", };
        for ( String proto : protos ) {
            handlerMap.put( proto,
                            "uk.ac.starlink.astrogrid.protocols."
                            + proto + ".Handler" );
        }
        URLStreamHandlerFactory fact = 
            new CustomURLStreamHandlerFactory( handlerMap );

        /* Attempt to install the custom handler. */
        try {
            URL.setURLStreamHandlerFactory( fact );
            logger_.config( "Set up URL custom protocol handlers " +
                            Arrays.asList( protos ) );
        }
        catch ( Throwable e ) {
            logger_.warning( "Can't set custom URL protocol handlers: " + e );
        }
    }
}
