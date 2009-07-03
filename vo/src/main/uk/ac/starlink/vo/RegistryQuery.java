package uk.ac.starlink.vo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import net.ivoa.registry.RegistryAccessException;
import net.ivoa.registry.search.Records;
import net.ivoa.registry.search.RegistrySearchClient;
import net.ivoa.registry.search.VOResource;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Describes a query on a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    4 Jan 2005
 */
public class RegistryQuery {

    private final RegistrySearchClient searchClient_;
    private final String text_;
    private URL endpoint_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    private static final String SEARCHABLE_REG_QUERY =
        "capability/@standardID = '" + RegCapabilityInterface.REG_STDID + "'" +
        " AND " +
        "capability/@xsi:type LIKE '%:Search'" +
        " AND " +
        "full LIKE 'true'";
        
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
     * Constructs a new query object from a search client and a query.
     *
     * @param  searchClient  registry search client
     * @param  text   ADQL WHERE clause for the registry query
     */
    public RegistryQuery( RegistrySearchClient searchClient, String text ) {
        searchClient_ = searchClient;
        text_ = text;
        endpoint_ = null;  // not currently accessible from RegistrySearchClient
    }

    /**
     * Constructs a new query object from a registry URL and a query.
     *
     * @param  endpoint   registry endpoint URL 
     * @param  text   ADQL WHERE clause for the registry query
     */
    public RegistryQuery( String endpoint, String text ) {
        this( new RegistrySearchClient( toUrl( endpoint ) ), text );
        searchClient_.setRecordBufferSize( 100 );
        endpoint_ = toUrl( endpoint );
    }

    /**
     * Executes the query described by this object and returns the
     * result.
     *
     * @return   query result
     */
    public Records performQuery() throws RegistryAccessException {
        logger_.info( "Making query \"" + text_ + "\" to " + getRegistry() );
        return searchClient_.searchByADQL( text_ );
    }

    /**
     * Executes the query described by this object and returns an 
     * Iterator over {@link RegResource} objects.
     * Note that the iterator's <code>next</code> method may throw the
     * unchecked exception {@link RegistryQueryException} with a cause
     * indicating the underlying error in case of a registry access problem.
     *
     * @return  iterator over {@link RegResource}s
     */
    public Iterator getQueryIterator() throws RegistryAccessException {
        final Records records = performQuery();
        return new Iterator() {
            private RegResource next_ = getNext();
            public boolean hasNext() {
                return next_ != null;
            }
            public Object next() {
                RegResource next = next_;
                if ( next != null ) {
                    try {
                        next_ = getNext();
                    }
                    catch ( RegistryAccessException e ) {
                        throw new RegistryQueryException( e );
                    }
                    return next;
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
            private RegResource getNext() throws RegistryAccessException {
                VOResource next = null;
                while ( records.hasNext() && next == null ) {
                    next = records.next();
                }
                return next == null ? null
                                    : new VORegResource( next );
            }
        };
    }

    /**
     * Executes the query described by this object and returns the result as
     * an array of {@link RegResource}s.
     *
     * @return   resource list
     */
    public RegResource[] getQueryResources() throws RegistryAccessException {
        List resList = new ArrayList();
        Records records = performQuery();
        while ( records.hasNext() ) {
            VOResource vores = records.next();
            if ( vores != null ) {
                resList.add( new VORegResource( vores ) );
            }
        }
        return (VORegResource[]) resList.toArray( new VORegResource[ 0 ] );
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
        return endpoint_;
    }

    /**
     * Returns the search client used to make queries.
     *
     * @return  search client
     */
    public RegistrySearchClient getSearchClient() {
        return searchClient_;
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
     * Searches the given registry access URL to find a list of full searchable
     * registry access URLs.
     *
     * @param   regUrl   registry to start with
     * @return   array of registries which can be searched
     */
    public static String[] getSearchableRegistries( String regUrl )
            throws RegistryAccessException {
        RegistryQuery regQuery = 
            new RegistryQuery( regUrl, SEARCHABLE_REG_QUERY );
        Set acurlSet = new TreeSet();
        try {
            for ( Iterator it = regQuery.getQueryIterator(); it.hasNext(); ) {
                RegResource res = (RegResource) it.next();
                RegCapabilityInterface[] caps = res.getCapabilities();
                for ( int ic = 0; ic < caps.length; ic++ ) {
                    RegCapabilityInterface cap = caps[ ic ];
                    if ( RegCapabilityInterface.REG_STDID
                        .equals( cap.getStandardId() ) ) {
                        String acurl = cap.getAccessUrl();
                        if ( acurl != null ) {
                            acurlSet.add( acurl );
                        }
                    }
                }
            }
            return (String[]) acurlSet.toArray( new String[ 0 ] );
        }
        catch ( RegistryQueryException e ) {
            throw (RegistryAccessException) new RegistryAccessException()
                                           .initCause( e );
        }
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
}
