package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import uk.ac.starlink.registry.BasicCapability;
import uk.ac.starlink.registry.BasicRegistryClient;
import uk.ac.starlink.registry.BasicResource;
import uk.ac.starlink.registry.RegistryRequestFactory;
import uk.ac.starlink.registry.RegistryQueryException;
import uk.ac.starlink.registry.SoapClient;
import uk.ac.starlink.registry.SoapRequest;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * RegistryQuery implementation using the SOAP Registry Interface 1.0 
 * mechanism.
 *
 * @author   Mark Taylor (Starlink)
 * @since    4 Jan 2005
 * @see  <a href="http://www.ivoa.net/documents/RegistryInterface/20091104/"
 *          >Registry Interface 1.0</a>
 */
public class Ri1RegistryQuery implements RegistryQuery {

    private final BasicRegistryClient regClient_;
    private final String text_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    private static final String SEARCHABLE_REG_QUERY =
        "capability/@standardID = 'ivo://ivoa.net/std/Registry'" +
        " AND " +
        "capability/@xsi:type LIKE '%:Search'" +
        " AND " +
        "full LIKE 'true'";

    /** Default maximum number of registry entries retrieved at once.
     *  Increasing this number may improve performance, but beware:
     *  registry records can be large, and setting it high (even 100) 
     *  can easily exhaust default heap memory with a single buffers-worth.
     *  This may be a consequence of poor memory usage in the registry
     *  classes or SOAP, or it may be fundamental - not sure. */
    public static int RECORD_BUFFER_SIZE = 50;
        
    /** Description of metadata item describing registry location. */
    public final static ValueInfo REGISTRY_INFO = new DefaultValueInfo(
        "Registry Location", URL.class, "URL of registry queried"
    );

    /** Description of metadata item describing query text. */
    public final static ValueInfo TEXT_INFO = new DefaultValueInfo(
        "Registry Query", String.class, "Text of query made to the registry"
    );

    /** Endpoint for primary AstroGrid registry. */
    public static final String AG_REG;

    /** Endpoint for secondary AstroGrid registry. */
    public static final String AG_REG2;

    /** Endpoint for VAO registry. */
    public static final String VAO_REG;

    /** Endpoint for Euro-VO registry. */
    public static final String EUROVO_REG;

    /** List of likely registries. */
    public static final String[] REGISTRIES = new String[] {
        EUROVO_REG = "https://registry.euro-vo.org/services/RegistrySearch",
        VAO_REG = "https://vao.stsci.edu/directory/ristandardservice.asmx",
        AG_REG = "http://registry.astrogrid.org/"
               + "astrogrid-registry/services/RegistryQueryv1_0",
        AG_REG2 = "http://alt.registry.astrogrid.org/"
               + "astrogrid-registry/services/RegistryQueryv1_0",
    };

    /**
     * Constructs a new query object from a SOAP client and a query.
     *
     * @param  soapClient   SOAP client
     * @param  text   ADQL WHERE clause for the registry query
     */
    public Ri1RegistryQuery( SoapClient soapClient, String text ) {
        text_ = text;
        regClient_ = new BasicRegistryClient( soapClient );
    }

    /**
     * Constructs a new query object from a registry URL and a query.
     *
     * @param  endpoint   registry endpoint URL 
     * @param  text   ADQL WHERE clause for the registry query
     */
    public Ri1RegistryQuery( String endpoint, String text ) {
        this( new SoapClient( toUrl( endpoint ) ), text );
    }

    public Iterator<RegResource> getQueryIterator() throws IOException {
        logger_.info( text_ );
        final Iterator<BasicResource> bIt =
            regClient_.getResourceIterator( getSoapRequest() );
        return new Iterator<RegResource>() {
            public boolean hasNext() {
                return bIt.hasNext();
            }
            public RegResource next() {
                return new BasicRegResource( bIt.next() );
            }
            public void remove() {
                bIt.remove();
            }
        };
    }

    public RegResource[] getQueryResources() throws IOException {
        logger_.info( text_ );
        List<BasicResource> bList =
            regClient_.getResourceList( getSoapRequest() );
        RegResource[] resources = new RegResource[ bList.size() ];
        for ( int i = 0; i < bList.size(); i++ ) {
            resources[ i ] = new BasicRegResource( bList.get( i ) );
        }
        return resources;
    }

    public String getText() {
        return text_;
    }

    public URL getRegistry() {
        return regClient_.getEndpoint();
    }

    public DescribedValue[] getMetadata() {
        return new DescribedValue[] {
            new DescribedValue( REGISTRY_INFO, getRegistry() ),
            new DescribedValue( TEXT_INFO, getText() ),
        };
    }

    /**
     * Gets the SoapRequest corresponding to this query.
     *
     * @return  soap request object
     */
    private SoapRequest getSoapRequest() throws IOException {
        return RegistryRequestFactory.adqlsSearch( text_ );
    }

    /**
     * Searches the given registry access URL to find a list of full searchable
     * registry access URLs.
     *
     * @param   regUrl   registry to start with
     * @return   array of registries which can be searched
     */
    public static String[] getSearchableRegistries( String regUrl )
            throws IOException {
        RegistryQuery regQuery = 
            new Ri1RegistryQuery( regUrl, SEARCHABLE_REG_QUERY );
        Set<String> acurlSet = new TreeSet<String>();
        for ( Iterator<RegResource> it = regQuery.getQueryIterator();
              it.hasNext(); ) {
            RegResource res = it.next();
            RegCapabilityInterface[] caps = res.getCapabilities();
            for ( int ic = 0; ic < caps.length; ic++ ) {
                RegCapabilityInterface cap = caps[ ic ];
                String xsiType = cap.getXsiType();
                if ( xsiType != null && xsiType.endsWith( ":Search" ) ) {
                    String acurl = cap.getAccessUrl();
                    if ( acurl != null ) {
                        acurlSet.add( acurl );
                    }
                }
            }
        }
        return acurlSet.toArray( new String[ 0 ] );
    }

    /**
     * Turns a string into a URL without any pesky checked exceptions.
     *
     * @param  url  URL string
     * @return  URL
     */
    static URL toUrl( String url ) {
        try {
            return new URL( url );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Not a URL: " + url )
                 .initCause( e );
        }
    }

    public String toString() {
        return text_;
    }

    /**
     * Returns an ADQL 1.0 WHERE clause which can be used to search
     * for capabilities of the given type in the registry.
     * The WHERE token is not included
     *
     * @param  cap  standard capability
     * @return  ADQL search query
     */
    public static String getAdqlWhere( Capability cap ) {
        StringBuffer abuf = new StringBuffer();
        int nterm = 0;
        if ( abuf.length() > 0 ) {
            abuf.append( " OR " );
        }
        abuf.append( "(" );
        Ivoid[] stdIds = cap.getStandardIds();
        for ( int is = 0; is < stdIds.length; is++ ) {
            if ( is > 0 ) {
                abuf.append( " OR " );
            }
            abuf.append( "capability/@standardID = '" )
                .append( stdIds[ is ] )
                .append( "'" );
        }
        abuf.append( ")" );
        nterm++;

        /* Some say that matching the xsiType is a good way to spot a
         * capability.  Others disagree.  Since there doesn't currently
         * seem to be any registry which works with this strategy but
         * not without it, omit this test for now. */
        //  if ( abuf.length() > 0 ) {
        //      abuf.append( " OR " );
        //  }
        //  abuf.append( "( capability/@xsi:type LIKE '" )
        //      .append( "%" )
        //      .append( xsiTypeTail )
        //      .append( "' )" );
        //  nterm++;

        /* Return the final ADQL search term. */
        return nterm > 1 ? "( " + abuf.toString() + " )"
                         : abuf.toString();
    }

    /**
     * Adapter from BasicResource to RegResource.
     */
    private static class BasicRegResource implements RegResource {
        private final String title_;
        private final String shortName_;
        private final String identifier_;
        private final String publisher_;
        private final String contact_;
        private final String[] subjects_;
        private final String referenceUrl_;
        private final RegCapabilityInterface[] caps_;

        /**
         * Constructor.
         */
        BasicRegResource( BasicResource bres ) {
            title_ = bres.getTitle();
            shortName_ = bres.getShortName();
            identifier_ = bres.getIdentifier();
            publisher_ = bres.getPublisher();
            contact_ = bres.getContact();
            subjects_ = bres.getSubjects();
            referenceUrl_ = bres.getReferenceUrl();
            BasicCapability[] bcaps = bres.getCapabilities();
            caps_ = new BasicRegCapability[ bcaps.length ];
            for ( int ic = 0; ic < bcaps.length; ic++ ) {
                caps_[ ic ] = new BasicRegCapability( bcaps[ ic ] );
            }
        }

        public String getTitle() {
            return title_;
        }
        public String getShortName() {
            return shortName_;
        }
        public String getIdentifier() {
            return identifier_;
        }
        public String getPublisher() {
            return publisher_;
        }
        public String getContact() {
            return contact_;
        }
        public String[] getSubjects() {
            return subjects_;
        }
        public String getReferenceUrl() {
            return referenceUrl_;
        }
        public RegCapabilityInterface[] getCapabilities() {
            return caps_;
        }
    }

    /**
     * Adapter from BasicCapability to RegCapabilityInterface.
     */
    private static class BasicRegCapability implements RegCapabilityInterface {
        private final String accessUrl_;
        private final String standardId_;
        private final String xsiType_;
        private final String description_;
        private final String version_;

        /**
         * Constructor.
         */
        BasicRegCapability( BasicCapability bcap ) {
            accessUrl_ = bcap.getAccessUrl();
            standardId_ = bcap.getStandardId();
            xsiType_ = bcap.getXsiType();
            description_ = bcap.getDescription();
            version_ = bcap.getVersion();
        }

        public String getAccessUrl() {
            return accessUrl_;
        }
        public String getStandardId() {
            return standardId_;
        }
        public String getXsiType() {
            return xsiType_;
        }
        public String getDescription() {
            return description_;
        }
        public String getVersion() {
            return version_;
        }
    }
}
