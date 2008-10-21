package uk.ac.starlink.ttools.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.TableFactoryParameter;

/**
 * Encapsulates servlet context aspects specific to the STILTS server mode.
 *
 * @author   Mark Taylor
 * @since    20 Oct 2008
 */
public class StiltsContext {

    private final ServletContext context_;

    /**
     * Name of the Servlet initialisation parameter which defines the
     * base URL on the server at which the TaskServlet runs.
     * A task can be accessed at the value of this parameter with
     * "/(taskname)" appended.
     */
    public static final String TASKBASE_PARAM = "stiltsTaskBase";

    /**
     * Name of the Servlet initialisation parameter which defines the
     * tasks which this servlet will provide over HTTP.
     * The value of this parameter is a space-separated list of the
     * tasks to provide.  If it is absent or empty, all tasks will be
     * provided.
     */
    public static String TASKLIST_PARAM = "stiltsTasks";

    /**
     * Name of a Servlet initialisation parameter which can be used to
     * customise table location.  If the value is non-null it will be
     * passed to the
     * {@link
     *    uk.ac.starlink.ttools.task.TableFactoryParameter#createTableFactory}
     * method to come up with a StarTableFactory which is used instead of the
     * default one.
     */
    public static String TABLEFACTORY_PARAM = "tableFactory";

    /** 
     * Constructor.
     *
     * @param  context  servlet context which provides the information for
     *                  this object
     */
    public StiltsContext( ServletContext context ) {
        context_ = context;
    }

    /**
     * Acquires a StarTableFactory suitable for use from a servlet
     * from the servlet context.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() throws ServletException {
        String tfactSpec = context_.getInitParameter( TABLEFACTORY_PARAM );
        try {
            return TableFactoryParameter.createTableFactory( tfactSpec );
        }
        catch ( UsageException e ) {
            throw new ServletException( e );
        }
    }

    /**
     * Returns the server URL below which task servlets can be accessed.
     *
     * @return   base task servlet server URL
     */
    public String getTaskBase() {
        return context_.getInitParameter( TASKBASE_PARAM );
    }
}
