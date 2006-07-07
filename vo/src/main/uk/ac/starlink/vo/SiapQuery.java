package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import org.us_vo.www.SimpleResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;

/**
 * A particular query to a Simple Image Access Protocol service.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Jan 2005
 * @see      <http://www.ivoa.net/Documents/latest/SIA.html>
 */
public class SiapQuery {

    private final CgiQuery cgi_;
    private String name_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a SIAP query based on a resource from a registry.
     *
     * @param  resource  resource describing the SIAP service
     * @param  raPos     right ascension of ROI center in degrees
     * @param  decPos    declination of ROI center in degrees
     * @param  raSize    ROI extent in right ascension in degrees
     * @param  decSize   ROI extent in declination in degrees
     */
    public SiapQuery( SimpleResource resource, double raPos, double decPos, 
                      double raSize, double decSize ) {
        this( resource.getServiceURL(), raPos, decPos, raSize, decSize );
        if ( ! resource.getServiceType().toUpperCase().startsWith( "SIAP" ) ) {
            throw new IllegalArgumentException( 
                "Resource " + resource.getShortName() + 
                " is not labelled as a SIAP service" );
        }
        String id = null;
        if ( id == null ) {
            id = resource.getShortName();
        }
        if ( id == null ) {
            id = resource.getTitle();
        }
        if ( id != null ) {
            name_ = id;
        }
    }

    /**
     * Constructs a SIAP query based on a service URL.
     *
     * @param  baseURL   URL forming basis of CGI query for the SIAP service
     * @param  raPos     right ascension of ROI center in degrees
     * @param  decPos    declination of ROI center in degrees
     * @param  raSize    ROI extent in right ascension in degrees
     * @param  decSize   ROI extent in declination in degrees
     */
    public SiapQuery( String baseURL, double raPos, double decPos, 
                      double raSize, double decSize ) {
        cgi_ = new CgiQuery( baseURL );
        name_ = baseURL;
        addArgument( "POS", raPos + "," + decPos );
        addArgument( "SIZE", ( raSize == decSize )
                             ? Double.toString( raSize )
                             : ( raSize + "," + decSize ) );
    }

    /**
     * Adds an argument to the query.  No validation is performed to check
     * it is one of the ones that SIAP knows about.
     *
     * @param   name  SIAP argument name
     * @param   value  argument value
     */
    public void addArgument( String name, String value ) {
        cgi_.addArgument( name, value );
    }

    /**
     * Executes this query synchronously, returning a StarTable which 
     * represents the results.  If the query resulted in a QUERY_STATUS 
     * of ERROR, or if the returned VOTable document is not comprehensible
     * according to the SIAP specification, an IOException will be thrown.
     * 
     * @param    tfact   factory which may be used to influence how the 
     *           table is built
     * @throws   IOException  in absence of good data
     */
    public StarTable execute( StarTableFactory tfact ) throws IOException {

        /* Submit the CGI query and create a DOM from the resulting stream. */
        URL qurl = cgi_.toURL();
        logger_.info( "Submitting query: " + qurl );
        StoragePolicy storage = tfact.getStoragePolicy();
        VOElement topEl;
        try {
            topEl = new VOElementFactory( storage )
                   .makeVOElement( qurl.openStream(), qurl.toString() );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Locate the first RESOURCE element with type="results", which
         * is defined to contain the result. */
        NodeList resources = topEl.getElementsByTagName( "RESOURCE" );
        Element results = null;
        for ( int i = 0; i < resources.getLength(); i++ ) {
            Element resource = (Element) resources.item( i );

            /* Since the VOTable 1.1 schema defines "results" as the default
             * value of the type attribute, an absent type should probably
             * count.  Use this if we don't find an explicit results value. */
            if ( ! resource.hasAttribute( "type" ) ) {
                results = resource;
            }

            /* If we have one explicitly marked results though, use that. */
            else if ( "results".equals( resource.getAttribute( "type" ) ) ) {
                results = resource;
                break;
            }
        }

        /* If there is no results element, throw an exception. */
        if ( results == null ) {
            throw new IOException( "No suitable RESOURCE found " +
                                   "in returned VOTable" );
        }

        /* Try to find the status info. */
        String status = null;
        String message = null;
        for ( Node node = results.getFirstChild(); node != null; 
              node = node.getNextSibling() ) {
            if ( node instanceof Element ) {
                Element el = (Element) node;
                if ( "INFO".equals( el.getTagName() ) &&
                     "QUERY_STATUS".equals( el.getAttribute( "name" ) ) ) {
                    status = el.getAttribute( "value" );
                    message = DOMUtils.getTextContent( el );
                    break;
                }
            }
        }

        /* If the query status is error, throw an exception. */
        if ( "ERROR".equals( status ) ) {
            throw new IOException( "SIAP query error: " + message );
        }

        /* Locate the table within the results resource. */
        StarTable st = null;
        for ( Node node = results.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( node instanceof TableElement ) {
                st = new VOStarTable( (TableElement) node );
                break;
            }
        }

        /* If there was no table, throw an exception. */
        if ( st == null ) {
            throw new IOException( "No TABLE element found in SIAP " +
                                   "returned VOTable" );
        }

        /* Return the StarTable. */
        return st;
    }

    public String toString() {
        return name_;
    }

}
