package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;

/**
 * Invokes the Stilts tasks using a {@link LineEnvironment}.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class LineInvoker {

    private final String toolName_;
    private final ObjectFactory taskFactory_;

    /**
     * Constructor.
     *
     * @param   toolName  name of the overall application
     * @param   taskFactory  factory which can create the tasks known to
     *          the application
     */
    public LineInvoker( String toolName, ObjectFactory taskFactory ) {
        toolName_ = toolName;
        taskFactory_ = taskFactory;
    }

    /**
     * Invokes one of the known tasks given a string of command-line words.
     * The <code>args</code> string will typically come straight out of
     * a static <code>main()</code> method.
     * 
     * @param   args   argument list
     */
    public void invoke( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );
        LineEnvironment env = new LineEnvironment();

        /* Treat flags. */
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) ) {
                if ( arg.equals( "-help" ) ||
                     arg.equals( "-h" ) ) {
                    it.remove();
                    System.out.println( "\n" + getUsage() );
                    System.exit( 0 );
                }
                else if ( arg.equals( "-version" ) ) {
                    it.remove();
                    System.out.println( "\n" + getVersion() );
                    System.exit( 0 );
                }
                else if ( arg.equals( "-debug" ) ) {
                    it.remove();
                    env.setDebug( true );
                }
                else {
                    it.remove();
                    System.err.println( "\n" + getUsage() );
                    System.exit( 1 );
                }
            }
            else {
                break;
            }
        }

        if ( argList.size() == 0 ) {
            System.err.println( "\n" + getUsage() );
            System.exit( 1 );
        }

        String taskName = (String) argList.remove( 0 );
        if ( taskFactory_.isRegistered( taskName ) ) {
            Task task = null;
            try {
                task = (Task) taskFactory_.createObject( taskName );
                String[] taskArgs = (String[])
                                    argList.toArray( new String[ 0 ] );
                if ( taskArgs.length == 0 || "help".equals( taskArgs[ 0 ] ) ) {
                    System.out.println( "\n" + getTaskUsage( task, taskName ) );
                }
                else if ( taskArgs[ 0 ].startsWith( "help=" ) ) {
                    String paramName = taskArgs[ 0 ].substring( 5 );
                    Parameter[] params = task.getParameters();
                    for ( int i = 0; i < params.length; i++ ) {
                        if ( paramName
                            .equalsIgnoreCase( params[ i ].getName() ) ) {
                            System.out.println( getParamUsage( env, task, 
                                                               taskName, 
                                                               params[ i ] ) );
                        }
                    }
                }
                else {
                    env.setArgs( taskArgs );
                    task.invoke( env );
                    String unused = env.getUnused();
                    if ( unused != null ) {
                        System.err.println( "\nWARNING: Unused arguments " 
                                          + unused + "\n" );
                    }
                }
            }
            catch ( TaskException e ) {
                if ( env.isDebug() ) {
                    e.printStackTrace( System.err );
                }
                else {
                    String msg = e.getMessage();
                    if ( msg == null ) {
                        msg = e.toString();
                    }
                    System.err.println( "\n" + msg );
                }
                if ( e instanceof UsageException && task != null ) {
                    System.err.println( getTaskUsage( task, taskName ) );
                }
                System.exit( 1 );
            }
            catch ( LoadException e ) {
                System.err.println( "Task " + taskName + " not available" );
                if ( e.getMessage() != null ) {
                    System.err.println( e.getMessage() );
                }
                if ( env.isDebug() ) {
                    e.printStackTrace( System.err );
                }
            }
        }
        else {
            System.err.println( "\n" + getUsage() );
            System.exit( 1 );
        }
    }

    /**
     * Returns a usage string for this invoker.
     *
     * @return  usage string
     * @see   #invoke
     */
    private String getUsage() {
        StringBuffer sbuf = new StringBuffer()
            .append( "Usage: " )
            .append( toolName_ )
            .append( " [-help]" )
            .append( " [-version]" )
            .append( " [-debug]" )
            .append( " <task-name> <task-args>" )
            .append( '\n' );
        sbuf.append( "\n   Known tasks:\n" );
        String[] tasks = taskFactory_.getNickNames();
        for ( int i = 0; i < tasks.length; i++ ) {
            sbuf.append( "      " )
                .append( tasks[ i ] )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Returns a usage string for a task known to this application.
     *
     * @param   task   task object
     * @param   taskName  task nickname (the one by which it is known to
     *          the user)
     */
    private String getTaskUsage( Task task, String taskName ) {
        StringBuffer usage = new StringBuffer();
        StringBuffer line = new StringBuffer();
        line.append( "Usage: " + taskName );
        String pad = line.toString().replaceAll( ".", " " );
        Parameter[] params = task.getParameters();
        for ( int i = 0; i < params.length; i++ ) {
            StringBuffer word = new StringBuffer();
            word.append( ' ' );
            Parameter param = params[ i ];
            int pos = param.getPosition();
            boolean byPos = pos > 0 && pos == i + 1;
            if ( byPos ) {
                word.append( '[' );
            }
            word.append( param.getName() )
                .append( '=' );
            if ( byPos ) {
                word.append( ']' );
            }
            word.append( param.getUsage() );
            if ( line.length() + word.length() > 75 ) {
                usage.append( line )
                     .append( '\n' );
                line = new StringBuffer( pad );
            }
            line.append( word );
        }
        usage.append( line )
             .append( '\n' );
        return usage.toString();
    }

    /**
     * Returns a usage string for a parameter of one of the tasks known
     * to this application.  May include extended usage information.
     * Consider the result to be a formatted string, that is, one which
     * contains newlines to keep line lengths down to a reasonable level.
     *
     * @param  env  execution environment
     * @param  task   task 
     * @param  taskName  task nickname
     * @param  param   parameter for which usage information is required
     * @return   usage message
     */
    private String getParamUsage( TableEnvironment env, Task task,
                                  String taskName, Parameter param ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "\nParameter information for parameter " )
            .append( param.getName().toUpperCase() )
            .append( " in task " )
            .append( taskName.toUpperCase() )
            .append( sbuf.toString().replaceAll( ".", "-" ) )
            .append( "\n\n   Name:\n" )
            .append( "      " )
            .append( param.getName() )
            .append( "\n\n   Usage:\n" )
            .append( "      " )
            .append( param.getName() )
            .append( '=' )
            .append( param.getUsage() )
            .append( "\n\n   Description:\n" )
            .append( "      " )
            .append( param.getPrompt() )
            .append( "\n\n   Default:\n" )
            .append( "      " )
            .append( param.getDefault() )
            .append( "\n" );
        if ( param instanceof ExtraParameter ) {
            sbuf.append( "\n" )
                .append( ((ExtraParameter) param).getExtraUsage( env ) );
        }   
        return sbuf.toString();
    }

    /**
     * Returns version string for this package.
     *
     * @param  usage string
     */
    public static String getVersion() {
        return "STILTS unknown version";
    }
    
}
