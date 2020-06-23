package uk.ac.starlink.vo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import org.xml.sax.InputSource;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Represents a particular query to a DAL-like service.
 * DAL refers to the the Data Access Layer family of protocols defined
 * by the IVOA.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Feb 2009
 */
public class DalQuery {

    private final CgiQuery cgi_;
    private final String serviceType_;
    private final ContentCoding coding_;
    private String name_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a DAL query based on a resource from a registry.
     *
     * @param  resource  resource describing the DAL service
     * @param  capability  DAL capability from resource
     * @param  serviceType  short name for service type; informative,
     *                      used for error messages etc
     * @param  raPos     right ascension of ROI center in degrees
     * @param  decPos    declination of ROI center in degrees
     * @param  size      ROI size in degrees
     */
    public DalQuery( RegResource resource, RegCapabilityInterface capability,
                     String serviceType,
                     double raPos, double decPos, double size ) {
        this( capability.getAccessUrl(), serviceType, raPos, decPos, size,
              ContentCoding.GZIP );
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
     * Constructs a DAL query based on a service URL.
     *
     * @param  baseURL   URL forming basis of CGI query for the DAL service
     * @param  serviceType  short name for service type; informative,
     *                      used for error messages etc
     * @param  raPos     right ascension of ROI center in degrees
     * @param  decPos    declination of ROI center in degrees
     * @param  size      size in degrees
     * @param  coding    controls HTTP-level byte-stream compression
     */
    public DalQuery( String baseURL, String serviceType,
                     double raPos, double decPos, double size,
                     ContentCoding coding ) {
        cgi_ = new CgiQuery( baseURL );
        name_ = baseURL;
        serviceType_ = serviceType;
        coding_ = coding;
        if ( !Double.isNaN( raPos ) || !Double.isNaN( decPos ) ) {
            addArgument( "POS", doubleToString( raPos ) + "," 
                              + doubleToString( decPos ) );
        }
        if ( ! Double.isNaN( size ) ) {
            addArgument( "SIZE", doubleToString( size ) );
        }
    }

    /**
     * Adds an argument to the query.  No validation is performed to check
     * it is one of the ones that the DAL service knows about.
     *
     * @param   name  service argument name
     * @param   value  argument value
     */
    public void addArgument( String name, String value ) {
        cgi_.addArgument( name, value );
    }

    /**
     * Executes this query synchronously, returning a StarTable which
     * represents the results.  If the query resulted in a QUERY_STATUS
     * of ERROR, or if the returned VOTable document is not comprehensible
     * according to the DAL rules, an IOException will be thrown.
     *
     * @param    tfact   factory which may be used to influence how the
     *           table is built
     * @throws   IOException  in absence of good data
     */
    public StarTable execute( StarTableFactory tfact ) throws IOException {
        return executeQuery( cgi_.toURL(), tfact, coding_ );
    }

    public String toString() {
        return name_;
    }

    /**
     * Encodes a floating point value as a string for use in a DAL query.
     * There ought to be a definition within the DAL protocols of how to
     * do this.  At time of writing there is not, so the current implementation
     * defers to the ad-hoc implementation in 
     * {@link uk.ac.starlink.util.CgiQuery#formatDouble},
     * which avoids exponential notation except for very large/small values.
     *
     * @param  value  numeric value
     * @return   string equivalent
     */
    public String doubleToString( double value ) {
        return CgiQuery.formatDouble( value );
    }

    /**
     * Submits a synchronous query to a URL and retrieves the result
     * as a StarTable following standard DAL conventions.
     *
     * @param  qurl  query URL
     * @param  tfact   table factory
     * @param  coding   encoding to use for communications
     * @return   table included in DAL result resource
     */
    public static StarTable executeQuery( URL qurl, StarTableFactory tfact,
                                          ContentCoding coding )
            throws IOException {
        logger_.info( "Submitting query: " + qurl );
        VOElementFactory vofact =
            new VOElementFactory( tfact.getStoragePolicy() );
        InputStream in =
            coding.openStreamAuth( qurl, AuthManager.getInstance() );
        InputSource inSrc = new InputSource( in );
        inSrc.setSystemId( qurl.toString() );
        return DalResultXMLFilter.getDalResultTable( vofact, inSrc );
    }
}
