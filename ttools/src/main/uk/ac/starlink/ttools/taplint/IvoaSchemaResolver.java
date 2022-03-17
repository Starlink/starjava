package uk.ac.starlink.ttools.taplint;

import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * ResourceResolver implementation used for validating documents against
 * XSD schemas relating to known IVOA standards.
 * Schemas for a number of IVOA-related namespaces are kept locally.
 * This means both that validation can be performed without having to
 * retrieve documents from the remote IVOA web site, and also that
 * documents are not able to subsitute their own hacked versions of
 * the schema for a given namespace, they have to use the official one.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2014
 */
public class IvoaSchemaResolver implements LSResourceResolver {

    private final Map<String,URL> schemaMap_;
    private final Set<String> resolvedNamespaces_;
    private final Set<String> unresolvedNamespaces_;

    /** Namespace URI for VODataService schema. */
    public static final String VODATASERVICE_URI =
        "http://www.ivoa.net/xml/VODataService/v1.1";

    /** Namespace URI for VOSI capabilities schema. */
    public static final String CAPABILITIES_URI =
        "http://www.ivoa.net/xml/VOSICapabilities/v1.0";

    /** Namespace URI for VOSI availability schema. */
    public static final String AVAILABILITY_URI =
        "http://www.ivoa.net/xml/VOSIAvailability/v1.0";

    /** Namespace URI for UWS schema. */
    public static final String UWS_URI =
        "http://www.ivoa.net/xml/UWS/v1.0";

    /**
     * Unmodifiable map of namespace URIs to local schema URLs for
     * a selection of schemas from http://www.ivoa.net.
     * Note that the namespace (map key) does not necessarily match the URL
     * (map value) in point of minor version number; the minor version number
     * in the URL may be later than the one in the namespace.
     * This is deliberate, and is codified and explained in the
     * XML Versioning document (at time of writing,
     * <a href="http://ivoa.net/documents/Notes/XMLVers/"
     *         >PEN-XMLVers-1.0-20160906</a>, sec 2.2.3).
     */
    public static final Map<String,URL> IVOA_SCHEMA_MAP =
        Collections.unmodifiableMap( createIvoaSchemaMap() );

    /**
     * Unmodifiable map of namespace URIs to local schema URLs for
     * a selection of schemas from http://www.w3.org.
     */
    public static final Map<String,URL> W3C_SCHEMA_MAP =
        Collections.unmodifiableMap( createW3cSchemaMap() );

    private static final String SCHEMA_TYPE =
        "http://www.w3.org/2001/XMLSchema";

    /**
     * Constructs a resolver with a supplied schema map.
     *
     * @param  schemaMap  of known namespace to schema URLs
     */
    public IvoaSchemaResolver( Map<String,URL> schemaMap ) {
        if ( schemaMap == null ) {
            schemaMap = new HashMap<String,URL>();
            schemaMap.putAll( W3C_SCHEMA_MAP );
            schemaMap.putAll( IVOA_SCHEMA_MAP );
        }
        schemaMap_ = schemaMap;
        resolvedNamespaces_ = new LinkedHashSet<String>();
        unresolvedNamespaces_ = new LinkedHashSet<String>();
    }

    /**
     * Constructs a resolver with a default schema map.
     */
    public IvoaSchemaResolver() {
        this( null );
    }

    /**
     * Returns the namespace-&gt;URL map of schemas known by this resolver.
     * This may be modified to change the resolution behaviour.
     *
     * @return  modifiable namespace-&gt;URL map for known schemas
     */
    public Map<String,URL> getSchemaMap() {
        return schemaMap_;
    }

    public LSInput resolveResource( String type, String namespaceURI,
                                    String publicId, String systemId,
                                    String baseURI ) {

        /* See if we are being asked for a schema relating to a given
         * namespace URI. */
        if ( SCHEMA_TYPE.equals( type ) && namespaceURI != null ) {

            /* See if we have a local copy of the relevant schema. */
            URL location = schemaMap_.get( namespaceURI );
            if ( location != null ) {
                resolvedNamespaces_.add( namespaceURI );
                return new UrlInput( location );
            }

            /* No local copy, return null to fall back to default resolution
             * behaviour, and warn that a non-standard schema is in use. */
            else {
                unresolvedNamespaces_.add( namespaceURI );
                return null;
            }
        }

        /* It's not a schema - fall back to default resolution behaviour. */
        else {
            return null;
        }
    }

    /**
     * Returns the the schema namespaces which this resolver has so far
     * been asked to resolve, and has successfully resolved to known URLs.
     *
     * @return  successful schema namespace resolutions to date
     */
    public Set<String> getResolvedNamespaces() {
        return resolvedNamespaces_;
    }

    /**
     * Returns the schema namespaces which this resolver has so far
     * been asked to resolve, and has failed because they are unknown.
     *
     * @return  unsuccessful schema namespace resolutions to date
     */
    public Set<String> getUnresolvedNamespaces() {
        return unresolvedNamespaces_;
    }

    /**
     * Returns a resource URL for an unqualified resource name
     * in the same namespace as this class.
     *
     * @param  name  unqualified resource name
     * @return  resource URL
     */
    private static URL getResource( String name ) {
        return IvoaSchemaResolver.class.getResource( name );
    }

    /**
     * Returns a namespace-&gt;URL map for a selection of IVOA schemas.
     *
     * @return  map
     */
    private static Map<String,URL> createIvoaSchemaMap() {
        Map<String,URL> map = new HashMap<>();
        map.put( VODATASERVICE_URI, getResource( "VODataService-v1.2.xsd" ) );
        map.put( CAPABILITIES_URI, getResource( "VOSICapabilities-v1.0.xsd" ) );
        map.put( AVAILABILITY_URI, getResource( "VOSIAvailability-v1.0.xsd" ) );
        map.put( UWS_URI, getResource( "UWS-v1.1.xsd" ) );
        map.put( "http://www.ivoa.net/xml/VOSITables/v1.0",
                 getResource( "VOSITables-v1.1.xsd" ) );
        map.put( "http://www.ivoa.net/xml/VOResource/v1.0",
                 getResource( "VOResource-v1.1.xsd" ) );
        map.put( "http://www.ivoa.net/xml/TAPRegExt/v1.0",
                 getResource( "TAPRegExt-v1.0-Erratum1.xsd" ) );
        map.put( "http://www.ivoa.net/xml/STC/stc-v1.30.xsd",
                 getResource( "stc-v1.30.xsd" ) );
        map.put( "http://www.ivoa.net/xml/VODataService/v1.0",
                 getResource( "VODataService-v1.0.xsd" ) );
        return map;
    }

    /**
     * Returns a namespace-&gt;URL map for a selection of W3C schemas.
     *
     * @return  map
     */
    private static Map<String,URL> createW3cSchemaMap() {
        Map<String,URL> map = new HashMap<>();
        map.put( "http://www.w3.org/1999/xlink",
                 getResource( "xlink.xsd" ) );
        map.put( "http://www.w3.org/2001/XMLSchema",
                 getResource( "XMLSchema.xsd" ) );
        map.put( "http://www.w3.org/XML/1998/namespace",
                 getResource( "xmlnamespace.xsd" ) );
        return map;
    }

    /**
     * Simple LSInput implementation that just supplies a URL.
     * Note this is not intended for general purpose use.
     */
    private static class UrlInput implements LSInput {

        private final URL url_;

        /**
         * Constructor.
         *
         * @return  url  URL containing resource
         */
        public UrlInput( URL url ) {
            url_ = url;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public void setCharacterStream( Reader characterStream ) {
            throw new UnsupportedOperationException();
        }

        public InputStream getByteStream() {
            return null;
        }

        public void setByteStream( InputStream byteStream ) {
            throw new UnsupportedOperationException();
        }

        public String getStringData() {
            return null;
        }

        public void setStringData( String stringData ) {
            throw new UnsupportedOperationException();
        }

        public String getSystemId() {
            return url_.toString();
        }

        public void setSystemId( String systemId ) {
            throw new UnsupportedOperationException();
        }

        public String getPublicId() {
            return null;
        }

        public void setPublicId( String publicId ) {
            throw new UnsupportedOperationException();
        }

        public String getBaseURI() {
            return null;
        }

        public void setBaseURI( String baseURI ) {
            throw new UnsupportedOperationException();
        }

        public String getEncoding() {
            return null;
        }

        public void setEncoding( String encoding ) {
            throw new UnsupportedOperationException();
        }

        public boolean getCertifiedText() {
            return false;
        }

        public void setCertifiedText( boolean certifiedText ) {
            throw new UnsupportedOperationException();
        }
    }
}
