package uk.ac.starlink.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Invokes Task objects in a way suitable for use from the <tt>main</tt> 
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
    private Map taskmap;

    /**
     * Creates a new invoker based on the given class with a given name.
     * This should generally be invoked from a main method - in the
     * event of an error it will call <tt>System.exit</tt> rather than
     * returning.
     *
     * @param   toolname   the name of this tool, used for user messages
     * @param   taskmap    map of task names to <tt>Task</tt> objects
     */
    public TerminalInvoker( String toolname, Map taskmap ) {
        this.toolname = toolname;
        this.taskmap = taskmap;
    }

    /**
     * Invokes a method from this TerminalInvoker's class.
     * The first element of the <tt>args</tt> identifies the method to be
     * called - it is a case-insensitive version of the name of a method
     * in the class.  Subsequent elements are passed 
     * (as an <tt>args.length-1</tt> element array of Strings) 
     * to the method in question.
     * In the event of any trouble (e.g. unknown task, task method throws
     * an exception) a message is printed to standard error and 
     * the JVM exits.
     *
     * @param  the arguments identifying what method to call and what
     *         arguments to pass to it
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
            Map tmap = new TreeMap( taskmap );
            for ( Iterator it = tmap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                System.out.println( "   " + (String) entry.getKey() + " " +
                                    ((Task) entry.getValue()).getUsage() );
            }
            System.out.println();
            return;
        }

        /* If we know of a task with this name, try to invoke it. */
        if ( taskmap.containsKey( taskname ) ) {

            /* Get the task object which defines what is to be done. */
            Task task = (Task) taskmap.get( taskname );

            /* Prepare the command line which may contain its parameter 
             * values. */
            String[] taskargs = new String[ args.length - 1 ];
            System.arraycopy( args, 1, taskargs, 0, args.length - 1 );
            try {

                /* Get the parameter objects associated with this task. */
                Parameter[] params = task.getParameters();

                /* Construct an execution environment for this task based
                 * on the command line. */
                Environment env = new TerminalEnvironment( taskargs, params );

                /* Configure each parameter by telling it what environment
                 * will provide its values. */
                for ( int i = 0; i < params.length; i++ ) {
                    params[ i ].setEnvironment( env );
                }

                /* The task is now properly configured; invoke it. */
                task.invoke( env );
            }

            /* Catch various exceptions and deal with them appropriately. */
            catch ( UsageException e ) {
                String tusage = e.getUsage();
                if ( tusage == null ) {
                    tusage = task.getUsage();
                }
                System.err.println( "Usage: " + toolname + " " + taskname +
                                    " " + tusage );
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
            catch ( AbortException e ) {
                System.err.println( toolname + " " + taskname 
                                  + ": User abort" );
                System.exit( 1 );
            }
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
        }
        else {
            System.err.println( toolname + ": Unknown task.  Use -h for list" );
        }
    }

}
