package uk.ac.starlink.tptask;

import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.ObjectFactory;
import uk.ac.starlink.util.URLUtils;

/**
 * Table plotting top-level command-line harness.
 * This may be subsumed into the top-level Stilts class at some point.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class TPlot {

    private static final ObjectFactory taskFactory_ = createTaskFactory();

    /**
     * Main method.
     * Run with "-help" for help.
     */
    public static void main( String[] args ) {
        Loader.loadProperties();
        URLUtils.installCustomHandlers();
        LineInvoker invoker = new LineInvoker( "tplot", taskFactory_ );
        int status = invoker.invoke( args );
        if ( status != 0 ) {
            System.exit( status );
        }
    }

    /**
     * Creates the factory which can generate Tasks for use with this harness.
     *
     * @return  task factory
     */
    private static ObjectFactory createTaskFactory() {
        ObjectFactory taskFactory = new ObjectFactory( Task.class );

        taskFactory.register( "plot2d", TablePlot2D.class.getName() );
        taskFactory.register( "histogram", TableHistogram.class.getName() );

        // Eventually do it like this to avoid unnecessary class loading.
        // For now go with compiler safety.
        // String tptaskPkg = "uk.ac.starlink.tptask.";
        // taskFactory_.register( "plot2d", tptaskPkg + "TablePlot2D" );

        return taskFactory;
    }
}
