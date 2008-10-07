package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Resource;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.server.ServletEnvironment;
import uk.ac.starlink.ttools.server.StiltsServlet;

/**
 * Runs STILTS in server mode.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2008
 */
public class StiltsServer implements Task {

    private final IntegerParameter portParam_;
    private final Parameter baseParam_;

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
            "The default is <code>/stilts</code>, which means that",
            "for instance requests to execute task <code>plot2d</code>",
            "should be directed to the URL",
            "<code>http://host:portnum" + baseDefault + "/plot2d" 
                + "?name=value&amp;name=value..." + "</code>",
        } );
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
        final String base = baseParam_.stringValue( env );
        final PrintStream out = env.getOutputStream();
        return new Executable() {
            public void execute() throws IOException {
                HttpServer server = new HttpServer();
                server.addListener( new InetAddrPort( port ) );
                HttpContext context = server.getContext( "/" );
                ServletHandler handler = new ServletHandler();
                handler.addServlet( "STILTS", base + "/*",
                                    StiltsServlet.class.getName() );
                context.addHandler( handler );
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
}
