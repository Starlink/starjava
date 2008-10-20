package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.server.FormServlet;
import uk.ac.starlink.ttools.server.ServletEnvironment;
import uk.ac.starlink.ttools.server.StiltsContext;
import uk.ac.starlink.ttools.server.TaskServlet;
import uk.ac.starlink.ttools.task.TableFactoryParameter;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Runs STILTS in server mode.
 *
 * <p>Things that should be possible but currently are not:
 * <ul>
 * <li>Provide pluggable table factory</li>
 * </ul>
 *
 * @author   Mark Taylor
 * @since    6 Oct 2008
 */
public class StiltsServer implements Task {

    private final IntegerParameter portParam_;
    private final Parameter baseParam_;
    private final Parameter tasksParam_;
    private final TableFactoryParameter tfactParam_;

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

        tasksParam_ = new Parameter( "tasks" );
        tasksParam_.setPrompt( "List of tasks provided" );
        tasksParam_.setUsage( "<task-name> ..." );
        tasksParam_.setNullPermitted( true );
        tasksParam_.setDescription( new String[] {
            "<p>Gives a space-separated list of tasks which will be provided",
            "by the running server.",
            "If the value is <code>null</code> then all tasks will be",
            "available.  However, some tasks don't make a lot of sense",
            "to run from the server, so the default value is a somewhat",
            "restricted list.",
            "If the server is being exposed to external users, you might",
            "also want to reduce the list for security reasons.",
            "</p>",
        } );
        ObjectFactory taskFactory = Stilts.getTaskFactory();
        List taskList =
            new ArrayList( Arrays.asList( taskFactory.getNickNames() ) );
        taskList.removeAll( Arrays.asList( new String[] {
            "server", "funcs",
        } ) );
        StringBuffer taskBuf = new StringBuffer();
        for ( Iterator it = taskList.iterator(); it.hasNext(); ) {
            taskBuf.append( (String) it.next() );
            if ( it.hasNext() ) {
                taskBuf.append( ' ' );
            }
        }
        String tasksDefault = taskBuf.toString();
        TaskServlet.getTaskNames( taskFactory, tasksDefault );  // test no error
        tasksParam_.setDefault( tasksDefault );

        tfactParam_ = new TableFactoryParameter( "tablefactory" );
    }

    public String getPurpose() {
        return "Runs an HTTP server to perform STILTS commands";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            portParam_,
            baseParam_,
            tasksParam_,
            tfactParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final int port = portParam_.intValue( env );
        String basePath = baseParam_.stringValue( env );
        final String base = basePath == null ? "" : basePath;
        final String tasks = tasksParam_.stringValue( env );
        final String factorySpec = tfactParam_.stringValue( env );
        try {
            TaskServlet.getTaskNames( Stilts.getTaskFactory(), tasks );
        }
        catch ( Exception e ) {
            throw new ParameterValueException( tasksParam_, e.toString(), e );
        }
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

                context.setInitParameter( StiltsContext.TASKBASE_PARAM,
                                          base + "/task" );
                context.setInitParameter( StiltsContext.TASKLIST_PARAM, tasks );
                if ( factorySpec != null ) {
                    context.setInitParameter( StiltsContext.TABLEFACTORY_PARAM,
                                              factorySpec );
                }
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
