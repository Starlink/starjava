package uk.ac.starlink.task;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Invoker which only knows how to invoke a single given task.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2006
 */
public class SingleTaskInvoker {

    private String taskName_;
    private final Task task_;

    /**
     * Constructor.
     *
     * @param   task   task to invoke
     * @param   taskName  human-readable name of the task
     */
    public SingleTaskInvoker( Task task, String taskName ) {
        task_ = task;
        taskName_ = taskName;
    }

    public int invoke( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );

        /* Process flags. */
        boolean debug = false;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.charAt( 0 ) != '-' ) {
                break;
            }
            if ( "-help".equals( arg ) ) {
                it.remove();
                System.out.println( getUsage() );
                return 0;
            }
            if ( "-debug".equals( arg ) ) {
                it.remove();
                debug = true;
            }
        }

        /* Execute the task given the now-flagless command line. */
        try {
            String[] taskArgs = (String[]) argList.toArray( new String[ 0 ] );
            Environment env =
                new LineEnvironment( taskArgs, task_.getParameters() );
            task_.createExecutable( env ).execute();
        }

        /* Catch various exceptions and deal with them appropriately. */
        catch ( Exception e ) {
            if ( debug ) {
                System.err.println();
                e.printStackTrace( System.err );
            }
            System.err.println();
            InvokeUtils.summariseError( e, System.err );

            if ( e instanceof ParameterValueException ) {
            }
            else if ( e instanceof IOException ) {
            }
            else if ( e instanceof UsageException ) {
                System.err.println();
                System.err.println( getUsage() );
            }
            else if ( e instanceof AbortException ) {
                System.err.println();
                System.err.println( "User abort" );
            }
            else if ( ! debug ) {
                System.err.println();
                e.printStackTrace( System.err );
            }
            return 1;
        }

        /* Successful return. */
        return 0;
    }

    /**
     * Returns a usage string for this invoker.
     *
     * @return  usage string
     */
    public String getUsage() {

        /* Add flag usage matter. */
        StringBuffer ubuf = new StringBuffer();
        ubuf.append( "Usage: " )
            .append( taskName_ );
        String padding = ubuf.toString().replaceAll( ".", " " ) + " ";
        ubuf.append( " [-help]" )
            .append( " [-debug]" );

        /* Get an ordered list of task parameters. */
        Parameter[] params =
            InvokeUtils.sortParameters( task_.getParameters() );

        /* Add an entry for each parameter. */
        for ( int i = 0; i < params.length; i++ ) {
            ubuf.append( '\n' )
                .append( padding );
            Parameter param = params[ i ];
            boolean optional = param.isNullPermitted()
                            || param.getDefault() != null;
            ubuf.append( optional ? "[" : "" )
                .append( param.getName() )
                .append( '=' )
                .append( param.getUsage() )
                .append( optional ? "]" : "" );
        }

        /* Return the completed string. */
        return ubuf.toString();
    }
}
