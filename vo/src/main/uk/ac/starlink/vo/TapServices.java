package uk.ac.starlink.vo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for working with TapService instances.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2016
 */
public class TapServices {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** GAVO RegTAP service is pretty reliable. */
    private static final TapService REGTAP =
        createTapService( RegTapRegistryQuery.GAVO_REG, TapVersion.V11 );

    /**
     * Sole private constructor prevents instantiation.
     */
    private TapServices() {
    }

    /**
     * Returns a default service  corresponding to a Relational Registry
     * (RegTAP) service.
     *
     * <p>The current implementation returns a hardcoded value,
     * the main GAVO registry service.  Perhaps it should be pluggable,
     * but the GAVO RegTAP service is expected to be pretty reliable.
     *
     * @return  default RegTAP service
     */
    public static TapService getRegTapService() {
        return REGTAP;
    }

    /**
     * Creates a TAP 1.0 service given the base URL,
     * with the endpoints in the default places.
     *
     * @param  baseUrl   base TAP URL
     * @return   service using standard (v1.0) TAP endpoints
     * @throws   IllegalArgumentException  in case of a bad URL
     */
    public static TapService createDefaultTapService( URL baseUrl ) {
        return createTapService( baseUrl, TapVersion.V10 );
    }

    /**
     * Creates a TAP service given the base URL,
     * with the endpoints in the default places and a specified TAP version.
     *
     * <p>This setup is more or less mandatory for
     * TAP 1.0 services, but TAP 1.1 services may have more freedom
     * to provide different sets of endpoints (capability/interface elements)
     * for different purposes, for instance with different securityMethods.
     *
     * @param  baseUrl   base TAP URL
     * @param  tapVersion  TAP protocol version
     * @return   TAP service with standard endpoints
     */
    public static TapService
            createTapService( URL baseUrl, final TapVersion tapVersion ) {
        final String identity = baseUrl.toString();
        final URL sync = appendPath( baseUrl, "/sync" );
        final URL async = appendPath( baseUrl, "/async" );
        final URL tables = appendPath( baseUrl, "/tables" );
        final URL capabilities = appendPath( baseUrl, "/capabilities" );
        final URL availability = appendPath( baseUrl, "/availability" );
        final URL examples = appendPath( baseUrl, "/examples" );
        return new TapService() {
            public String getIdentity() {
                return identity;
            }
            public URL getSyncEndpoint() {
                return sync;
            }
            public URL getAsyncEndpoint() {
                return async;
            }
            public URL getTablesEndpoint() {
                return tables;
            }
            public URL getCapabilitiesEndpoint() {
                return capabilities;
            }
            public URL getAvailabilityEndpoint() {
                return availability;
            }
            public URL getExamplesEndpoint() {
                return examples;
            }
            public TapVersion getTapVersion() {
                return tapVersion;
            }
        };
    }

    /**
     * Creates a TAP 1.0 service given a base URL string,
     * with the endpoints in the default places and a specified TAP version.
     * If the URL is bad, a warning is logged, and null is returned.
     *
     * @param   baseUrl   base TAP URL
     * @param  tapVersion  TAP protocol version
     * @return   service with standard (v1.0) TAP endpoints
     */
    public static TapService createTapService( String baseUrl,
                                               TapVersion tapVersion ) {
        try {
            return createTapService( new URL( baseUrl ), tapVersion );
        }
        catch ( MalformedURLException e ) {
            logger_.warning( "Bad URL for TAP service: " + baseUrl );
            return null;
        }
    }

    /**
     * Returns an array of TapService instances that are described by
     * a supplied TAP capabilities document.
     * The returned list will have at least one entry, but may have
     * more if multiple TAP interfaces or security method variants
     * have been declared.
     *
     * @param   baseUrl    base URL for TAP service (source of capabilities doc)
     * @param   capsDoc    parsed capabilities document
     * @return  array containing one or more TAP services
     */
    public static TapService[] createTapServices( URL baseUrl,
                                                  TapCapabilitiesDoc capsDoc ) {
        List<TapService> taps = new ArrayList<TapService>();
        StdCapabilityInterface[] intfs = capsDoc.getInterfaces();
        for ( StdCapabilityInterface intf : intfs ) {
            URL accessUrl = getBaseUrl( intf.getAccessUrl() );
            if ( Capability.TAP_IVOID
                           .equalsIvoid( new Ivoid( intf.getStandardId() ) ) &&
                 "std".equals( intf.getRole() ) &&
                 accessUrl != null ) {
                TapVersion version = TapVersion.fromString( intf.getVersion() );
                taps.add( createTapService( accessUrl, version ) );
            }
        }
        if ( taps.size() == 0 ) {
            taps.add( createTapService( baseUrl, TapVersion.V10 ) );
        }
        return taps.toArray( new TapService[ 0 ] );
    }

    /**
     * Turns a string giving a service access URL into a URL object.
     * If it can't be done, null is returned and a warning is logged.
     *
     * @param  accessUrl  URL string
     * @return  URL, or null
     */
    private static URL getBaseUrl( String accessUrl ) {
        if ( accessUrl == null ) {
            return null;
        }
        else {
            try {
                return new URL( accessUrl );
            }
            catch ( MalformedURLException e ) {
                logger_.warning( "Ignore badly formed TAP base URL: "
                               + accessUrl );
                return null;
            }
        }
    }

    /**
     * Adds a sub-path to an existing URL.
     *
     * @param  base  base URL
     * @return  subPath   text to append to base
     * @return   concatenation
     */
    private static URL appendPath( URL base, String subPath ) {
        try {
            return new URL( base.toString() + subPath );
        }
        catch ( MalformedURLException e ) {
            assert false;
            throw (RuntimeException)
                  new IllegalArgumentException( "Bad URL???" )
                 .initCause( e );
        }
    }
}
