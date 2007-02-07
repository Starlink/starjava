package uk.ac.starlink.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Invokes tasks from a command line when the tasks are available from 
 * an {@link uk.ac.starlink.util.ObjectFactory}.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2007
 */
public class MultiTaskInvoker {

    private final String toolName_;
    private final ObjectFactory taskFactory_;
    private String versionMessage_;

    /**
     * Constructor.
     *
     * @param  toolName  user-known name for the tool
     * @param  taskFactory  factory which produces {@link Task} objects
     */
    public MultiTaskInvoker( String toolName, ObjectFactory taskFactory ) {
        toolName_ = toolName;
        taskFactory_ = taskFactory;
    }

    /**
     * Invokes one of the tasks known by this invoker given a command line.
     * The commmand line is an array of words of the form
     * <pre>
     *    [&lt;flags&gt;] &lt;taskname&gt; [&lt;task-args&gt;]
     * </pre>
     *
     * @param  args  command line words
     */
    public int invoke( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );

        /* Process flags. */
        boolean debug = false;
        boolean bench = false;
        int verbose = 0;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.charAt( 0 ) != '-' ) {
                break;
            }
            else if ( "-help".equals( arg ) ) {
                it.remove();
                System.out.println( getUsage() );
                return 0;
            }
            else if ( "-debug".equals( arg ) ) {
                it.remove();
                debug = true;
            }
            else if ( "-verbose".equals( arg ) ) {
                it.remove();
                verbose++;
            }
            else if ( "-version".equals( arg ) &&
                      getVersionMessage() != null ) {
                it.remove();
                System.out.println( getVersionMessage() );
                return 0;
            }
            else if ( "-bench".equals( arg ) ) {
                it.remove();
                bench = true;
            }
            else if ( arg.charAt( 0 ) == '-' ) {
                it.remove();
                System.err.println( getUsage() );
                return 1;
            }
            else {
                break;
            }
        }
        InvokeUtils.configureLogging( verbose, debug );

        /* Find the task. */
        if ( argList.size() == 0 ) {
            System.err.println( getUsage() );
            return 1;
        }
        String taskName = (String) argList.remove( 0 );
        Task task;
        try {
            task = (Task) taskFactory_.createObject( taskName );
        }
        catch ( LoadException e ) {
            System.err.println( "No such task " + taskName );
            System.err.println( getUsage() );
            return 1;
        }

        /* Report task-specific help if required. */
        if ( argList.size() > 0 ) {
            String arg1 = ((String) argList.get( 0 )).toLowerCase();
            if ( arg1.startsWith( "-help" ) ||
                 arg1.equals( "help" ) ) {
                System.out.println( getUsage( taskName, task ) );
                return 0;
            }
        }

        /* Execute the task. */
        String[] taskArgs = (String[]) argList.toArray( new String[ 0 ] );
        try {
            Environment env =
                new LineEnvironment( taskArgs, task.getParameters() );
            long start = System.currentTimeMillis();
            Executable exec = task.createExecutable( env );
            exec.execute();
            if ( bench ) {
                long millis = System.currentTimeMillis() - start;
                String secs =
                    Float.toString( ( millis / 100L ) * 0.1f );
                System.err.println( "Elapsed time: " + secs + "s" );
            }
            return 0;
        }
        catch ( Exception e ) {
            if ( debug ) {
                System.err.println();
                e.printStackTrace( System.err );
            }
            System.err.println();
            InvokeUtils.summariseError( e, System.err );
            if ( e instanceof ParameterValueException ||
                 e instanceof IOException ) {
            }
            else if ( e instanceof UsageException ) {
                System.err.println();
                System.err.println( getUsage( taskName, task ) );
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
    }

    /**
     * Sets the message which will be reported if the "-version" flag is
     * given.
     *
     * @param   msg   formatted version message
     */
    public void setVersionMessage( String msg ) {
        versionMessage_ = msg;
    }

    /**
     * Returns the message to be reported if the "-version" flag is
     * given.
     *
     * @return  formatted version message
     */
    public String getVersionMessage() {
        return versionMessage_;
    }

    /**
     * Returns a usage message for a given task.
     *
     * @param   taskName  task nickname
     * @param   task  task object
     * @return  formatted usage message
     */
    private String getUsage( String taskName, Task task ) {
        StringBuffer ubuf = new StringBuffer();
        ubuf.append( "Usage: " )
            .append( toolName_ )
            .append( ' ' )
            .append( taskName );
        String padding = ubuf.toString().replaceAll( ".", " " ) + " ";
        ubuf.append( "\n" );
        Parameter[] params = InvokeUtils.sortParameters( task.getParameters() );
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            ubuf.append( padding );
            boolean optional = param.isNullPermitted()
                            || param.getDefault() != null;
            ubuf.append( optional ? "[" : "" )
                .append( param.getName() )
                .append( '=' )
                .append( param.getUsage() )
                .append( optional ? "]" : "" )
                .append( "\n" );
        }
        return "\n" + ubuf.toString();
    }

    /**
     * Returns a usage message for this tool.
     *
     * @return  formatted usage message
     */
    private String getUsage() {
        StringBuffer sbuf = new StringBuffer()
            .append( "Usage:\n" );
        String pad1 = "   ";
        String pad = ( pad1 + toolName_ ).replaceAll( ".", " " );

        sbuf.append( pad1 )
            .append( toolName_ )
            .append( " [-help]" );
        if ( getVersionMessage() != null ) {
            sbuf.append( " [-version]" );
        }
        sbuf.append( " [-verbose]" )
            .append( " [-debug]" )
            .append( " [-bench]" )
            .append( '\n' )
            .append( pad )
            .append( " <task-name> <task-args>" )
            .append( '\n' )
            .append( '\n' );
        sbuf.append( pad1 )
            .append( toolName_ )
            .append( " <task-name> -help" )
            .append( "\n" )
            .append( "\n" );
        sbuf.append( "   Known tasks:\n" );
        String[] tasks = taskFactory_.getNickNames();
        for ( int i = 0; i < tasks.length; i++ ) {
            sbuf.append( "      " )
                .append( tasks[ i ] )
                .append( "\n" );
        }
        return "\n" + sbuf.toString();
    }
}
