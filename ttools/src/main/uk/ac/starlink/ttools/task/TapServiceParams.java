package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.AuthStatus;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.util.IOSupplier;
import uk.ac.starlink.vo.TapCapabilitiesDoc;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.vo.TapServices;
import uk.ac.starlink.vo.TapVersion;

/**
 * Manages a collection of parameters used to generate a TapService.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2016
 */
public class TapServiceParams {

    private final URLParameter baseParam_;
    private final Parameter<String> intfParam_;
    private final BooleanParameter authParam_;
    private final EndpointParameter syncParam_;
    private final EndpointParameter asyncParam_;
    private final EndpointParameter tablesParam_;
    private final EndpointParameter capabilitiesParam_;
    private final EndpointParameter availabilityParam_;
    private final EndpointParameter examplesParam_;
    private final URLParameter[] otherParams_;

    private final String INTFPARAM_NAME = "interface";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param  baseParamName  name of the parameter that specifies the
     *                        base TAP URL
     * @param  readCapabilitiesDflt if true, the default behaviour involves
     *                              reading the service's capabilities document;
     *                              if false, the default behaviour assumes
     *                              standard endpoints
     */
    public TapServiceParams( String baseParamName,
                             boolean readCapabilitiesDflt ) {
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

        StringBuffer obuf = new StringBuffer();
        for ( int i = 0; i < otherParams_.length; i++ ) {
            obuf.append( i == 0 ? "(" : ", " )
                .append( "<code>" )
                .append( otherParams_[ i ].getName() )
                .append( "</code>" );
        }
        obuf.append( ")" );
        String otherTxt = obuf.toString();

        baseParam_.setDescription( new String[] {
            "<p>The base URL of a Table Access Protocol service.",
            "This is the bare URL without a trailing \"/[a]sync\".",
            "</p>",
            "<p>In the usual case, the default values of the various endpoints",
            "(sync and async query submission, tables metadata,",
            "service-provided examples etc)",
            "use this URL as a parent",
            "and append standard sub-paths.",
            "</p>",
            "<p>In some cases however, determination of the endpoints is",
            "more complicated, as determined by",
            "the <code>" + INTFPARAM_NAME + "</code> parameter",
            "which may cause endpoints to be read from the capabilities",
            "document at",
            "<code>" + baseParam_.getName() + "/capabilities</code>,",
            "and by other endpoint-specific parameters",
            otherTxt,
            "for fine tuning.",
            "</p>"
        } );

        intfParam_ = new StringParameter( INTFPARAM_NAME );
        intfParam_.setPrompt( "TAP interface label" );
        intfParam_.setUsage( "tap1.0|tap1.1|cap" );
        intfParam_.setDescription( new String[] {
            "<p>Defines how the service endpoints and",
            "the version of the TAP protocol to use for queries is determined.",
            "This may take one of the following (case-insensitive) values:",
            "<ul>",
            "<li><code>TAP1.0</code>:",
            "The standard TAP endpoints are used,",
            "without examining the service's capabilities document.",
            "The service is queried using version 1.0 of the TAP protocol.",
            "</li>",
            "<li><code>TAP1.1</code>:",
            "The standard TAP endpoints are used,",
            "without examining the service's capabilities document.",
            "The service is queried using version 1.1 of the TAP protocol.",
            "</li>",
            "<li><code>cap</code>:",
            "The service's capabilities document is examined,",
            "and the endpoints listed there are used.",
            "</li>",
            "</ul>",
            "</p>",
            "<p>The capabilities document, if used, is read from",
            "<code>" + baseParam_.getName() + "</code>/capabilities",
            "unless the <code>" + capabilitiesParam_.getName() + "</code>",
            "parameter is defined, in which case that is used.",
            "</p>",
            "<p>The baseline value of all the TAP service endpoints",
            "(<code>sync</code>, <code>async</code>, <code>tables</code>,",
            "<code>capabilities</code>, <code>examples</code>)",
            "are determined by this parameter,",
            "but each of those endpoint values may be overridden",
            "individually by other endpoint-specific parameters",
            otherTxt,
            "</p>",
            "<p>For default (unauthenticated) access,",
            "the default value is usually suitable.",
            "</p>",
        } );
        intfParam_.setStringDefault( readCapabilitiesDflt ? "cap" : "tap1.0" );
        intfParam_.setNullPermitted( true );

        authParam_ = new BooleanParameter( "auth" );
        authParam_.setPrompt( "Attempt optional authentication?" );
        authParam_.setDescription( new String[] {
            "<p>If true, then an attempt will be made to",
            "<ref id='AuthManager'>authenticate</ref>",
            "with the TAP service even if anonymous operation is permitted.",
            "If the service offers authentication,",
            "you will be asked for credentials.",
            "</p>",
            "<p>To use this option in non-interactive contexts,",
            "you may want to use the",
            "<code>auth.username</code> and <code>auth.password</code>",
            "<ref id='sysProperties'>system properties</ref>.",
            "</p>",
        } );
        authParam_.setBooleanDefault( false );
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
     * Returns the parameters used to select the TAP interface,
     * including authentication options.
     *
     * @return  TAP interface parameters
     */
    public List<Parameter<?>> getInterfaceParameters() {
        List<Parameter<?>> intfParams = new ArrayList<Parameter<?>>();
        intfParams.add( intfParam_ );
        intfParams.add( authParam_ );
        return intfParams;
    }

    /**
     * Returns a list of other parameters managed by this object
     * that specify endpoints for TAP-related services.
     *
     * @return   list of parameters excluding the service URL
     */
    public List<URLParameter> getOtherParameters() {
        return Arrays.asList( otherParams_ );
    }

    /**
     * Manages acquisition of a TapService instance from the environment
     * using the parameters managed by this object.
     *
     * <p>The idea is that all the environment interaction is done by
     * this method, and all the service interaction is done when using
     * the supplier's {@link uk.ac.starlink.util.IOSupplier#get get} method.
     * This may not be perfectly true because of difficulties in
     * arranging dynamic parameter defaulting based on contacting the service.
     *
     * @param   env   execution environment
     * @return   supplier for a TAP service description
     */
    public IOSupplier<TapService> getServiceSupplier( final Environment env )
            throws TaskException {
        String intfkey = intfParam_.objectValue( env );
        final TapService baseService;

        /* Null not permitted. */
        if ( intfkey == null || intfkey.trim().length() == 0 ) {
            throw new ParameterValueException( intfParam_,
                                               "Null not permitted" );
        }

        /* Use TAP 1.0 or TAP 1.1 standard endpoints. */
        else if ( "TAP1.0".equalsIgnoreCase( intfkey ) ) {
            baseService = createStandardTapService( env, TapVersion.V10 );
        }
        else if ( "TAP1.1".equalsIgnoreCase( intfkey ) ) {
            baseService = createStandardTapService( env, TapVersion.V11 );
        }

        /* Standard unauthenticated access. */
        else if ( "cap".equalsIgnoreCase( intfkey ) ) {
            baseService = createCapabilitiesTapService( env );
        }

        /* Unknown. */
        else {
            throw new ParameterValueException( intfParam_,
                                               "Unknown interface format" );
        }

        /* Having got the interfaces we asked for, adjust them according
         * to the overridel parameters, if supplied. */
        final URL syncUrl =
            getEndpoint( env, syncParam_, baseService.getSyncEndpoint() );
        final URL asyncUrl =
            getEndpoint( env, asyncParam_, baseService.getAsyncEndpoint() );
        final URL tablesUrl =
            getEndpoint( env, tablesParam_, baseService.getTablesEndpoint() );
        final URL capabilitiesUrl =
            getEndpoint( env, capabilitiesParam_,
                         baseService.getCapabilitiesEndpoint() );
        final URL availabilityUrl =
            getEndpoint( env, availabilityParam_,
                         baseService.getAvailabilityEndpoint() );
        final URL examplesUrl =
            getEndpoint( env, examplesParam_,
                         baseService.getExamplesEndpoint() );
        final String baseUrl = baseService.getIdentity();
        final TapVersion tapVersion = baseService.getTapVersion();
        logger_.config( "TAP version:        " + tapVersion );
        logger_.config( "TAP base URL:       " + baseUrl );
        if ( ! syncUrl.equals( baseService.getSyncEndpoint() ) ) {
            logger_.config( "TAP sync:           " + syncUrl );
        }
        if ( ! asyncUrl.equals( baseService.getAsyncEndpoint() ) ) {
            logger_.config( "TAP async:          " + asyncUrl );
        }
        if ( ! capabilitiesUrl.equals( baseService.getCapabilitiesEndpoint())) {
            logger_.config( "TAP capabilities:   " + capabilitiesUrl );
        }
        if ( ! tablesUrl.equals( baseService.getTablesEndpoint() ) ) {
            logger_.config( "TAP tables:         " + tablesUrl );
        }
        if ( ! examplesUrl.equals( baseService.getExamplesEndpoint() ) ) {
            logger_.config( "TAP examples:       " + examplesUrl );
        }
        if ( ! availabilityUrl.equals( baseService.getAvailabilityEndpoint())) {
            logger_.config( "TAP availability:   " + availabilityUrl );
        }

        /* Create service. */
        TapService service = new TapService() {
            public String getIdentity() {
                return baseUrl;
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
            public TapVersion getTapVersion() {
                return tapVersion;
            }
        };

        boolean isAuth = authParam_.booleanValue( env );

        /* Return the supplier.  This should not have reference to the
         * environment, and should do any time-consuming activities
         * that happen at execution time rather than task preparation time. */
        return () -> {

            /* Perform pre-emptive authentication if so requested. */
            if ( isAuth ) {
                boolean isHead = true;
                boolean forceLogin = true;
                AuthManager.getInstance()
                           .authcheck( capabilitiesUrl, isHead, forceLogin );
            }

            /* Return service object. */
            return service;
        };
    }

    /**
     * Returns a defaulted value for a URL parameter.
     *
     * @param  env  execution environment
     * @param  urlParam  parameter
     * @param  dfltUrl   default value for parameter
     * @return  value from environment
     */
    private static URL getEndpoint( Environment env, Parameter<URL> urlParam,
                                    URL dfltUrl )
            throws TaskException {
        urlParam.setStringDefault( dfltUrl.toString() );
        return urlParam.objectValue( env );
    }

    /**
     * Returns a TAP service object corresponding to the standard endpoints
     * for a given TAP version.  The capabilities document is not read.
     *
     * @param  env  execution environment
     * @param  tapVersion  TAP protocol version
     * @return   TAP service object
     */
    private TapService createStandardTapService( Environment env,
                                                 TapVersion tapVersion )
            throws TaskException {
        final URL baseUrl = baseParam_.objectValue( env );
        return TapServices.createTapService( baseUrl, tapVersion );
    }

    /**
     * Returns a TAP service object obtained by examining the service's
     * capabilities document.
     *
     * @param  env  execution environment
     * @return   TAP service object
     */
    private TapService createCapabilitiesTapService( Environment env )
            throws TaskException {
        final URL baseUrl = baseParam_.objectValue( env );
        final URL capabilitiesUrl = capabilitiesParam_.urlValue( env, baseUrl );
        logger_.info( "Reading TAP capabilities from " + capabilitiesUrl );
        TapCapabilitiesDoc capsDoc;
        try {
            capsDoc = TapCapabilitiesDoc.readCapabilities( capabilitiesUrl );
        }
        catch ( IOException e ) {
            throw new ExecutionException( "Error reading capabilities from "
                                        + capabilitiesUrl + ": " + e, e );
        }
        catch ( SAXException e ) {
            throw new ExecutionException( "Error parsing capabilities from "
                                        + capabilitiesUrl + ": " + e, e );
        }
        TapService[] services =
            TapServices.createTapServices( baseUrl, capsDoc );
        if ( services.length > 0 ) {
            return services[ 0 ];
        }
        else {
            throw new TaskException( "No TAP services declared in "
                                   + "capabilities doc " + capabilitiesUrl );
        }
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
            setPrompt( "Override URL for TAP " + label + " endpoint" );
            setDescription( new String[] {
                "<p>Sets the URL to use for the " + label + " endpoint",
                "of the TAP service.",
                "The default value would be",
                "<code>&lt;" + baseParam_.getName() + "&gt;/"
                             + label + "</code>,",
                "but it may be influenced by the chosen",
                "<code>" + INTFPARAM_NAME + "</code> value,",
                "and it can be further overridden by setting this value.",
                "</p>",
            } );
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
