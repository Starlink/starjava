package uk.ac.starlink.splat.vo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import uk.ac.starlink.registry.RegistryRequestFactory;
import uk.ac.starlink.registry.RegistryQueryException;
import uk.ac.starlink.registry.SoapClient;
import uk.ac.starlink.registry.SoapRequest;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Describes a query on a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    4 Jan 2005
 */
public class SSAPRegistryQuery {

    private final SSAPRegistryClient regClient_;
    private final String text_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.splat.vo" );

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

    /** Endpoint for NVO registry. */
    public static final String NVO_REG;

    /** Endpoint for Euro-VO registry. */
    public static final String EUROVO_REG;

    /** List of likely registries. */
    public static final String[] REGISTRIES = new String[] {
        AG_REG = "http://registry.astrogrid.org/"
               + "astrogrid-registry/services/RegistryQueryv1_0",
        AG_REG2 = "http://alt.registry.astrogrid.org/"
               + "astrogrid-registry/services/RegistryQueryv1_0",
        NVO_REG = "http://nvo.stsci.edu/vor10/ristandardservice.asmx",
        EUROVO_REG = "http://registry.euro-vo.org/services/RegistrySearch",
    };

    /**
     * Constructs a new query object from a SOAP client and a query.
     *
     * @param  soapClient   SOAP client
     * @param  text   ADQL WHERE clause for the registry query
     */
    public SSAPRegistryQuery( SoapClient soapClient, String text ) {
        FileOutputStream out=null;
        try {
            out = new FileOutputStream("RegistryOutput.xml");
            soapClient.setEchoStream(out);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     
        text_ = text;
        regClient_ = new SSAPRegistryClient( soapClient );
    }

    /**
     * Constructs a new query object from a registry URL and a query.
     *
     * @param  endpoint   registry endpoint URL 
     * @param  text   ADQL WHERE clause for the registry query
     */
    public SSAPRegistryQuery( String endpoint, String text ) {
        this( new SoapClient( toUrl( endpoint ) ), text );
    }

    /**
     * Executes the query described by this object and returns an 
     * Iterator over {@link RegResource} objects.
     * Note that the iterator's <code>next</code> method may throw the
     * unchecked exception 
     * {@link uk.ac.starlink.registry.RegistryQueryException} with a cause
     * indicating the underlying error in case of a registry access problem.
     *
     * @return  iterator over {@link RegResource}s
     */
    public Iterator getQueryIterator() throws IOException {
        logger_.info( text_ );
        final Iterator<SSAPRegResource> bIt =
            regClient_.getResourceIterator( getSoapRequest() );
        return new Iterator<SSAPRegResource>() {
            public boolean hasNext() {
                return bIt.hasNext();
            }
            public SSAPRegResource next() {
                return new SSAPRegResource( bIt.next() );
            }
            public void remove() {
                bIt.remove();
            }
        };
    }

    /**
     * Executes the query described by this object and returns the result as
     * an array of {@link RegResource}s.
     *
     * @return   resource list
     */
    public SSAPRegResource[] getQueryResources() throws IOException {
        logger_.info( text_ );
        List<SSAPRegResource> bList =
            regClient_.getResourceList( getSoapRequest() );
        SSAPRegResource[] resources = new SSAPRegResource[ bList.size() ];
        for ( int i = 0; i < bList.size(); i++ ) {
            resources[ i ] = new SSAPRegResource( bList.get( i ) );
        }
        return resources;
    }

    /**
     * Returns the query text.
     *
     * @return  query
     */
    public String getText() {
        return text_;
    }

    /**
     * Returns the registry URL.
     *
     * @return url
     */
    public URL getRegistry() {
        return regClient_.getEndpoint();
    }

    /**
     * Returns a set of DescribedValue objects which characterise this query.
     * These would be suitable for use in the parameter list of a 
     * {@link uk.ac.starlink.table.StarTable} resulting from the execution
     * of this query.
     */
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
        SSAPRegistryQuery regQuery = 
            new SSAPRegistryQuery( regUrl, SEARCHABLE_REG_QUERY );
        Set acurlSet = new TreeSet();
        for ( Iterator it = regQuery.getQueryIterator(); it.hasNext(); ) {
            SSAPRegResource res = (SSAPRegResource) it.next();
            SSAPRegCapability[] caps = (SSAPRegCapability[]) res.getCapabilities();
            for ( int ic = 0; ic < caps.length; ic++ ) {
                SSAPRegCapability cap = caps[ ic ];
                String xsiType = cap.getXsiType();
                if ( xsiType != null && xsiType.endsWith( ":Search" ) ) {
                    String acurl = cap.getAccessUrl();
                    if ( acurl != null ) {
                        acurlSet.add( acurl );
                    }
                }
            }
        }
        return (String[]) acurlSet.toArray( new String[ 0 ] );
    }

    /**
     * Turns a string into a URL without any pesky checked exceptions.
     *
     * @param  url  URL string
     * @return  URL
     */
    private static URL toUrl( String url ) {
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

/*******
    /**
     * Adapter from BasicResource to RegResource.
     *
    private static class SSAPRegResource implements RegResource {
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
         *
        SSAPRegRegResource( SSAPRegResource bres ) {
            title_ = bres.getTitle();
            shortName_ = bres.getShortName();
            identifier_ = bres.getIdentifier();
            publisher_ = bres.getPublisher();
            contact_ = bres.getContact();
            subjects_ = bres.getSubjects();
            referenceUrl_ = bres.getReferenceUrl();
            SSAPRegCapability[] bcaps = bres.getCapabilities();
            caps_ = new SSAPRegRegCapability[ bcaps.length ];
            for ( int ic = 0; ic < bcaps.length; ic++ ) {
                caps_[ ic ] = new SSAPRegRegCapability( bcaps[ ic ] );
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
     *
    private static class BasicRegCapability implements RegCapabilityInterface {
        private final String accessUrl_;
        private final String standardId_;
        private final String xsiType_;
        private final String description_;
        private final String version_;

        /**
         * Constructor.
         *
        SSAPRegRegCapability( SSAPRegCapability bcap ) {
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
    }*/
}
