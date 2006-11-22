package uk.ac.starlink.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
                new TerminalEnvironment( taskArgs, task_.getParameters() );
            task_.createExecutable( env ).execute();
        }

        /* Catch various exceptions and deal with them appropriately. */
        catch ( RuntimeException e ) {
            throw e;
        }
        catch ( Exception e ) {
            if ( debug ) {
                System.err.println();
                e.printStackTrace( System.err );
            }
            System.err.println();

            if ( e instanceof ParameterValueException ) {
                System.err.println( e.getMessage() );
            }
            else if ( e instanceof UsageException ) {
                System.err.println( getUsage() );
            }
            else if ( e instanceof AbortException ) {
                System.err.println( "User abort" );
            }
            else {
                System.err.println( e );
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
        Parameter[] params = task_.getParameters();
        List numbered = new ArrayList();
        List unNumbered = new ArrayList();
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            ( param.getPosition() > 0 ? numbered : unNumbered ).add( param );
        }
        Collections.sort( numbered, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                int pos1 = ((Parameter) o1).getPosition();
                int pos2 = ((Parameter) o2).getPosition();
                if ( pos1 < pos2 ) {
                    return -1;
                }
                else if ( pos2 < pos1 ) {
                    return +1;
                }
                else {
                    throw new IllegalArgumentException(
                        "Two params have same position" );
                }
             }
        } );
        List paramList = numbered;
        paramList.addAll( unNumbered );

        /* Add an entry for each parameter. */
        for ( Iterator it = paramList.iterator(); it.hasNext(); ) {
            ubuf.append( '\n' )
                .append( padding );
            Parameter param = (Parameter) it.next();
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
