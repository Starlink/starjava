package uk.ac.starlink.ttools.task;

import java.net.URL;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.vo.EndpointSet;

/**
 * Manages a collection of parameters used to generate a TAP EndpointSet.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2016
 */
public class TapEndpointParams {

    final URLParameter baseParam_;
    final EndpointParameter syncParam_;
    final EndpointParameter asyncParam_;
    final EndpointParameter tablesParam_;
    final EndpointParameter capabilitiesParam_;
    final EndpointParameter availabilityParam_;
    final EndpointParameter examplesParam_;
    final URLParameter[] otherParams_;

    /**
     * Constructor.
     *
     * @param  baseParamName  name of the parameter that specifies the
     *                        base TAP URL
     */
    public TapEndpointParams( String baseParamName ) {
        baseParam_ = new URLParameter( baseParamName );
        baseParam_.setPrompt( "Base URL of TAP service" );
        otherParams_ = new URLParameter[] {
            syncParam_ = new EndpointParameter( "sync" ),
            asyncParam_ = new EndpointParameter( "async" ),
            tablesParam_ = new EndpointParameter( "tables" ),
            capabilitiesParam_ = new EndpointParameter( "capabilities" ),
            availabilityParam_ = new EndpointParameter( "availability" ),
            examplesParam_ = new EndpointParameter( "examples" ),
        };
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<p>The base URL of a Table Access Protocol service.\n" )
            .append( "This is the bare URL without a trailing \"/[a]sync\".\n" )
            .append( "</p>\n" )
            .append( "<p>The default values of the various endpoints\n" )
            .append( "(sync and async query submission, tables metadata,\n" )
            .append( "service-provided examples etc)\n" )
            .append( "use this URL as a parent\n" )
            .append( "and append standard sub-paths.\n" )
            .append( "However, other parameters (" );
        for ( int i = 0; i < 2; i++ ) {
            sbuf.append( "<code>" )
                .append( otherParams_[ i ].getName() )
                .append( "</code>, " );
        }
        sbuf.append( "...)\n" )
            .append( "are provided so that the different endpoints\n" )
            .append( "can be set individually if required.\n" )
            .append( "</p>\n" );
        baseParam_.setDescription( sbuf.toString() );
    }

    /**
     * Returns the parameter that supplies the base TAP service URL.
     *
     * @return   service URL parameter
     */
    public URLParameter getBaseParameter() {
        return baseParam_;
    }

    /**
     * Returns a list of other parameters managed by this object
     * that specify endpoints for TAP-related services.
     *
     * @return   list of parameters excluding the service URL
     */
    public URLParameter[] getOtherParameters() {
        return otherParams_.clone();
    }

    /**
     * Acquires an EndpointSet instance from the environment using the
     * parameters managed by this object.
     *
     * @param   env   execution environment
     * @return   endpoint set
     */
    public EndpointSet getEndpointSet( final Environment env )
            throws TaskException {
        final URL baseUrl = baseParam_.objectValue( env );
        final URL syncUrl = syncParam_.urlValue( env, baseUrl );
        final URL asyncUrl = asyncParam_.urlValue( env, baseUrl );
        final URL tablesUrl = tablesParam_.urlValue( env, baseUrl );
        final URL capabilitiesUrl = capabilitiesParam_.urlValue( env, baseUrl );
        final URL availabilityUrl = availabilityParam_.urlValue( env, baseUrl );
        final URL examplesUrl = examplesParam_.urlValue( env, baseUrl );
        return new EndpointSet() {
            public String getIdentity() {
                return baseUrl.toString();
            }
            public URL getSyncEndpoint() {
                return syncUrl;
            }
            public URL getAsyncEndpoint() {
                return asyncUrl;
            }
            public URL getTablesEndpoint() {
                return tablesUrl;
            }
            public URL getCapabilitiesEndpoint() {
                return capabilitiesUrl;
            }
            public URL getAvailabilityEndpoint() {
                return availabilityUrl;
            }
            public URL getExamplesEndpoint() {
                return examplesUrl;
            }
        };
    }

    /**
     * Parameter for specifying a URL that defaults to one hanging off
     * the base service URL.
     */
    private class EndpointParameter extends URLParameter {

        private final String label_;

        /**
         * Constructor.
         *
         * @param  label   endpoint label; this defines the default sub-path
         *                 for the endpoint, but it is also used to name
         *                 and document the parameter
         */
        EndpointParameter( String label ) {
            super( label + "url" );
            label_ = label;
            setPrompt( "URL for TAP " + label + " endpoint" );
            setDescription( new String[] {
                "<p>Sets the URL to use for the " + label + " endpoint",
                "of the TAP service.",
                "By default, this would be",
                "<code>&lt;" + baseParam_.getName() + "&gt;/"
                             + label + "</code>",
                "but you can set this parameter to some other location",
                "if required.",
                "If left blank, the default value is used.",
                "</p>",
            } );
            setNullPermitted( true );
            setPreferExplicit( false );
        }

        /**
         * Returns the URL specified by this parameter.
         *
         * @param  env  execution environment
         * @param  baseUrl   base service URL that may be used for defaults
         * @return   endpoint specified by this parameter
         */
        URL urlValue( Environment env, URL baseUrl ) throws TaskException {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( baseUrl.toString() );
            if ( sbuf.charAt( sbuf.length() - 1 ) != '/' ) {
                sbuf.append( '/' );
            }
            sbuf.append( label_ );
            setStringDefault( sbuf.toString() );
            return objectValue( env );
        }
    }
}
