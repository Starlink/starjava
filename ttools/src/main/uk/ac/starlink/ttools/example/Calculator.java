package uk.ac.starlink.ttools.example;

import java.io.IOException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.Calc;

/**
 * Minimal example of invoking a stilts task programmatically
 * using the parameter system.
 *
 * @author   Mark Taylor
 * @since    3 Oct 2014
 */
public class Calculator {
    public static void main( String[] args ) throws TaskException, IOException {

        /* Get expression from command line. */
        if ( args.length != 1 ) {
            System.err.println( "\n   Usage: " + Calculator.class.getName()
                                               + " <expression>\n" );
            System.exit( 1 );
        }
        String expr = args[ 0 ];

        /* Set up and populate an execution environment with parameter
         * values. */
        MapEnvironment env = new MapEnvironment();
        env.setValue( "expression", expr );

        /* Create and execute a task of the right kind with these parameters.
         * The Calc class is the one used by the stilts calc command. */
        Task calcTask = new Calc();
        calcTask.createExecutable( env ).execute();

        /* Retrieve the result from the environment, and output it. */
        String result = env.getOutputText();
        System.out.println( expr + "\t=\t" + result );
    }
}
