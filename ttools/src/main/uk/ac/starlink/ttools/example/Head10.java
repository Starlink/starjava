package uk.ac.starlink.ttools.example;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.TablePipe;

/**
 * Minimal example of invoking a stilts table processing task
 * programmatically using the parameter system.
 *
 * @author   Mark Taylor
 * @since    3 Oct 2014
 */
public class Head10 {
    public static void main( String[] args ) throws TaskException, IOException {

        /* Get input table file name from the command line. */
        if ( args.length != 1 ) {
            System.err.println( "\n   Usage: " + Head10.class.getName() 
                                               + " <table>\n" );
        }
        String fileName = args[ 0 ];

        /* Set up and populate an execution environment
         * with parameter values.  An alternative to setting the value
         * of the "in" parameter with a string would be to use a StarTable
         * object. */
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in", fileName );
        env.setValue( "cmd", "head 10" );

        /* Create and execute a task of the right kind with these parameters. 
         * The TablePipe class is the one used by the stilts tpipe command. */
        Task task = new TablePipe();
        task.createExecutable( env ).execute();

        /* Retrieve the output table from the execution environment.
         * The output parameter is "omode", which is the parameter of
         * type ProcessingMode in the tpipe task. */
        StarTable outTable = env.getOutputTable( "omode" );

        /* Write the result to standard output. */
        new StarTableOutput().writeStarTable( outTable, "-", "ascii" );
    }
}
