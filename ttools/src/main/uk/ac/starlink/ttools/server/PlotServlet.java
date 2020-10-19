package uk.ac.starlink.ttools.server;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotScene;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DiskCache;
import uk.ac.starlink.ttools.plot2.task.PlotConfiguration;
import uk.ac.starlink.ttools.plot2.task.TypedPlot2Task;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;
import uk.ac.starlink.util.Pair;

/**
 * Servlet class that can supply interactive STILTS plots in a web page.
 *
 * <p>The service is self-documenting, so start it up and look at the
 * top-level or 400 response to see the commands that it accepts.
 *
 * @author   Mark Taylor
 * @since    13 Dec 2019
 */
public class PlotServlet extends HttpServlet {

    private PlotCaching caching_;
    private ObjectFactory<TypedPlot2Task<?,?>> taskFactory_;
    private Set<String> taskNameSet_;
    private Map<String,PlotService> serviceMap_;
    private StarTableFactory tableFactory_;
    private DataStoreFactory dataStoreFactory_;
    private DiskCache imgCache_;
    private StarTableOutput tableOutput_;
    private JDBCAuthenticator jdbcAuth_;
    private SoftCache<String,PlotSession<?,?>> sessionCache_;
    private String servletId_;
    private String acao_;
    private Logger logger_;
    private static final String UTF8 = "UTF-8";
    private static final String DFLT_ALLOWORIGINS = "*"; // think it's safe
    private static final Map<String,String> MIME_TYPES = mimeTypes();

    /** Replacement token for server base URL. */
    public static final String BASEURL_SUBST = "%PLOTSERV_URL%";

    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        try {
            PlotSession.init();
        }
        catch ( IOException e ) {
            throw new ServletException( "Initialisation error: " + e, e );
        }
        caching_ = PlotCaching.createFullyCached();
        taskFactory_ = Stilts.getPlot2TaskFactory();
        taskNameSet_ =
            new HashSet<>( Arrays.asList( taskFactory_.getNickNames() ) );
        serviceMap_ = createServiceMap();
        jdbcAuth_ = null;
        ServletContext context = config.getServletContext();
        StiltsContext sContext = new StiltsContext( context );
        tableFactory_ = sContext.getTableFactory();
        dataStoreFactory_ = sContext.getDataStoreFactory();
        imgCache_ = sContext.getImageCache();
        tableOutput_ = new StarTableOutput();
        sessionCache_ = new SoftCache<String,PlotSession<?,?>>();
        servletId_ = createId( this );
        String acao = sContext.getAllowOrigins();
        acao_ = acao == null ? DFLT_ALLOWORIGINS : acao;
        logger_ = Logger.getLogger( "uk.ac.starlink.ttools.server" );
    }

    @Override
    public void destroy() {
        sessionCache_.clear();
        super.destroy();
    }

    @Override
    public String getServletInfo() {
        return "STILTS plot2 servlet " + Stilts.getVersion()
             + "; See https://www.starlink.ac.uk/stilts/";
    }

    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws IOException, ServletException {
        doProcess( request, response );
    }

    @Override
    protected void doPost( HttpServletRequest request,
                           HttpServletResponse response )
            throws IOException, ServletException {
        doProcess( request, response );
    }

    private void doProcess( HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException {
        if ( acao_ != null ) {
            response.setHeader( "Access-Control-Allow-Origin", acao_ );
        }
        try {
            process( request, response );
        }
        catch ( IOException|ServletException|RuntimeException|Error e ) {
            e.printStackTrace( System.err );
            throw e;
        }
    }

    /**
     * This does the work for a GET or POST operation.
     *
     * @param  request  request, supplying parameters
     * @param  response   response, to which HTTP output is made
     */
    private void process( HttpServletRequest request,
                          HttpServletResponse response )
            throws IOException, ServletException {
        response.setHeader( "X-STILTS-Version", Stilts.getVersion() );

        /* Break up the request URL into the parts we need.
         * The request.getPathInfo() method is nearly what we need here
         * to get the subPath, but that doesn't get the escaping quite
         * right for our purposes.  We are using "&" characters as
         * delimiters in the *path* part of the URL (this does not concern
         * the query part), so we need them to be distinct from
         * percent-encoded ampersands (%26) - getPathInfo does the
         * escape decoding already. */
        String prefix = request.getContextPath() + request.getServletPath();
        String path = request.getRequestURI();
        assert path.startsWith( prefix );
        String subPath = path.substring( prefix.length() );

        /* If this is a request to serve a static resource file
         * (with or without a bit of editing), do that. */
        if ( subPath.indexOf( '/' ) == 0 &&
             subPath.length() > 1 &&
             getClass().getResource( subPath.substring( 1 ) ) != null ) {
            String resourceFile = subPath.substring( 1 );
            int idot = resourceFile.lastIndexOf( '.' );
            String extension = idot >= 0 ? resourceFile.substring( idot )
                                         : null;
            String contentType = MIME_TYPES.get( extension );
            if ( contentType != null ) {
                response.setContentType( contentType );
            }
            else {
                logger_.warning( "Unknown MIME type for " + resourceFile );
            }
            response.setStatus( 200 );
            InputStream rIn = getClass().getResourceAsStream( resourceFile );
            if ( ".py".equals( extension ) ||
                 ".ipynb".equals( extension ) ||
                 ".html".equals( extension ) ) {
                String requestUrl = request.getRequestURL().toString();
                int islash = requestUrl.lastIndexOf( '/' );
                String baseUrl = requestUrl.substring( 0, islash );
                BufferedReader rdr =
                    new BufferedReader( new InputStreamReader( rIn, UTF8 ) );
                BufferedWriter writer =
                    new BufferedWriter(
                        new OutputStreamWriter( response.getOutputStream(),
                                                UTF8 ) );
                for ( String line = null; ( line = rdr.readLine() ) != null; ) {
                    writer.write( line.replaceAll( BASEURL_SUBST, baseUrl ) );
                    writer.newLine();
                }
                writer.flush();
            }
            else {
                IOUtils.copy( rIn, response.getOutputStream() );
            }
            rIn.close();
            return;
        }

        /* Otherwise, it should be some dynamic content.
         * Parse the request and find out what kind of action is required. */
        Matcher matcher = Pattern.compile( "/+([a-z]+)/+(.*)" )
                         .matcher( subPath );
        final String plotTxt;
        final PlotService service;
        if ( matcher.matches() ) {
            String requestMode = matcher.group( 1 );
            service = serviceMap_.get( requestMode );
            plotTxt = matcher.group( 2 );
        }
        else {
            plotTxt = null;
            service = null;
        }

        /* If the request cannot be parsed, or the required action is
         * not known, respond with a help page. */
        if ( service == null ) {
            response.setStatus( 400 );
            response.setContentType( "text/html" );
            response.getOutputStream().println( getHelpHtml( request ) );
            return;
        }

        /* Make sure we have a session ID.
         * It had better be unique within this servlet. */
        String sessionId = getSingleParameter( request, "sessionId" );
        if ( sessionId == null ) {

            /* In some cases (initiating a session) it is permissible to
             * omit the session ID - in that case we can make up one here. */
            if ( service.canCreateSession() ) {
                sessionId = servletId_ + "-" + createId( plotTxt );
            }

            /* Otherwise the session ID is essential, so respond with an error
             * if it's not present. */
            else {
                response.sendError( 400, "No sessionId" );
                return;
            }
        }

        /* Get a session object corresponding to the session ID.
         * If there is one in the cache, use that.
         * If there isn't, create one based on the plot text.
         * This latter case may obtain either because this is the
         * initial request for this plot, or because an existing session
         * has expired or the server has restarted since the last request.
         * We don't distinguish between the two cases.
         * Recreating an existing session will lose state (the aspect
         * will return to its initial default) but it is likely to be
         * better than failing to service the request at all. */
        PlotSession<?,?> session = sessionCache_.get( sessionId );
        if ( session == null ) {
            try {
                session = createSession( plotTxt, response );
            }
            catch ( Throwable e ) {
                replyError( response, 500, e );
                return;
            }
            if ( session == null ) {
                response.sendError( 400, "Bad plot request" );
                return;
            }
            sessionCache_.put( sessionId, session );
            sessionCache_.purge();
        }

        /* Service the session-specific request. */
        service.sessionRespond( session, request, response );
    }

    /**
     * Returns a new session object that can be used to make
     * successive requests for updated plot images.
     *
     * @param   plotTxt   text of plot command in the form
     *                    plot2xxx&amp;n1=v1&amp;n2=v2&amp;...;
     *                    each of the the ampersand-delimited words is
     *                    application/x-www-form-urlencoded
     * @param  response  response object to which result will be written
     */
    private PlotSession<?,?> createSession( String plotTxt,
                                            HttpServletResponse response )
            throws IOException, InterruptedException,
                   LoadException, TaskException {
        List<String> words =
            new ArrayList<>( Arrays.asList( plotTxt.split( "&" ) ) );
        String taskName = words.remove( 0 );
        if ( ! taskNameSet_.contains( taskName ) ) {
            return null;
        }
        assert taskFactory_.isRegistered( taskName );
        TypedPlot2Task<?,?> task = taskFactory_.createObject( taskName );
        List<Pair<String>> nvPairs = new ArrayList<>();
        for ( String word : words ) {
            int ieq = word.indexOf( '=' );
            if ( ieq <= 0 ) {
                throw new TaskException( "Word \"" + word + "\" not of form "
                                       + "<name>=<value>" );
            }
            String name = URLDecoder.decode( word.substring( 0, ieq ), UTF8 );
            String value = URLDecoder.decode( word.substring( ieq + 1 ), UTF8 );
            nvPairs.add( new Pair<String>( name, value ) );
        }
        PlotServletEnvironment env =
            new PlotServletEnvironment( response, nvPairs, tableFactory_,
                                        tableOutput_, jdbcAuth_,
                                        dataStoreFactory_ );
        Executable exec = task.createExecutable( env );
        exec.execute();
        PlotConfiguration<?,?> plotConfig = env.getPlotConfiguration();
        GraphicExporter exporter = env.getGraphicExporter();
        return createPlotSession( plotTxt, plotConfig, exporter );
   }

   /**
    * Creates a session object from configuration.
    *
    * @param  plotTxt   unencoded plot setup string
    * @param  plotConfig   plot configuration object
    * @param  exporter   graphic exporter
    * @return   new session
    */
   private <P,A> PlotSession<P,A>
            createPlotSession( String plotTxt,
                               PlotConfiguration<P,A> plotConfig,
                               GraphicExporter exporter )
            throws IOException, InterruptedException {
        DataStore dataStore = plotConfig.createDataStore( null );
        PlotScene<P,A> scene =
            plotConfig.createPlotScene( dataStore, caching_ );
        Navigator<A> navigator = plotConfig.createNavigator();
        Dimension size = plotConfig.getPlotSize();
        return new PlotSession<P,A>( plotTxt, scene, navigator,
                                     exporter, dataStore, size, imgCache_ );
    }

    /**
     * Utility method to return a plain text HTTP response.
     *
     * @param  response  destination object
     * @param  code   3-digit HTTP response code
     * @param  text   accompanying text
     */
    private void replyPlain( HttpServletResponse response, int code,
                             String text )
           throws IOException, ServletException {
        if ( response.isCommitted() ) {
            throw new ServletException( "Error after response commit" );
        }
        else {
            response.setStatus( code );
            response.setContentType( "text/plain" );
            PrintStream pout = new PrintStream( response.getOutputStream() );
            pout.println( text );
            pout.flush();
        }
    }

    /**
     * Writes error information to the response.
     *
     * @param   response  destination
     * @param   code   3-digit HTTP response code
     * @param   error  exception to be passed to caller
     */
    private void replyError( HttpServletResponse response, int code,
                             Throwable error )
            throws IOException, ServletException {
        if ( response.isCommitted() ) {
            throw new ServletException( "Error after response commit", error );
        }
        else {
            response.setStatus( code );
            response.setContentType( "text/plain" );
            PrintStream pout = new PrintStream( response.getOutputStream() );
            error.printStackTrace( pout );
            pout.flush();
        }
    }

    /**
     * Utility method to return a probably-unique string corresponding
     * to a given object at the current time.  Will return different
     * values if called on different occasions.
     *
     * @param  object  object to identify
     * @return  identifier string
     */
    private static String createId( Object object ) {
        int value = 9901;
        value = 23 * value + (int) System.nanoTime();
        value = 23 * value + System.identityHashCode( object );
        return String.format( "%08x", value );
    }

    /**
     * Returns the text of an HTML page explaining usage.
     *
     * @param  request  request for which help is required
     */
    private String getHelpHtml( HttpServletRequest request ) {
        String hostPart = request.getRequestURL().toString()
                                 .replaceFirst( "([^/:])/.*", "$1" );
        String prefix = request.getContextPath()
                      + request.getServletPath();
        return String.join( "\n",
            "<html>",
            "<head>",
            "<meta charset='UTF-8'>",
            "<title>STILTS Plot server bad request</title>",
            "</head>",
            "<body>",
            "<h2>Malformed plot request</h2>",
            "<p>This is STILTS plot server version ",
            Stilts.getVersion() + ".",
            "</p>",
            "<p>The requested URL did not conform to the syntax requirements",
            "of this plot servlet.",
            "For documentation, please see below.",
            "</p>",
            "<h2>Plot Server Documentation</h2>",
            getHtmlDocumentation( hostPart, prefix ),
            "</body>",
            "</html>",
        "" );
    }

    /**
     * Returns service syntax and semantics documentation in HTML format.
     *
     * @return   HTML string
     */
    private String getHtmlDocumentation( String hostPart, String prefix ) {
        String baseUrl = hostPart + prefix;
        String standaloneExample =
            baseUrl + "/" + PlotSession.HTML_SERVICE.getServiceName()
                    + "/plot2plane&amp;"
                    + "layer1=function&amp;fexpr1=0.5%2B0.4*x*sin(x*40)";
        String plotDocref = 
            "<a href='http://www.starlink.ac.uk/stilts/sun256/plot2.html'"
             + ">SUN/256</a>.";
        String jslibRef = 
            "<a href='" + prefix + "/" + PlotSession.JS_FILE + "'"
                                 + ">" + PlotSession.JS_FILE + "</a>";
        return String.join( "\n",
            "<h3>Usage and Examples</h3>",
            "<ul>",
            "<li>Basic standalone plot example: "
               + alink( standaloneExample ) + "</li>",
            "<li>Library for embedding interactive plots: "
               + jslibRef + "</li>",
            "</ul>",
            "<p>The easiest way to insert interactive plots",
            "in your web pages is by using",
            jslibRef,
            "as in the examples above,",
            "but you can also write your own JavaScript client using the API",
            "described below.",
            "</p>",
            "",
            getHtmlSyntaxDocumentation( baseUrl, serviceMap_, plotDocref )
        );
    }

    /**
     * Returns user documentation in XML format explaining the
     * RESTful interface of the plot server.
     *
     * @return   documentation text in SUN-friendly XML
     */
    public static String getXmlSyntaxDocumentation()
            throws IOException, javax.xml.transform.TransformerException {
        String plotDocref = "<ref id='plot2'/>";
        String helpHtml = getHtmlSyntaxDocumentation( "&lt;base-url&gt;",
                                                      createServiceMap(),
                                                      plotDocref );
        return DocUtils.fromXhtml( helpHtml );
    }

    /**
     * Returns user documentation in HTML format explaining the
     * RESTful interface of the plot server.
     *
     * @param  baseUrl  base URL of plot service
     * @param  serviceMap   gives available service endpoints
     * @param  plotDocref   literal HTML text to use for referring to
     *                      STILTS plot2* command syntax
     * @return   documentation text in simple HTML
     */
    private static String
            getHtmlSyntaxDocumentation( String baseUrl,
                                        Map<String,PlotService> serviceMap,
                                        String plotDocref ) {
        StringBuffer sbuf = new StringBuffer().append( String.join( "\n",
            "<h3>General URL Syntax</h3>",
            "<p>The plot service accepts URLs of the form",
            "<pre>",
            "   &lt;base-url&gt;/&lt;action-type&gt;/&lt;plot-spec&gt;"
             + "[?&lt;session-id&gt;&amp;&lt;arg-list&gt;]",
            "</pre>",
            "</p>",
            "<p>These parts are expanded as follows:",
            "<dl>",
        "" ) );
        if ( baseUrl.startsWith( "http" ) ) {
            sbuf.append( String.join( "\n",
                "<dt><code>&lt;base-url&gt;</code></dt>",
                "<dd>For this service, the base URL is <code><a href='"
                    + baseUrl + "'>" + baseUrl + "</a></code>.",
                "    </dd>",
            "" ) );
        }
        sbuf.append( String.join( "\n",
            "<dt><code>&lt;action-type&gt;</code></dt>",
            "<dd>The action type string determines the kind of request,",
            "    and is one of the strings",
                 serviceMap
                .keySet()
                .stream()
                .map( s -> "\"<code>" + s + "</code>\"" )
                .collect( Collectors.joining( ", " ) ) + ";",
            "    see below for details.",
            "    </dd>",
            "<dt><code>&lt;plot-spec&gt;</code></dt>",
            "<dd>The plot specification is an ampersand-separated",
            "    STILTS plot command string; the form is \"<code>"
                 + "&lt;command-name&gt;&amp;&lt;arg-name&gt;=&lt;value&gt;"
                                     + "&amp;&lt;arg-name&gt;=&lt;value&gt;..."
                 + "</code>\"",
            "    for instance \"<code>"
                 + "plot2sky&amp;layer1=mark&amp;in1=stars.vot&amp;"
                 + "lon1=ra&amp;lat1=dec"
                 + "</code>\".",
            "   See STILTS plotting documentation in",
            "   " + plotDocref,
            "   for command syntax.",
            "   Note that although this part contains",
            "   <code>&amp;</code>-separated <code>name=value</code> pairs",
            "   which are syntactically",
            "   <code>application/x-www-form-urlencoded</code>",
            "   it is part of the URI path and <em>not</em> a URI query string",
            "   since it does not come after a question mark",
            "   ('<code>?</code>')",
            "   and it <em>cannot</em> be supplied by POSTing parameters.",
            "   </dd>",
            "<dt><code>&lt;session-id&gt;</code></dt>",
            "<dd>The session identifier is of the form",
            "    \"<code>sessionId=&lt;unique-string&gt;</code>\"",
            "    and it serves to maintain the state of the plot",
            "    between requests (so that for instance a navigation action",
            "    starts from where the last one left off).",
            "    The <code>&lt;unique-string&gt;</code> string",
            "    is chosen by the client;",
            "    any string value is permitted, but it's up to the client",
            "    to pick something that is unlikely to be chosen",
            "    by other unrelated clients on the same or different machines.",
            "    Incorporating a high-resolution timestamp is a good bet.",
            "    In case of a collision, confusing results may ensue,",
            "    but it's probably not necessary to resort to",
            "    cryptographic-grade hashing.",
            "    This part is not necessary for the",
            "    <code>\"" + PlotSession.HTML_SERVICE.getServiceName()
                           + "\"</code>",
            "    <code>&lt;action-type&gt;</code>.",
            "</dd>",
            "<dt><code>&lt;arg-list&gt;</code></dt>",
            "<dd>A list of zero or more ampersand-separated",
            "    \"<code>&lt;name&gt;=&lt;value&gt;</code>\" parameters,",
            "    specific to the <code>&lt;action-type&gt;</code>;",
            "    see below for details.",
            "    </dd>",
            "</dl>",
            "</p>",
            "<p>The <code>[?&lt;session-id&gt;&amp;&lt;arg-list&gt;]</code>",
            "part of the URL is an",
            "<code>application/x-www-form-urlencoded</code> query-string,",
            "and may be supplied as a POST body rather than",
            "as part of the GET query if preferred.",
            "Note that does <em>not</em> apply to the",
            "<code>&lt;plot-spec&gt;</code> part,",
            "which is in RFC3986 terms part of the <em>path</em>",
            "and not part of the <em>query</em>.",
            "</p>",
            "",
            "<h3>Available Action Types</h3>",
            "<p>The options for the various different values of the",
            "<code>&lt;action-type&gt;</code> string,",
            "with their associated parameters and response values,",
            "are as follows:",
            "<dl>",
        "" ) );
        for ( PlotService service : serviceMap.values() ) {
            sbuf.append( "<dt><code>" )
                .append( service.getServiceName() )
                .append( "</code></dt>\n" )
                .append( "<dd>" )
                .append( service.getXmlDescription() );
            sbuf.append( "</dd>\n" );
        }
        sbuf.append( String.join( "\n",
            "</dl>",
            "</p>",
        "" ) );
        return sbuf.toString();
    }

    /**
     * Utility method to interrogate a single-valued request parameter.
     *
     * @param  request  request
     * @param  name  parameter name
     * @return   value of unique parameter with given name, or null
     */
    private static String getSingleParameter( HttpServletRequest request,
                                              String name ) {
        Object value = request.getParameterMap().get( name );
        if ( value == null ) {
            return null;
        }
        else if ( value instanceof String[] ) {
            String[] values = (String[]) value;
            return values.length == 1 ? values[ 0 ] : null;
        }
        if ( value instanceof String ) {
            return (String) value;
        }
        else {
            return null;
        }
    }

    /**
     * Returns an extension-&gt;content-type map for resources that
     * may be served by this servlet.
     * Map keys include the dot (for instance ".html")
     *
     * @return  map from extension to content-type
     */
    private static Map<String,String> mimeTypes() {
        Map<String,String> map = new HashMap<>();
        map.put( ".txt", "text/plain" );
        map.put( ".html", "text/html" );
        map.put( ".js", "text/javascript" );
        map.put( ".png", "image/png" );
        map.put( ".py", "text/x-python" );
        map.put( ".ipynb", "application/x-ipynb+json" );
        map.put( ".xml", "application/xml" );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Utility method to produce the text for an HTML A element
     * with both src attribute and content set to a given URL-like string.
     *
     * @param  url   link target
     * @return  HTML A element text
     */
    private static String alink( String url ) {
        return new StringBuffer()
              .append( "<a href='" )
              .append( url )
              .append( "'>" )
              .append( url )
              .append( "</a>" )
              .toString();
    }

    /**
     * Returns a map of the session-specific actions supported by this servlet.
     *
     * @return   map from action name (request path element) to action
     */
    private static Map<String,PlotService> createServiceMap() {
        Map<String,PlotService> map = new LinkedHashMap<>();
        for ( PlotService service : PlotSession.SERVICES ) {
            map.put( service.getServiceName(), service );
        }
        return Collections.unmodifiableMap( map );
    }
}
