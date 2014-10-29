package uk.ac.starlink.ttools.taplint;

import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    private final Reporter reporter_;
    private volatile int nResolved_;

    /** Namespace URI for VODataService schema. */
    public static final String VODATASERVICE_URI;

    /** Namespace URI for VOSI capabilities schema. */
    public static final String CAPABILITIES_URI;

    /** Namespace URI for VOSI availability schema. */
    public static final String AVAILABILITY_URI;

    private static final String TABLES_URI;
    private static final String VORESOURCE_URI;
    private static final String TAPREGEXT_URI;
    private static final String STC_URI;
    private static final String VODATASERVICE10_URI;
    private static final String XLINK_URI;

    /** Map of namespace URIs to local schema URLs. */
    private static final Map<String,URL> schemaMap_;
    static {
        Class<?> base = IvoaSchemaResolver.class;
        Map<String,URL> map = new HashMap<String,URL>();
        map.put( VODATASERVICE_URI =
                 "http://www.ivoa.net/xml/VODataService/v1.1",
                 base.getResource( "VODataService-v1.1.xsd" ) );
        map.put( CAPABILITIES_URI =
                 "http://www.ivoa.net/xml/VOSICapabilities/v1.0",
                 base.getResource( "VOSICapabilities-v1.0.xsd" ) );
        map.put( AVAILABILITY_URI =
                 "http://www.ivoa.net/xml/VOSIAvailability/v1.0",
                 base.getResource( "VOSIAvailability-v1.0.xsd" ) );
        map.put( TABLES_URI =
                 "http://www.ivoa.net/xml/VOSITables/v1.0",
                 base.getResource( "VOSITables-v1.0.xsd" ) );
        map.put( VORESOURCE_URI =
                 "http://www.ivoa.net/xml/VOResource/v1.0",
                 base.getResource( "VOResource-v1.0.xsd" ) );
        map.put( TAPREGEXT_URI =
                 "http://www.ivoa.net/xml/TAPRegExt/v1.0",
                 base.getResource( "TAPRegExt-v1.0-Erratum1.xsd" ) );
        map.put( STC_URI =
                 "http://www.ivoa.net/xml/STC/stc-v1.30.xsd",
                 base.getResource( "stc-v1.30.xsd" ) );
        map.put( VODATASERVICE10_URI =
                 "http://www.ivoa.net/xml/VODataService/v1.0",
                 base.getResource( "VODataService-v1.0.xsd" ) );
        map.put( XLINK_URI =
                 "http://www.w3.org/1999/xlink",
                 base.getResource( "xlink.xsd" ) );
        schemaMap_ = Collections.unmodifiableMap( map );
    }
    private static final String SCHEMA_TYPE =
        "http://www.w3.org/2001/XMLSchema";

    /**
     * Constructor.
     *
     * @param  reporter  destination for validation messages
     */
    public IvoaSchemaResolver( Reporter reporter ) {
        reporter_ = reporter;
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
                nResolved_++;
                return new UrlInput( location );
            }

            /* No local copy, return null to fall back to default resolution
             * behaviour, and warn that a non-standard schema is in use. */
            else {
                reporter_.report( FixedCode.W_UNSC,
                                  "Schema from unknown namespace "
                                + "during validation: " + namespaceURI );
                return null;
            }
        }

        /* It's not a schema - fall back to default resolution behaviour. */
        else {
            return null;
        }
    }

    /**
     * Returns the number of schemas that this resolver has so far been
     * asked to resolve, and has successfully resolved to known local URLs.
     *
     * @return  number of successful entity resolutions to date
     */
    public int getResolvedCount() {
        return nResolved_;
    }

    /**
     * Returns the namespace->URL map of schemas known by this resolver.
     *
     * @return  namespace->URL map for known schemas
     */
    static Map<String,URL> getSchemaMap() {
        return schemaMap_;
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
