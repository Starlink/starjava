package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.us_vo.www.SimpleResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * Encapsulates the mechanics of a standard cone search web service.
 * The cone search service definition is taken to be supplied by the
 * document at 
 * <a href="http://us-vo.org/pubs/files/conesearch.html">http://us-vo.org/pubs/files/conesearch.html</a>.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Dec 2004
 */
public class ConeSearch {

    SimpleResource resource_;
    private Logger logger_ = Logger.getLogger( "uk.ac.starlink.vo" );
    
    /**
     * Constructs a new ConeSearch from a CONE-type resource.
     *
     * @param   resource  resource from registry
     * @throws  IllegalArgumentType if <code>resource.getServiceType()</code> 
     *          is not "CONE" or the service URL is unsuitable
     */
    public ConeSearch( SimpleResource resource ) {
        resource_ = resource;
        String stype = resource_.getServiceType();
        if ( ! stype.equalsIgnoreCase( "CONE" ) ) { 
            throw new IllegalArgumentException( "ServiceType \"" + stype + 
                                                "\" should be \"CONE\"" );
        }
        new CgiQuery( resource_.getServiceURL() );  // may throw
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
        new VOTableBuilder().streamStarTable( qurl.openStream(), sink, null );
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
                   .makeVOElement( qurl.openStream(), qurl.toString() );
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
        CgiQuery query = new CgiQuery( resource_.getServiceURL() )
                        .addArgument( "RA", ra )
                        .addArgument( "DEC", dec )
                        .addArgument( "SR", sr );
        if ( verb > 0 && verb < 3 ) {
            query.addArgument( "VERB", sr );
        }
        return query.toURL();
    }

    /**
     * Returns this search's resource.
     *
     * @return  resource
     */
    public SimpleResource getResource() {
        return resource_;
    }

    /**
     * Returns a list of described values which characterise this cone search.
     *
     * @return  cone search service metadata
     */
    public DescribedValue[] getMetadata() {
        List metadata = new ArrayList();
        addMetadatum( metadata, resource_.getShortName(), 
                      "Service short name",
                      "Short name for cone search service" );
        addMetadatum( metadata, resource_.getTitle(), 
                      "Service title", 
                      "Cone search service title" );
        addMetadatum( metadata, resource_.getDescription(), 
                      "Service description", 
                      "Description of cone search service" );
        addMetadatum( metadata, resource_.getReferenceURL(), 
                      "Service reference URL",
                      "Descriptive URL for cone search service" );
        addMetadatum( metadata, resource_.getPublisher(),
                      "Service publisher",
                      "Publisher for cone search service" );
        addMetadatum( metadata, resource_.getServiceURL(),
                      "Service endpoint",
                      "Base URL for cone search service" );
        return (DescribedValue[]) metadata.toArray( new DescribedValue[ 0 ] );
    }

    /**
     * Adds a DescribedValue to a list of them, based on given values and
     * characteristics.  If the given value is blank, it is not added.
     *
     * @param  metadata  list of DescribedValue objects
     * @param  value     the value of the object to add
     * @param  name      the name of the object
     * @param  description  the description of the object
     */
    private static void addMetadatum( List metadata, String value, String name,
                                      String description ) {
        if ( value != null && value.trim().length() > 0 ) {
            ValueInfo info = new DefaultValueInfo( name, String.class,
                                                   description );
            metadata.add( new DescribedValue( info, value ) );
        }
    }

    public String toString() {
        SimpleResource res = getResource();
        String id = null;
        if ( id == null ) {
            id = res.getShortName();
        }
        if ( id == null ) {
            id = res.getTitle();
        }
        if ( id == null ) {
            id = res.getServiceURL().toString();
        }
        return id;
    }
}
