package uk.ac.starlink.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Invokes Task objects in a way suitable for use from the <code>main</code> 
 * method, using a command line interface.
 * The Task invoke methods are invoked and may throw any exception; 
 * {@link UsageException} and {@link ExecutionException} will be treated
 * specially (a sanitised error message without stacktrace will be 
 * presented to the user).
 *
 * @author   Mark Taylor (Starlink)
 */
public class TerminalInvoker {

    private String toolname;
    private Map<String,Task> taskmap;

    /**
     * Creates a new invoker based on the given class with a given name.
     * This should generally be invoked from a main method - in the
     * event of an error it will call <code>System.exit</code> rather than
     * returning.
     *
     * @param   toolname   the name of this tool, used for user messages
     * @param   taskmap    map of task names to <code>Task</code> objects
     */
    public TerminalInvoker( String toolname, Map<String,Task> taskmap ) {
        this.toolname = toolname;
        this.taskmap = taskmap;
    }

    /**
     * Invokes a method from this TerminalInvoker's class.
     * The first element of the <code>args</code> identifies the method to be
     * called - it is a case-insensitive version of the name of a method
     * in the class.  Subsequent elements are passed 
     * (as an <code>args.length-1</code> element array of Strings) 
     * to the method in question.
     * In the event of any trouble (e.g. unknown task, task method throws
     * an exception) a message is printed to standard error and 
     * the JVM exits.
     *
     * @param  args  the arguments identifying what method to call and what
     *               arguments to pass to it
     */
    public void invoke( String[] args ) throws Exception {
        String usage = "Usage: " + toolname
                                 + " [-help]"
                                 + " [-fulltrace] " 
                                 + " [-bench ntimes] "
                                 + " taskname [args]";
        if ( args.length < 1 || args[ 0 ].length() == 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        String taskname = args[ 0 ].toLowerCase();
        boolean fulltrace = false;
 
        /* Benchmark flag. */
        if ( taskname.equals( "-bench" ) ) {
            int ntimes = Integer.parseInt( args[ 1 ] );
            String[] benchargs = new String[ args.length - 2 ];
            System.arraycopy( args, 2, benchargs, 0, args.length - 2 );
            long start = System.currentTimeMillis();
            for ( int i = 0; i < ntimes; i++ ) {
                invoke( benchargs );
            }
            long finish = System.currentTimeMillis();
            System.out.println( "\n    Time for " + ntimes + " iterations: " +
                                ( finish - start ) + " ms\n" );
            return;
        }

        /* Modify the way that stack traces are displayed. */
        if ( taskname.equals( "-fulltrace" ) ) {
            fulltrace = true;

            /* Rejig the argument list so it doesn't contain the '-fulltrace'
             * and proceed. */
            String[] targs = new String[ args.length - 1 ];
            System.arraycopy( args, 1, targs, 0, args.length - 1 );
            args = targs;
            taskname = args[ 0 ].toLowerCase();
        }

        /* Unknown flag - treat as a request for a usage message. */
        if ( taskname.startsWith( "-" ) ) {
            System.out.println( usage );
            System.out.println( "Known tasks:" );
            Map<String,Task> tmap = new TreeMap<String,Task>( taskmap );
            for ( Map.Entry<String,Task> entry : tmap.entrySet() ) {
                System.out.println( "   " + entry.getKey() + " " +
                                    getTaskUsage( entry.getValue() ) );
 
            }
            System.out.println();
            return;
        }

        /* If we know of a task with this name, try to invoke it. */
        if ( taskmap.containsKey( taskname ) ) {

            /* Get the task object which defines what is to be done. */
            Task task = taskmap.get( taskname );

            /* Prepare the command line which may contain its parameter 
             * values. */
            String[] taskargs = new String[ args.length - 1 ];
            System.arraycopy( args, 1, taskargs, 0, args.length - 1 );
            try {

                /* Get the parameter objects associated with this task. */
                Parameter<?>[] params = task.getParameters();

                /* Construct an execution environment for this task based
                 * on the command line. */
                Environment env = new TerminalEnvironment( taskargs, params );

                /* The task is now properly configured; invoke it. */
                task.createExecutable( env ).execute();
            }

            /* Catch various exceptions and deal with them appropriately. */
            catch ( ParameterValueException e ) {
                Throwable cause = e.getCause();
                if ( cause != null ) {
                    if ( fulltrace ) {
                        cause.printStackTrace( System.err );
                    }
                    else {
                        System.err.println( cause.getMessage() );
                    }
                }
                System.err.println( toolname + " " + taskname
                                  + ": " + e.getMessage() );
            }
            catch ( UsageException e ) {
                System.err.println( "Usage: " + toolname + " " + taskname +
                                    " " + getTaskUsage( task ) );
                System.exit( 1 );
            }
            catch ( ExecutionException e ) {
                Throwable cause = e.getCause();
                if ( cause != null ) {
                    if ( fulltrace ) {
                        cause.printStackTrace( System.err );
                    }
                    else {
                        System.err.println( cause.getMessage() );
                    }
                }
                System.err.println( toolname + " " + taskname + ": " +
                                    e.getMessage() );
                System.exit( e.getErrorCode() );
            }
            catch ( IOException e ) {
                Throwable cause = e.getCause();
                if ( cause != null ) {
                    if ( fulltrace ) {
                        cause.printStackTrace( System.err );
                    }
                    else {
                        System.err.println( cause.getMessage() );
                    }
                }
                System.err.println( toolname + " " + taskname + ": " +
                                    e.getMessage() );
                System.exit( 1 );
            }
            catch ( AbortException e ) {
                System.err.println( toolname + " " + taskname 
                                  + ": User abort" );
                System.exit( 1 );
            }
        }
        else {
            System.err.println( toolname + ": Unknown task.  Use -h for list" );
        }
    }

    /**
     * Returns a usage string for a given task.  This contains only a
     * symbolic representation of the parameter names, not the task name
     * itself.
     * 
     * @param   task  task
     * @return  usage string
     */
    public static String getTaskUsage( Task task ) {

        /* Assemble a list of parameters in the right order, that is all
         * the numbered ones first and all the unnumbered ones (in the
         * order they were submitted) following. */
        List<Parameter<?>> numbered = new ArrayList<Parameter<?>>();
        List<Parameter<?>> unNumbered = new ArrayList<Parameter<?>>();
        for ( Parameter<?> param : task.getParameters() ) {
            ( param.getPosition() > 0 ? numbered : unNumbered ).add( param );
        }
        Collections.sort( numbered, new Comparator<Parameter<?>>() {
            public int compare( Parameter<?> p1, Parameter<?> p2 ) {
                int pos1 = p1.getPosition();
                int pos2 = p2.getPosition();
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
        List<Parameter<?>> paramList = numbered;
        paramList.addAll( unNumbered );

        /* Assemble the usage message with one element for each parameter. */
        StringBuffer usage = new StringBuffer();
        for ( Iterator<Parameter<?>> it = paramList.iterator();
              it.hasNext(); ) {
            Parameter<?> param = it.next();
            boolean optional = param.isNullPermitted()
                            || param.getStringDefault() != null;
            if ( optional ) {
                usage.append( '[' );
            }
            usage.append( param.getName() );
            if ( optional ) {
                usage.append( ']' );
            }
            if ( it.hasNext() ) {
                usage.append( ' ' );
            }
        }
        return usage.toString();
    }

}
