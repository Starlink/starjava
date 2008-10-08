package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Resource;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.server.FormServlet;
import uk.ac.starlink.ttools.server.ServletEnvironment;
import uk.ac.starlink.ttools.server.TaskServlet;

/**
 * Runs STILTS in server mode.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2008
 */
public class StiltsServer implements Task {

    private final IntegerParameter portParam_;
    private final Parameter baseParam_;
    public static final String TASKBASE_PARAM = "stiltsTaskBase";

    /**
     * Constructor.
     */
    public StiltsServer() {
        portParam_ = new IntegerParameter( "port" );
        portParam_.setPrompt( "Server port" );
        portParam_.setDescription( new String[] {
            "<p>Port number on which the server should run.",
            "</p>",
        } );
        portParam_.setDefault( new Integer( 2112 ).toString() );

        baseParam_ = new Parameter( "basepath" );
        baseParam_.setPrompt( "Base path for server URLs" );
        String baseDefault = "/stilts";
        baseParam_.setDescription( new String[] {
            "<p>Base path on the server at which request URLs are rooted.",
            "The default is <code>" + baseDefault + "</code>, which means that",
            "for instance requests to execute task <code>plot2d</code>",
            "should be directed to the URL",
            "<code>http://host:portnum" + baseDefault + "/task/plot2d" 
                + "?name=value&amp;name=value..." + "</code>",
            "</p>",
        } );
        baseParam_.setNullPermitted( true );
        baseParam_.setDefault( baseDefault );
    }

    public String getPurpose() {
        return "Runs an HTTP server to perform STILTS commands";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            portParam_,
            baseParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final int port = portParam_.intValue( env );
        String basePath = baseParam_.stringValue( env );
        final String base = basePath == null ? "" : basePath;
        final PrintStream out = env.getOutputStream();
        return new Executable() {
            public void execute() throws IOException {
                HttpServer server = new HttpServer();
                server.addListener( new InetAddrPort( port ) );
                HttpContext context = server.getContext( "/" );
                ServletHandler handler = new ServletHandler();
                context.addHandler( handler );
                List baseList = new ArrayList();
                handler.addServlet( "STILTS Tasks", base + "/task/*",
                                    TaskServlet.class.getName() );
                baseList.add( base + "/task/" );
                handler.addServlet( "STILTS Forms", base + "/form/*",
                                    FormServlet.class.getName() );
                baseList.add( base + "/form/" );
                String[] bases = (String[]) baseList.toArray( new String[ 0 ] );
                context.addHandler( new FallbackHandler( bases ) );

                context.setInitParameter( TASKBASE_PARAM, base + "/task" );
                try {
                    server.start();
                    String url = "http://"
                               + InetAddress.getLocalHost().getHostName()
                               + ":" + port + base + "/";
                    out.println( "Server running at " + url );
                }
                catch ( IOException e ) {
                    throw e;
                }
                catch ( Exception e ) {
                    throw (IOException) new IOException( "Can't start server" )
                                       .initCause( e );
                }
            }
        };
    }

    /**
     * Handler which will be used if the requested URL does not match any
     * other registered.
     * It tells the user where to go.
     */
    private static class FallbackHandler extends NotFoundHandler {
        private final String[] paths_;

        /**
         * Constructor.
         *
         * @param  paths  array of relative URLs giving suggested starting
         *                points for a user
         */
        FallbackHandler( String[] paths ) {
            paths_ = paths;
        }

        public void handle( String pathInContext, String pathParams,
                            HttpRequest request, HttpResponse response )
                throws IOException {
            response.setContentType( "text/html" );
            response.setStatus( 404 );
            response.setReason( "Not Found" );
            PrintStream out = new PrintStream( response.getOutputStream() );
            out.println( "<html>" );
            out.println( "<head><title>404 Not Found</title></head>" );
            out.println( "<body>" );
            out.println( "<h2>No such resource</h2>" );
            out.println( "Try one of these:" );
            out.println( "<ul>" );
            for ( int i = 0; i < paths_.length; i++ ) {
                String path = paths_[ i ];
                out.println( "<li><a href='" + path + "'>" + path
                           + "</a></li>" );
            }
            out.println( "</ul>" );
            out.println( "<p><i>"
                       + "<a href='http://www.starlink.ac.uk/stilts/'>"
                       + "STILTS</a>"
                       + " Server " + Stilts.getVersion()
                       + "</i></p>" );
            out.println( "<body>" );
            out.println( "</html>" );
            out.close();
            response.commit();
        }
    }
}
