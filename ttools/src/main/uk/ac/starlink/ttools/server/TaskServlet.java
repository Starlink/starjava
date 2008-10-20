package uk.ac.starlink.ttools.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.InvokeUtils;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Servlet implementation which allows STILTS commands to be executed
 * server-side.
 * Both GET and POST methods are accepted.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2008
 */
public class TaskServlet extends HttpServlet {

    private ObjectFactory taskFactory_;
    private StarTableFactory tableFactory_;
    private StarTableOutput tableOutput_;
    private JDBCAuthenticator jdbcAuth_;
    private Collection taskNameSet_;

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        taskFactory_ = Stilts.getTaskFactory();
        tableOutput_ = new StarTableOutput();
        jdbcAuth_ = null;
        ServletContext context = config.getServletContext();
        StiltsContext sContext = new StiltsContext( context );

        /* Set up list of tasks which will be provided. */
        String taskList =
            context.getInitParameter( StiltsContext.TASKLIST_PARAM );
        taskNameSet_ = Arrays.asList( getTaskNames( taskFactory_, taskList ) );

        /* Set up table factory. */
        tableFactory_ = sContext.getTableFactory();
    }


    public void destroy() {
        tableFactory_ = null;
        super.destroy();
    }

    public String getServletInfo() {
        return "STILTS Servlet " + Stilts.getVersion()
             + "; See http://www.starlink.ac.uk/stilts/";
    }

    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws IOException, ServletException {
        try {
            process( request, response );
        }
        catch ( Throwable e ) {
            e.printStackTrace( System.err );
            replyError( response, 400, e );
        }
    }

    protected void doPost( HttpServletRequest request,
                           HttpServletResponse response )
            throws IOException, ServletException {
        try {
            process( request, response );
        }
        catch ( Throwable e ) {
            e.printStackTrace( System.err );
            replyError( response, 400, e );
        }
    }

    /**
     * Does the work for a GET or POST request - they have the same behaviour.
     *
     * @param  request  request
     * @param  response  response
     */
    protected void process( HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException {
        response.setHeader( "STILTS-Version", Stilts.getVersion() );
        ServletEnvironment env =
            new ServletEnvironment( request, response, tableFactory_,
                                    tableOutput_, jdbcAuth_ );
        String subpath = request.getPathInfo();
        String taskName = subpath.replaceAll( "^/*", "" );
        if ( taskNameSet_.contains( taskName ) ) {
            assert taskFactory_.isRegistered( taskName );
            Task task;
            try { 
                task = (Task) taskFactory_.createObject( taskName );
            }
            catch ( LoadException e ) {
                replyError( response, 500, e );
                return;
            }

            /* If we have a request for help information, write help. */
            if ( env.isHelp() ) {
                response.setStatus( 200 );
                response.setContentType( "text/html" );
                ServletOutputStream out = response.getOutputStream();
                out.println( "<html>" );
                out.println( "<head><title>STILTS " + taskName
                           + "</title></head>" );
                out.println( "<body>" );
                writeTaskHelp( request.getServletPath(), out, taskName, task );
                out.println( "</body>" );
                out.println( "</html>" );
                out.flush();
            }

            /* Otherwise, try to execute the task with the given parameters. */
            else {
                Executable exec;
                try {
                    exec = task.createExecutable( env );
                }
                catch ( TaskException e ) {
                    replyError( response, 400, e );
                    return;
                }
                catch ( Throwable e ) {
                    e.printStackTrace( System.err );
                    replyError( response, 500, e );
                    return;
                }
                try {
                    exec.execute();
                }
                catch ( TaskException e ) {
                    if ( ! response.isCommitted() ) {
                        replyError( response, 400, e );
                    }
                    else {
                        throw new ServletException(
                            "Error during task execution", e );
                    }
                }
            }
        }

        /* If no task was identified, write some help text. */
        else {
            response.setContentType( "text/html" );
            ServletOutputStream out = response.getOutputStream();

            /* If it looked like a help request, write lots of useful
             * information. */
            if ( env.isHelp() ) {
                response.setStatus( 200 );
                out.println( "<html>" );
                out.println( "<head><title>STILTS</title></head>" );
                out.println( "<body>" );
                out.println( "<h2>STILTS</h2>" );
                out.println( "<p>This is STILTS, the STIL Tool Set.<br />" );
                out.println( "Running in server mode.</p>" );
                out.println( "<p><b>STILTS version:</b> "
                           + Stilts.getVersion() + "<br />" );
                out.println( "<b>STIL version:</b> "
                           + IOUtils.getResourceContents( StarTable.class,
                                                          "stil.version" )
                           + "<br />" );
                out.println( "<b>JVM:</b> " + InvokeUtils.getJavaVM() );
                out.println( "</p>" );
                out.println( "<p><b>Author:</b> "
                           + "<a href='http://www.star.bris.ac.uk/~mbt/'>"
                           + "Mark Taylor" + "</a><br />" );
                out.println( "<b>WWW:</b> "
                           + "<a href='http://www.starlink.ac.uk/stilts/'>"
                           + "http://www.starlink.ac.uk/stilts/</a>" );
                out.println( "</p>" );
                writeStiltsHelp( request.getServletPath(), out );
                out.println( "</body>" );
                out.println( "</html>" );
            }

            /* Otherwise an error message with some other pointers. */
            else {
                response.setStatus( 400 );
                out.println( "<html>" );
                out.println( "<head><title>No such task: "
                           + taskName + "</title></head>" );
                out.println( "<body>" );
                out.println( "<h2>No such task: " + taskName + "</h2>" );
                writeStiltsHelp( request.getServletPath(), out );
                out.println( "</body>" );
                out.println( "</html>" );
            }
            out.flush();
            out.close();
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
            pout.close();
        }
    }

    /**
     * Writes HTML information about known tasks to an output stream.
     *
     * @param  baseUrl  main servlet URL
     * @param  out  response output stream
     */
    private void writeStiltsHelp( String baseUrl, ServletOutputStream out )
            throws IOException {
        out.println( "<p>Known tasks:</p>" );
        out.println( "<ul>" );
        String[] taskNames = (String[]) taskNameSet_.toArray( new String[ 0 ] );
        for ( int i = 0; i < taskNames.length; i++ ) {
            String taskName = taskNames[ i ];
            out.print( "<li>" );
            out.print( "<a href='" + baseUrl + "/" + taskName + "'>"
                       + taskName + "</a>" );
            out.print( "</li>" );
            out.println();
        }
        out.println( "</ul>" );
    }

    /**
     * Writes HTML information about a given task to an output stream.
     *
     * @param  baseUrl  main servlet URL
     * @param  out  response output stream
     * @param  taskName  task name
     * @param  task   task object
     */
    private void writeTaskHelp( String baseUrl, ServletOutputStream out,
                                String taskName, Task task )
            throws IOException {
        out.println( "<h2>STILTS task <code>" + taskName + "</code></h2>" );

        out.println( "<h3>Purpose</h3>" );
        out.println( task.getPurpose() );

        out.println( "<h3>Usage</h3>" );
        out.println( "<p><code>" );
        out.println( baseUrl + "/" + taskName );
        out.println( "<blockquote>" );
        Parameter[] params = task.getParameters();
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            out.print( " <font color='green'>"
                     + ( i > 0 ? "&amp;" : "?" )
                     + "</font> " );
            out.print( "<a href='#" + param.getName() + "'>"
                     + escape( param.getName() ) + "</a>" + "="
                     + "<font color='brown'>" + escape( param.getUsage() )
                     + "</font>" );
            out.println( "<br />" );
        }
        out.println( "</blockquote>" );
        out.println( "</code></p>" );

        out.println( "<h3>Parameters</h3>" );
        out.println( "<dl>" );
        Arrays.sort( params, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                Parameter p1 = (Parameter) o1;
                Parameter p2 = (Parameter) o2;
                return ((Parameter) o1).getName()
                      .compareTo( ((Parameter) o2).getName() ); 
            }
        } );
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            out.println( "<dt><b><a name='" + param.getName() + "'>"
                       + escape( param.getName() + " = " + param.getUsage() )
                       + "</a></b></dt>" );
            out.println( "<dd>" + param.getDescription() );
            out.println( "</dd>" );
        }
        out.println( "</dl>" );
    }

    /**
     * Returns an array of the known tasks from a given task factory
     * based on a space-separated list. 
     * An exception will be thrown if unknown tasks appear.
     *
     * @param  taskList   space-separated list of tasks to include
     * @param  taskFactory  STILTS task factory
     * @throws   IllegalArgumentException  if any unknown task name
     *           is included in <code>taskList</code>
     */
    public static String[] getTaskNames( ObjectFactory taskFactory,
                                         String taskList ) {
        Collection knownTasks =
            new HashSet( Arrays.asList( taskFactory.getNickNames() ) );
        if ( taskList == null || taskList.trim().length() == 0 ) {
            return (String[]) knownTasks.toArray( new String[ 0 ] );
        }
        else {
            String[] taskNames = taskList.split( "\\s+" );
            List okList = new ArrayList();
            for ( int i = 0; i < taskNames.length; i++ ) {
                String task = taskNames[ i ];
                if ( knownTasks.contains( task ) ) {
                    okList.add( task );
                }
                else {
                    throw new IllegalArgumentException( "Unknown task "
                                                      + task );
                }
            }
            return (String[]) okList.toArray( new String[ 0 ] );
        }
    }

    /**
     * Escapes a plain text string for use in HTML output.
     *
     * @param  text  input
     * @return   escaped text
     */
    private static String escape( String text ) {
        return text.replaceAll( "&", "&amp;" )
                   .replaceAll( "<", "&lt;" )
                   .replaceAll( ">", "&gt;" );
    }
}
