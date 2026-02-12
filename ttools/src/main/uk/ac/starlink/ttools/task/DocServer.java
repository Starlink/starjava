package uk.ac.starlink.ttools.task;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.UtilServer;
import org.astrogrid.samp.httpd.URLMapperHandler;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.StiltsDoc;
import uk.ac.starlink.util.IOUtils;

/**
 * Provides the user documentation via a local web server.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2026
 */
public class DocServer implements Task {

    private final StringParameter topicParam_;
    private final BooleanParameter singleParam_;
    private final IntegerParameter portParam_;
    private final BooleanParameter browserParam_;

    private static final String CHECKSUM_NAME = "checksum";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    public DocServer() {
        topicParam_ = new StringParameter( "topic" );
        topicParam_.setPosition( 1 );
        topicParam_.setPrompt( "Name of topic for initial display" );
        topicParam_.setDescription( new String[] {
            "<p>If supplied, gives the name of the topic to display",
            "initially in the web browser.",
            "The whole document will in any case be accessible",
            "via normal navigation links.",
            "The supplied value may be the name of a STILTS",
            "<ref id='cmdUsage'>command</ref>,",
            "<ref id='filterSteps'>filter</ref>,",
            "<ref id='outModes'>output mode</ref>,",
            "<ref id='LayerType'>plot layer type</ref>,",
            "<ref id='TableScheme'>input scheme</ref>,",
            "or the name (without the <code>.html</code> extension)",
            "of one of the HTML pages of the SUN/256 document.",
            "</p>",
        } );
        topicParam_.setNullPermitted( true );

        singleParam_ = new BooleanParameter( "single" );
        singleParam_.setPrompt( "Single HTML page?" );
        singleParam_.setDescription( new String[] {
            "<p>If true, the user document is shown in the browser as",
            "a single HTML page; this is rather long but it may be",
            "convenient for searching.",
            "If false, the multi-page version is displayed.",
            "Note in this case the web server generally does not need to",
            "continue running in order to explore the document,",
            "document, but it is not terminated automatically.",
            "</p>",
        } );
        singleParam_.setBooleanDefault( false );

        browserParam_ = new BooleanParameter( "browser" );
        browserParam_.setPrompt( "Show document in browser?" );
        browserParam_.setDescription( new String[] {
            "<p>If true, an attempt will be made to display",
            "the requested documentation in a browser on the desktop.",
            "Otherwise, the documentation service will be started and",
            "only the URL will be printed to standard output.",
            "</p>",
        } );
        browserParam_.setBooleanDefault( true );

        portParam_ = new IntegerParameter( "port" );
        portParam_.setPrompt( "Port for document HTTP server" );
        portParam_.setDescription( new String[] {
            "<p>Preferred port for the HTTP server at which the local host",
            "will serve the documentation.",
            "If that port is already in use or in certain other circumstances",
            "another port may be used.",
            "</p>",
        } );
        portParam_.setIntDefault( 3535 );
    }

    public String getPurpose() {
        return "Displays STILTS documentation in a web browser";
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            topicParam_,
            singleParam_,
            browserParam_,
            portParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {

        /* Check documentation resources are present. */
        String docBase = "/sdoc";
        String dirName = "sun256";
        DocResource docResource =
            createDocResource( StiltsDoc.class, dirName );
        if ( docResource == null ) {
            throw new TaskException( "No documentation found" );
        }

        /* Work out filename and fragment required from HTTP server. */
        String topic = topicParam_.stringValue( env );
        final String secName;
        if ( topic == null ) {
            secName = null;
        }
        else {
            secName = docResource.findTopicSection( topic );
            if ( secName == null ) {
                throw new ParameterValueException( topicParam_,
                                                   "No such topic" );
            }
        }
        boolean isSingle = singleParam_.booleanValue( env );
        final String fname;
        final String frag;
        if ( isSingle ) {
            fname = "sun256.html";
            frag = secName;
        }
        else {
            fname = secName == null ? "index.html" : ( secName + ".html" );
            frag = null;
        }

        /* Return executable. */
        int port = portParam_.intValue( env );
        boolean isBrowser = browserParam_.booleanValue( env );
        return () -> {

            /* See if a server is already running in the place we're about
             * to set one up, with the content we're about to serve. */
            String requestedServerDocBase =
                "http://" + SampUtils.getLocalhost() + ":" + port +
                docBase + "/" + dirName;
            String resourceDocBase =
                docResource.dirUrlTxt_;
            boolean isRunning =
                compareFiles( requestedServerDocBase + "/" + CHECKSUM_NAME,
                              resourceDocBase + "/" + CHECKSUM_NAME );

            /* If not, start a new one. */
            final String serverDocBase;
            if ( isRunning ) {
                serverDocBase = requestedServerDocBase;
            }
            else {
                System.setProperty( UtilServer.PORT_PROP,
                                    Integer.toString( port ) );
                UtilServer utilServer = UtilServer.getInstance();
                HttpServer server = utilServer.getServer();
                URLMapperHandler docHandler =
                    new URLMapperHandler( server,
                                          utilServer.getBasePath( docBase ),
                                          docResource.dirUrl_, true );
                server.addHandler( docHandler );
                serverDocBase =
                    server.getBaseUrl() +
                    utilServer.getBasePath( docBase + "/" + dirName );
            }

            /* Come up with the URL for the initially displayed page. */
            URI docUri = URI.create( serverDocBase + "/" + fname
                                   + ( frag == null ? "" : ( "#" + frag ) ) );

            /* Write the document URI to standard output. */
            System.out.println( docUri );

            /* Open browser with that URI if possible. */
            if ( isBrowser ) {
                Desktop desktop = getBrowserDesktop();
                if ( desktop == null ) {
                    logger_.warning( "No browser connection available" );
                }
                else {
                    desktop.browse( docUri );
                }
            }

            /* If we started an HTTP server, stay alive until killed
             * to keep serving the docs. */
            if ( ! isRunning ) {
                try {
                    Thread.sleep( Long.MAX_VALUE );
                }
                catch ( InterruptedException e ) {
                }
            }
        };
    }

    /**
     * Returns a browse-capable desktop instance, or null if none is available.
     * 
     * @return  desktop or null
     */
    private static Desktop getBrowserDesktop() {
        if ( Desktop.isDesktopSupported() ) {
            Desktop desktop = Desktop.getDesktop();
            return desktop.isSupported( Desktop.Action.BROWSE ) ? desktop
                                                                : null;
        }
        else {
            return null;
        }
    }

    /**
     * Returns an object representing the internal URL corresponding
     * to the given file/directory in the same location as the given class.
     *
     * @param  locationClazz  class locating the directory
     * @param  dirName    name of subdirectory
     * @return   new DocResource
     */
    private static DocResource createDocResource( Class<?> locationClazz,
                                                  String dirName ) {
        String cname = locationClazz.getSimpleName() + ".class";
        URL cUrl = locationClazz.getResource( cname );
        if ( cUrl == null ) {
            return null;
        }
        String cRes = cUrl.toString();
        cRes = cRes.substring( 0, cRes.length() - cname.length() ) + dirName;
        URL dirUrl;
        try {
            dirUrl = new URI( cRes ).toURL();
        }
        catch ( MalformedURLException | URISyntaxException e ) {
            return null;
        }
        return dirUrl == null ? null : new DocResource( dirUrl );
    }

    /**
     * Compare the contents of two given resources.
     *
     * @param   url1  first URL
     * @param   url2  second URL
     * @return  true iff both urls can be read from and both have
     *          identical non-empty content
     */
    private static boolean compareFiles( String url1, String url2 ) {
        try {
            String txt1 = IOUtils.readUrl( url1 );
            String txt2 = IOUtils.readUrl( url2 );
            return txt1 != null && txt1.trim().length() > 0
                && txt1.equals( txt2 );
        }
        catch ( IOException e ) {
            return false;
        }
    }

    /**
     * Represents an internal resource.
     */
    private static class DocResource {

        private final URL dirUrl_;
        private final String dirUrlTxt_;

        /**
         * Constructor.
         *
         * @param  dirUrl  URL of the top-level directory
         */
        DocResource( URL dirUrl ) {
            dirUrl_ = dirUrl;
            dirUrlTxt_ = dirUrl.toString();
        }

        /**
         * Returns the section name name corresponding to a given
         * user-specified topic string.
         * If no such section exists, null is returned.
         *
         * @param  topic  user-specified topic
         * @return   relevant file, or null if none found
         */
        String findTopicSection( String topic ) {
            for ( String prefix :
                  new String[] { "", "layer-", "scheme-", "mode-" } ) {
                String sec = prefix + topic;
                if ( canRead( sec + ".html" ) ) {
                    return sec;
                }
            }
            return null;
        }

        /**
         * Indicates whether a readable resource exists for a given
         * file within this resource's directory.
         *
         * @param  name  filename
         * @return  true iff filename resource can be read
         */
        private boolean canRead( String name ) {
            URL nameUrl = subUrl( name );
            if ( nameUrl != null ) {
                try ( InputStream in = nameUrl.openStream() ) {
                    in.read();
                    return true;
                }
                catch ( IOException e ) {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        /**
         * Returns the internal URL corresponding to a filename under
         * this object's top-level URL.
         *
         * @param  file  filename
         * @return   resource URL, or null
         */
        private URL subUrl( String file ) {
            try {
                return new URI( dirUrlTxt_ + "/" + file ).toURL();
            }
            catch ( URISyntaxException | MalformedURLException e ) {
                return null;
            }
        }
    }
}
