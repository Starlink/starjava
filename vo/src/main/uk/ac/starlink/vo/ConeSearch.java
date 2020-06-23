package uk.ac.starlink.vo;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * Encapsulates the mechanics of a standard cone search web service.
 * The cone search service definition is taken to be supplied by the
 * document at
 * <a href="http://us-vo.org/pubs/files/conesearch.html"
 *         >http://us-vo.org/pubs/files/conesearch.html</a>.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Dec 2004
 */
public class ConeSearch {

    private final String serviceUrl_;
    private final ContentCoding coding_;
    private String label_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a new ConeSearch from its service URL with explicit
     * content-coding.
     *
     * @param  serviceUrl   base URL for cone search
     * @param  coding   controls HTTP-level compression requests
     * @throws  IllegalArgumentException if the service URL is unsuitable
     */
    public ConeSearch( String serviceUrl, ContentCoding coding ) {
        new CgiQuery( serviceUrl );  // may throw
        serviceUrl_ = serviceUrl;
        label_ = serviceUrl_;
        coding_ = coding;
    }

    /**
     * Constructs a new ConeSearch from its service URL with default
     * content-coding.
     *
     * @param  serviceUrl   base URL for cone search
     * @throws  IllegalArgumentException if the service URL is unsuitable
     */
    public ConeSearch( String serviceUrl ) {
        this( serviceUrl, ContentCoding.GZIP );
    }

    /**
     * Constructs a new ConeSearch from a CONE-type resource.
     *
     * @param   resource  resource from registry
     * @param   capability   cone search capability interface
     * @throws  IllegalArgumentException if the service URL is unsuitable
     */
    public ConeSearch( RegResource resource,
                       RegCapabilityInterface capability ) {
        this( capability.getAccessUrl() );
        if ( ! RegistryProtocol.RI1.hasCapability( Capability.CONE,
                                                   capability ) ) {
            logger_.warning( capability.getAccessUrl()
                           + " doesn't look like a cone search" );
        }
        String label = null;
        if ( label == null ) {
            label = resource.getShortName();
        }
        if ( label == null ) {
            label = resource.getTitle();
        }
        if ( label == null ) {
            label = resource.getIdentifier();
        }
        label_ = label;
    }

    /**
     * Asynchronously executes a cone search request, feeding the
     * resulting table to a TableSink.  Note this will not correctly
     * identify error conditions.
     *
     * @param  ra    J2000 right ascension in decimal degrees
     * @param  dec   J2000 declination in decimal degrees
     * @param  sr    search radius in decimal degrees
     * @param  verb  verbosity level - 1, 2 or 3 for increasing verbosity,
     *               other values give default
     * @param  sink  table destination
     */
    public void performSearch( double ra, double dec, double sr, int verb,
                               TableSink sink ) throws IOException {
        URL qurl = getSearchURL( ra, dec, sr, verb );
        logger_.info( "Submitting query: " + qurl );
        InputStream in =
            coding_.openStreamAuth( qurl, AuthManager.getInstance() );
        new VOTableBuilder().streamStarTable( in, sink, null );
    }

    /**
     * Synchronously executes a cone search request.
     *
     * @param  ra    J2000 right ascension in decimal degrees
     * @param  dec   J2000 declination in decimal degrees
     * @param  sr    search radius in decimal degrees
     * @param  verb  verbosity level - 1, 2 or 3 for increasing verbosity,
     *               other values give default
     * @return   table
     */
    public StarTable performSearch( double ra, double dec, double sr,
                                    int verb, StarTableFactory tfact )
            throws IOException {
        URL qurl = getSearchURL( ra, dec, sr, verb );
        logger_.info( "Submitting query: " + qurl );

        /* Submit the CGI query and create a DOM from the resulting stream. */
        StoragePolicy storage = tfact.getStoragePolicy();
        VOElement topEl;
        try {
            topEl = new VOElementFactory( storage )
                   .makeVOElement( coding_
                                  .openStreamAuth( qurl,
                                                   AuthManager.getInstance() ),
                                   qurl.toString() );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* If there is a TABLE in the resulting DOM, return it. */
        NodeList tableEls = topEl.getElementsByTagName( "TABLE" );
        for ( int i = 0; i < tableEls.getLength(); i++ ) {
            Element tableEl = (Element) tableEls.item( i );
            if ( tableEl instanceof TableElement ) {
                StarTable st = new VOStarTable( (TableElement) tableEl );
                st.setURL( qurl );
                return st;
            }
        }

        /* Otherwise, look through PARAM and INFO elements to try to find
         * an explanation of the error. */
        throwErrorElement( topEl );

        /* No nothing.  Throw an exception. */
        throw new IOException( "No TABLE or error param " +
                               "in returned stream" );
    }

    /**
     * Recurses through a DOM looking for elements which appear to represent
     * an error status.  If it finds one, it will throw a suitable IOException.
     *
     * @param  el  DOM root
     */
    private static void throwErrorElement( Element el ) throws IOException {
        String tagName = el.getTagName();
        if ( tagName.indexOf( ':' ) >= 0 ) {
            tagName = tagName.substring( tagName.indexOf( ':' ) + 1 );
        }
        if ( el.getTagName().equalsIgnoreCase( "INFO" ) ||
             el.getTagName().equalsIgnoreCase( "PARAM" ) ) {
            if ( el.hasAttribute( "name" ) &&
                 el.getAttribute( "name" ).equalsIgnoreCase( "ERROR" ) ) {
                throw new IOException( el.getAttribute( "value" ) );
            }
        }
        for ( Node node = el.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( node instanceof Element ) {
                throwErrorElement( (Element) node );
            }
        }
    }

    /**
     * Returns a cone search CGI URL for this service.
     *
     * @param  ra    J2000 right ascension in decimal degrees
     * @param  dec   J2000 declination in decimal degrees
     * @param  sr    search radius in decimal degrees
     * @param  verb  verbosity level - 1, 2 or 3 for increasing verbosity,
     *               other values give default
     * @return   CGI url which will return the VOTable result of this query
     */
    public URL getSearchURL( double ra, double dec, double sr, int verb ) {
        CgiQuery query = new CgiQuery( serviceUrl_ )
                        .addArgument( "RA", ra )
                        .addArgument( "DEC", dec )
                        .addArgument( "SR", sr );
        if ( verb > 0 && verb <= 3 ) {
            query.addArgument( "VERB", verb );
        }
        return query.toURL();
    }

    /**
     * Returns the service URL for this service.
     *
     * @return   base URL for cone search queries
     */
    public URL getServiceURL() {
        try {
            return new URL( serviceUrl_ );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError();
        }
    }

    public String toString() {
        return label_;
    }
}
