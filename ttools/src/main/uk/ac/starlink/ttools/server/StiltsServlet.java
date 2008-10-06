package uk.ac.starlink.ttools.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Servlet implementation which allows STILTS commands to be executed
 * server-side.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2008
 */
public class StiltsServlet extends HttpServlet {

    private ObjectFactory taskFactory_;
    private StarTableFactory tableFactory_;
    private StarTableOutput tableOutput_;
    private JDBCAuthenticator jdbcAuth_;

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        taskFactory_ = Stilts.getTaskFactory();
        tableFactory_ = new StarTableFactory();
        tableOutput_ = new StarTableOutput();
        jdbcAuth_ = null;
    }

    public void destroy() {
        super.destroy();
    }

    public String getServletInfo() {
        return "STILTS Servlet " + Stilts.getVersion()
             + "; See http://www.starlink.ac.uk/stilts/";
    }

    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws IOException, ServletException {
        process( request, response );
    }

    protected void doPost( HttpServletRequest request,
                           HttpServletResponse response )
            throws IOException, ServletException {
        process( request, response );
    }

    /**
     * Does the work for a GET or POST request - they have the same behaviour.
     *
     * @param  request  request
     * @param  response  response
     */
    private void process( HttpServletRequest request,
                          HttpServletResponse response )
            throws IOException, ServletException {
        response.setHeader( "STILTS-Version", Stilts.getVersion() );
        ServletEnvironment env =
            new ServletEnvironment( request, response, tableFactory_,
                                    tableOutput_, jdbcAuth_ );
        String subpath = request.getPathInfo();
        String taskName = subpath.replaceAll( "^/*", "" );
        if ( taskFactory_.isRegistered( taskName ) ) {
            Task task;
            try { 
                task = (Task) taskFactory_.createObject( taskName );
            }
            catch ( LoadException e ) {
                response.sendError( 500, serialize( e ) );
                return;
            }
            Executable exec;
            try {
                exec = task.createExecutable( env );
            }
            catch ( TaskException e ) {
                response.sendError( 400, serialize( e ) );
                return;
            }
            catch ( Throwable e ) {
                e.printStackTrace( System.err );
                response.sendError( 500, serialize( e ) );
                return;
            }
            try {
                exec.execute();
            }
            catch ( TaskException e ) {
                if ( ! response.isCommitted() ) {
                    response.sendError( 400, serialize( e ) );
                }
                else {
                    throw new ServletException( "Error during task execution",
                                                e );
                }
            }
        }
        else {
            response.sendError( 400, "No such task " + taskName );
        }
    }

    /**
     * Turns a throwable into a string which can be returned to the caller
     * in a servlet response.
     *
     * @param  error  throwable to serialize
     * @return   string incorporating stacktrace
     */
    private static String serialize( Throwable error ) {
        try {
            ByteArrayOutputStream bufout = new ByteArrayOutputStream();
            PrintStream pout = new PrintStream( bufout );
            error.printStackTrace( pout );
            pout.flush();
            pout.close();
            return new String( bufout.toByteArray(), "UTF-8" );
        }
        catch ( IOException e ) {
            return error.toString();
        }
    }
}
