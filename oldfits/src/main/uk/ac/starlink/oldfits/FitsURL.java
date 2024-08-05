package uk.ac.starlink.oldfits;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a URL as referring to a Fits resource.
 * It splits the URL into the URL of the container file and the 
 * HDU number.  The primary HDU is numbered 0, the first extension is
 * numbered 1.
 */
class FitsURL {

    private final URL container;
    private final int hdu;

    private FitsURL( URL container, int hdu ) {
        this.container = container;
        this.hdu = hdu;
    }

    public URL getContainer() {
        return container;
    }

    public int getHDU() {
        return hdu;
    }

    /**
     * Make a new FitsURL object from a URL.  Return null if the
     * URL doesn't look like a FITS one.
     * A URL is only recognised as a FITS one if its file part ends in 
     * one of the extensions in the supplied list and if it matches
     * one of the ways of specifying an HDU  
     * (ends with "#num", "[num]" or no specifier).
     */
    static FitsURL parseURL( URL url, List<String> extensions ) {
        String urlstr = url.toExternalForm();
        URL basicURL;
        int hduIndex;

        /* Split the URL into a basic part and an explicit or implicit
         * HDU index.  We allow two formats. */
        if ( urlstr.endsWith( "]" ) ) {
            String patstr = "^(.*)\\[([0-9]+)\\]$";
            Pattern pat = Pattern.compile( patstr );
            Matcher mat = pat.matcher( urlstr );
            if ( mat.matches() ) {
                try {
                    basicURL = newURL( mat.group( 1 ) );
                }
                catch ( MalformedURLException e ) {
                    throw new AssertionError( e );
                }
                hduIndex = Integer.parseInt( mat.group( 2 ) );
            }
            else {
                return null;
            }
        }
        else if ( url.getRef() != null ) {
            String patstr = "^(.*)#([0-9]+)$";
            Pattern pat = Pattern.compile( patstr );
            Matcher mat = pat.matcher( urlstr );
            if ( mat.matches() ) {
                try {
                    basicURL = newURL( mat.group( 1 ) );
                }
                catch ( MalformedURLException e ) {
                    throw new AssertionError( e );
                }
                hduIndex = Integer.parseInt( mat.group( 2 ) );
            }
            else {
                return null;
            }
        }
        else {
            basicURL = url;
            hduIndex = 0;
        }

        /* See if the basic URL ends in one of the known FITS extensions,
         * and return null if not. */
        boolean isFits = false;
        for ( Iterator<String> it = extensions.iterator(); it.hasNext(); ) {
            if ( basicURL.getPath().endsWith( it.next() ) ) {
                isFits = true;
                break;
            }
        }
        if ( ! isFits ) {
            return null;
        }

        /* Otherwise, return a new instance. */
        return new FitsURL( basicURL, hduIndex );
    }

    public String toString() {
        return container + "#" + hdu;
    }

    @Override
    public int hashCode() {
        int code = 99801;
        code = 23 * code + hdu;
        code = 23 * code + container.toString().hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof FitsURL ) {
            FitsURL other = (FitsURL) o;
            return this.hdu == other.hdu
                && this.container.toString()
                  .equals( other.container.toString() );
        }
        else {
            return false;
        }
    }

    /**
     * Creates a URL from a string.
     * This is intended as a drop-in replacement for the one-argument
     * URL constructor, which is deprecated in later Java versions.
     *
     * @param  spec  the string to parse as a URL
     * @return URL
     */
    private static URL newURL( String spec ) throws MalformedURLException {

        /* I originally tried replacing the deprecated call with some URI 
         * manipulation.  However, it's tricky, because this package
         * encourages use of trailing square brackets to reference HDUs,
         * in a way that is probably illegal for URIs.  The chances of
         * putting something non-deprecated in here that behave the same
         * as the previous behaviour seem slim.  So just suppress the
         * deprecation warning. */
        @SuppressWarnings("deprecation")
        URL url = new URL( spec );
        return url;
    }

}
