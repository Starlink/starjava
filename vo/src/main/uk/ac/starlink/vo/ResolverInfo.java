package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * Information about an astronomical object obrtained from some name
 * resolution service.
 * The current implementation is in terms of CDS's Sesame name resolver
 * web service.
 * 
 * @author  Mark Taylor (Starlink)
 * @since   4 Feb 2005
 * @see  <a href="http://cdsweb.u-strasbg.fr/doc/sesame.htx"
 *          >Sesame Documentation</a>
 */
public class ResolverInfo {

    private double raDegrees_;
    private double decDegrees_;

    /** Base URL for HTTP-GET-based Sesame service. */
    public static final String SESAME_URL =
        "http://cdsweb.u-strasbg.fr/cgi-bin/nph-sesame/-ox2?";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a new resolver from the &lt;Resolver&gt; XML element 
     * containing the result of a successful CDS resolver service query.
     *
     * @param   resolverEl  Resolver element
     */
    private ResolverInfo( Element resolverEl ) throws ResolverException {
        checkTagname( resolverEl, "Resolver" );
        raDegrees_ = Double.NaN;
        decDegrees_ = Double.NaN;
        for ( Node node = resolverEl.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            String tag = getTagname( node );
            if ( "jradeg".equals( tag ) ) {
                raDegrees_ = getDoubleContent( (Element) node );
            }
            else if ( "jdedeg".equals( tag ) ) {
                decDegrees_ = getDoubleContent( (Element) node );
            }
        }
        if ( Double.isNaN( raDegrees_ ) || Double.isNaN( decDegrees_ ) ) {
            throw new ResolverException( "No position information" );
        }
    }

    /**
     * Returns object right ascension in degrees.
     *
     * @return  RA
     */
    public double getRaDegrees() {
        return raDegrees_;
    }

    /**
     * Returns object declination in degrees.
     *
     * @return   declination
     */
    public double getDecDegrees() {
        return decDegrees_;
    }

    /**
     * Attempts to resolve an object by name using a name resover service,
     * and either returns an object containing information about it or
     * throws an exception.
     *
     * @param   name  object name
     * @return  resolver info object
     * @throws  ResolverException  if resolution failed for some reason
     */
    public static ResolverInfo resolve( String name ) throws ResolverException {
        return resolve(name, SESAME_URL);       
    }
    
    public static ResolverInfo resolve( String name, String resolverUrl ) throws ResolverException {
        try {
            URL url = new URL( resolverUrl
                    + URLEncoder.encode( name, "UTF-8" ) );
            logger_.info( url.toString() );
            Element el = new SourceReader()
                        .getElement( new StreamSource( AuthManager.getInstance()
                                                      .openStream( url ) ) );
            String tag = getTagname( el );
            if ( "Sesame".equals( tag ) ) {
                return interpretSesame( el );
            }
            else {
                throw new ResolverException( "Bad element type <" + tag + 
                                             "> from Sesame" );
            }
        }
        catch ( IOException e ) {
            throw new ResolverException( e.getMessage(), e );
        }
        catch ( TransformerException e ) {
            throw new ResolverException( e.getMessage(), e );
        }
    }

    private static ResolverInfo interpretSesame( Element sesameEl )
            throws ResolverException {
        checkTagname( sesameEl, "Sesame" );
        checkForErrors( sesameEl );
        for ( Node node = sesameEl.getFirstChild(); node != null; 
              node = node.getNextSibling() ) {
            if ( "Resolver".equals( getTagname( node ) ) ) {
                Element resolverEl = (Element) node;
                checkForErrors( resolverEl );
                return new ResolverInfo( resolverEl );
            }
        }
        throw new ResolverException( "Name not resolved" );
    }

    private static void checkForErrors( Element el ) throws ResolverException {
        for ( Node node = el.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( "ERROR".equals( getTagname( node ) ) ) {
                throw new ResolverException( DOMUtils
                                            .getTextContent( (Element) node ) );
            }
        }
    }

    private static void checkTagname( Node el, String tagname ) {
        if ( ! tagname.equals( getTagname( el ) ) ) {
            throw new IllegalArgumentException( "Node is not a <" +
                                                tagname + ">" );
        }
    }

    private static String getTagname( Node node ) {
        return node instanceof Element 
             ? ((Element) node).getTagName()
             : null;
    }

    private static double getDoubleContent( Element el ) {
        try {
            return Double.parseDouble( DOMUtils.getTextContent( el ) );
        }
        catch ( NumberFormatException e ) {
            return Double.NaN;
        }
    }

    private static String getStringContent( Element el ) {
        String s = DOMUtils.getTextContent( el );
        if ( s == null || s.trim().length() == 0 ) {
            return null;
        }
        else {
            return s.trim();
        }
    }

    public static void main( String[] args ) throws ResolverException {
        for ( int i = 0; i < args.length; i++ ) {
            String name = args[ i ];
            ResolverInfo info = resolve( name );
            System.out.println( "\t" + name + ":\t" + 
                                (float) info.getRaDegrees() + ",\t" +
                                (float) info.getDecDegrees() );
        }
    }
}
