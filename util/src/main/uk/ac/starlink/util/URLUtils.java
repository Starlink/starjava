package uk.ac.starlink.util;

import java.net.MalformedURLException;
import java.net.URL;

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
 * <tt>makeURL</tt> is called on the string "gftp://host/file" and
 * no gftp handler is installed, it will be interpreted as a file-protocol
 * URL referring to the (presumably non-existent) file "gftp://host/file".
 * In this case the eventual upshot will presumably be a file-not-found
 * type error rather than a MalformedURLException type error getting
 * presented to the user.  Users of this class should be of the opinion
 * that this is not a particularly bad thing.
 * <p>
 * The <tt>systemId</tt> strings used by {@link javax.xml.transform.Source}s
 * have similar semantics to the strings which this class converts
 * to URLs or contexts.
 * <p>
 * This class assumes that the "file:" protocol is legal for URLs, 
 * and will throw AssertionErrors if this turns out not to be the case.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class URLUtils {

    /* Set up a URL representing the default context (the current directory). */
    private static URL defaultContext;
    static {
        try {
            defaultContext = new URL( "file:." );
        }
        catch ( MalformedURLException e ) {
            throw protestFileProtocolIsLegal( e );
        }
    }

    /**
     * Obtains a URL from a string.  If the String has the form of a URL,
     * it turned directly into a URL.  If it does not, it is treated as
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
            return new URL( location );
        }
        catch ( MalformedURLException e ) {
            try {
                return new URL( "file", null, location );
            }
     
            catch ( MalformedURLException e2 ) {
                throw protestFileProtocolIsLegal( e2 );
            }
        }
    }

    /**
     * Obtains a URL from a string in a given context.
     * The string <tt>context</tt> is turned into a URL as per 
     * the {@link #makeURL(String)} method, unless it is null or
     * the empty string, in which case it is treated as a reference
     * to the current directory.
     * The string <tt>location</tt> is then turned into a URL in
     * the same way as using {@link #makeURL(String)}, except that
     * if it represents a relative path it is resolved in the context
     * of <tt>context</tt>, taking its protocol and/or relative position
     * from it.
     *
     * @param   context   a string representing the context within which
     *                    <tt>location</tt> is to be resolved
     * @param   location   a string representing the location of a resource
     * @return  a URL representing the location of the resource
     */
    public static URL makeURL( String context, String location ) {
        URL contextURL;
        if ( context == null || context.trim().length() == 0 ) {
            contextURL = defaultContext;
        }
        else {
            contextURL = makeURL( context );
        }
        try {
            return new URL( contextURL, location );
        }
        catch ( MalformedURLException e ) {
            try {
                return new URL( contextURL, makeURL( location ).toString() );
            }
            catch ( MalformedURLException e2 ) {
                // can this happen??
                return makeURL( location );
            }
        }
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
}
