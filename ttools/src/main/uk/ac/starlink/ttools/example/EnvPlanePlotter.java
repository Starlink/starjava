package uk.ac.starlink.ttools.example;

import java.awt.Insets;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.task.PlanePlot2Task;
import uk.ac.starlink.ttools.plot2.task.PlotDisplay;
import uk.ac.starlink.ttools.task.MapEnvironment;

/**
 * PlanePlotter implementation that uses the name/value pairs in the
 * same way as the STILTS application command-line interface to set
 * up a plot.
 *
 * This is much easier to do than the alternative, since the large
 * majority of the options will assume sensible defaults if not set.
 * It allows pretty much the same capabilities.  However, it does
 * not offer compile-time safety: there is no guarantee that a plot
 * set up like this will not generate a run-time error.
 *
 * @author   Mark Taylor
 * @since    12 Jun 2014
 */
public class EnvPlanePlotter implements SinePlot.PlanePlotter {

    public PlotDisplay createPlotComponent( StarTable table,
                                            boolean dataMayChange )
            throws InterruptedException, IOException, TaskException {

        /* Prepare an execution environment consisting of a set of name/value
         * pairs describing what plot to do on the table. */
        Environment env = configurePlotEnvironment( table );

        /* Pass the populated environment to the PlanePlot2Task object,
         * which can turn it into a JComponent containing the plot. */
        boolean caching = ! dataMayChange;
        return new PlanePlot2Task().createPlotComponent( env, caching );
    }

    /**
     * Constructs and returns an execution environment populated with
     * the name/value pairs that will cause PlanePlot2Task to make a plot
     * for the supplied table.  This method does the work of specifying
     * the plot.
     *
     * @param  table  input table
     * @return   PlanePlot2Task execution environment ready to plot
     */
    private Environment configurePlotEnvironment( StarTable table ) {

        /* Create a new empty execution environment for the stilts plot task. */
        MapEnvironment env = new MapEnvironment();

        /* Populate the environment with parameter name/value pairs.
         * For the available parameters and their values, see the user
         * documentation of the corresponding STILTS command-line task,
         * plot2plane.
         *
         * Some general points:
         *
         *   - Values can always be set as strings.
         *
         *   - In most cases, values can alternatively be set as objects of
         *     the type corresponding to the parameter.  These types are
         *     reported as part of the usage documentation for each parameter.
         *
         *   - Most parameters are optional, and will assume sensible
         *     defaults if not set.  There are several tens of parameters
         *     available, allowing detailed setup if you want to do it.
         *     In the example below, the required parameters are so marked,
         *     the others can be omitted if you want to accept default values.
         */

        /* Global parameters for the plot. */
        env.setValue( "type", "plane" );           // required
        env.setValue( "insets", new Insets( 10 , 30, 30, 8 ) );   

        /* Parameters for the first (in this case, only) layer;
         * the parameter names have a trailing (arbitrary) label "_1".
         * The values of the x_1/y_1 parameters, giving the data coordinates,
         * are names of the columns in the input table. */
        env.setValue( "layer_1", "mark" );         // required
        env.setValue( "in_1", table );             // required 
        env.setValue( "x_1", "x" );                // required
        env.setValue( "y_1", "y" );                // required
        env.setValue( "shading_1", "flat" );
        env.setValue( "shape_1", "open_circle" );
        env.setValue( "size_1", "2" );

        /* You could add more layers here. */

        /* Return the configured execution environment. */
        return env;
    }
}
