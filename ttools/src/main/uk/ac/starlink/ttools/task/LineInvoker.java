package uk.ac.starlink.ttools.task;

import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.InvokeUtils;
import uk.ac.starlink.task.LineEnvironment;
import uk.ac.starlink.task.LineFormatter;
import uk.ac.starlink.task.LineWord;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.plottask.PlotStateFactory;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;
import uk.ac.starlink.util.gui.MemoryMonitor;

/**
 * Invokes the Stilts tasks using a {@link LineTableEnvironment}.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class LineInvoker {

    private final String toolName_;
    private final ObjectFactory taskFactory_;
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.ttools" );

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
    public int invoke( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );
        LineTableEnvironment env = new LineTableEnvironment();
        int verbosity = 0;
        boolean bench = false;
        boolean memgui = false;
        PrintStream out = System.out;
        PrintStream err = System.err;

        /* Treat flags. */
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) || arg.startsWith( "+" ) ) {
                if ( arg.equals( "-help" ) ||
                     arg.equals( "-h" ) ) {
                    it.remove();
                    String topic = it.hasNext() ? (String) it.next() : null;
                    out.println( "\n" + getUsage( topic ) );
                    return 0;
                }
                else if ( arg.equals( "-version" ) ) {
                    it.remove();
                    String[] lines = new String[] {
                        "",
                        "This is STILTS, the STIL Tool Set",
                        "",
                        "STILTS version " + Stilts.getVersion(),
                        "STIL version "
                        + IOUtils.getResourceContents( StarTable.class,
                                                       "stil.version" ),
                        "Starjava revision: "
                        + IOUtils.getResourceContents( Stilts.class,
                                                       "revision-string" ),
                        "JVM: " + InvokeUtils.getJavaVM(),
                        "",
                        "Author: Mark Taylor",
                        "WWW: http://www.starlink.ac.uk/stilts/",
                        "",
                    };
                    for ( int il = 0; il < lines.length; il++ ) {
                        String line = lines[ il ];
                        if ( line.length() > 0 ) {
                            out.print( "    " );
                        }
                        out.println( line );
                    }
                    return 0;
                }
                else if ( arg.equals( "-verbose" ) ) {
                    it.remove();
                    verbosity++;
                }
                else if ( arg.equals( "+verbose" ) ) {
                    it.remove();
                    verbosity--;
                }
                else if ( arg.equals( "-memory" ) ) {
                    it.remove();
                    StoragePolicy.setDefaultPolicy( StoragePolicy
                                                   .PREFER_MEMORY );
                    env.getTableFactory()
                       .setStoragePolicy( StoragePolicy.PREFER_MEMORY );
                }
                else if ( arg.equals( "-disk" ) ) {
                    it.remove();
                    StoragePolicy.setDefaultPolicy( StoragePolicy.PREFER_DISK );
                    env.getTableFactory()
                       .setStoragePolicy( StoragePolicy.PREFER_DISK );
                }
                else if ( arg.equals( "-votstrict" ) ) {
                    it.remove();
                    env.setStrictVotable( true );
                }
                else if ( arg.equals( "-novotstrict" ) ) {
                    it.remove();
                    env.setStrictVotable( false );
                }
                else if ( arg.equals( "-batch" ) ) {
                    it.remove();
                    env.setInteractive( false );
                }
                else if ( arg.equals( "-prompt" ) ) {
                    it.remove();
                    env.setPromptAll( true );
                }
                else if ( arg.equals( "-debug" ) ) {
                    it.remove();
                    env.setDebug( true );
                }
                else if ( arg.equals( "-bench" ) ) {
                    it.remove();
                    bench = true;
                }
                else if ( arg.equals( "-memgui" ) ) {
                    it.remove();
                    memgui = true;
                }
                else if ( arg.equals( "-checkversion" ) && it.hasNext() ) {
                    it.remove();
                    String vers = (String) it.next();
                    it.remove();
                    if ( ! vers.equals( Stilts.getVersion() ) ) {
                        err.println( "Version mismatch: "
                                   + Stilts.getVersion() + " != " + vers );
                        return 1;
                    }
                }
                else if ( arg.equals( "-stdout" ) && it.hasNext() ) {
                    it.remove();
                    String outName = (String) it.next();
                    it.remove();
                    if ( outName == null || outName.trim().length() == 0 ||
                         "-".equals( outName ) ) {
                        out = System.out;
                    }
                    else {
                        try {
                            out = new PrintStream(
                                      new FileOutputStream( outName ) );
                        }
                        catch ( IOException e ) {
                            if ( env.isDebug() ) {
                                e.printStackTrace( err );
                            }
                            else {
                                String msg = e.getMessage();
                                if ( msg == null ) {
                                    msg = e.toString();
                                }
                                err.println( "\n" + msg + "\n" );
                            }
                            return 1;
                        }
                    }
                }
                else if ( arg.equals( "-stderr" ) && it.hasNext() ) {
                    it.remove();
                    String errName = (String) it.next();
                    it.remove();
                    if ( errName == null || errName.trim().length() == 0 ||
                         "=".equals( errName ) ) {
                        err = System.err;
                    }
                    else {
                        try {
                            err = new PrintStream(
                                      new FileOutputStream( errName ) );
                        }
                        catch ( IOException e ) {
                            if ( env.isDebug() ) {
                                e.printStackTrace( err );
                            }
                            else {
                                String msg = e.getMessage();
                                if ( msg == null ) {
                                    msg = e.toString();
                                }
                                err.println( "\n" + msg + "\n" );
                            }
                            return 1;
                        }
                    }
                }
                else {
                    it.remove();
                    err.println( "\n" + getUsage() );
                    return 1;
                }
            }
            else {
                break;
            }
        }

        if ( argList.size() == 0 ) {
            err.println( "\n" + getUsage() );
            return 1;
        }

        env.setOutputStream( out );
        env.setErrorStream( err );

        InvokeUtils.configureLogging( verbosity, env.isDebug() );

        String taskName = (String) argList.remove( 0 );
        if ( taskFactory_.isRegistered( taskName ) ) {
            Task task = null;
            try {
                task = (Task) taskFactory_.createObject( taskName );
                String[] taskArgs = (String[])
                                    argList.toArray( new String[ 0 ] );
                String helpText = helpMessage( env, task, taskName, taskArgs );
                if ( helpText != null ) {
                    out.println( "\n" + helpText );
                    return 0;
                }
                else {
                    LineWord[] words = new LineWord[ taskArgs.length ];
                    for ( int i = 0; i < taskArgs.length; i++ ) {
                        words[ i ] = new LineWord( taskArgs[ i ] );
                    }
                    env.setWords( words );
                    long start = System.currentTimeMillis();
                    Executable exec = task.createExecutable( env );
                    String[] unused = env.getUnused();
                    if ( unused.length == 0 ) {
                        logParameterValues( taskName, env );
                        JFrame monwin = memgui ? startMemoryMonitor( "STILTS" )
                                               : null;
                        exec.execute();
                        if ( monwin != null ) {
                            monwin.dispose();
                        }
                        if ( bench ) {
                            long millis = System.currentTimeMillis() - start;
                            String secs =
                                Float.toString( ( millis / 100L ) * 0.1f );
                            err.println( "Elapsed time: " + secs + "s" );
                        }
                        return 0;
                    }
                    else {
                        err.println( "\n" + getUnusedWarning( unused ) );
                        err.println( getTaskUsage( task, taskName ) );
                        return 1;
                    }
                }
            }
            catch ( TaskException e ) {
                reportError( env, e );
                if ( e instanceof ParameterValueException && task != null ) {
                    Parameter param =
                        ((ParameterValueException) e).getParameter();
                    try {
                        err.println( "Value was: " + param.getName() + "=\""
                                   + param.stringValue( env ) + "\"" );
                    }
                    catch ( TaskException e2 ) {
                        // never mind
                    }
                    err.println( "Usage: " + param.getName() + "="
                               + param.getUsage() );
                    err.println();
                }
                else if ( e instanceof UsageException && task != null ) {
                    err.println( getTaskUsage( task, taskName ) );
                }
                return 1;
            }
            catch ( IllegalArgumentException e ) {
                reportError( env, e );
                return 1;
            }
            catch ( RuntimeException e ) {
                e.printStackTrace();
                return 1;
            }
            catch ( IOException e ) {
                reportError( env, e );
                return 1;
            }
            catch ( LoadException e ) {
                err.println( "Task " + taskName + " not available" );
                if ( e.getMessage() != null ) {
                    err.println( e.getMessage() + "\n" );
                }
                if ( env.isDebug() ) {
                    e.printStackTrace( err );
                }
                return 1;
            }
            catch ( OutOfMemoryError e ) {
                err.println( "\nOut of memory" );
                if ( e.getMessage() != null ) {
                    err.println( e.getMessage() );
                }
                if ( env.getTableFactory().getStoragePolicy() 
                     != StoragePolicy.PREFER_DISK ) {
                    err.println( "Try \"-disk\" flag?\n" );
                }
                else {
                    err.println( "Try increasing heap memory"
                               + " (-Xmx flag)\n" );
                }
                if ( env.isDebug() ) {
                    e.printStackTrace( err );
                }
                return 1;
            }
            catch ( NoClassDefFoundError e ) {
                reportError( env, e );
                if ( env.isDebug() ) {
                    e.printStackTrace( err );
                }
                try {
                    String msg = new StringBuffer()
                        .append( "\n" )
                        .append( e )
                        .append( "\n" )
                        .append( "The runtime Java Runtime Environment (JRE) " )
                        .append( "is missing some compile-time classes.\n" )
                        .append( "The most likely reason is that you are " )
                        .append( "using an incomplete java such as GNU gcj.\n" )
                        .append( "The JVM you are using is " )
                        .append( System.getProperty( "java.vm.name",
                                                     "unknown" ) )
                        .append( " version " )
                        .append( System.getProperty( "java.vm.version", "?" ) )
                        .append( ".\n" )
                        .append( "The recommended JRE is Sun's J2SE " )
                        .append( "version 1.5 or greater.\n" )
                        .toString();
                    err.println( msg );
                }
                catch ( Throwable e1 ) {
                    e1.printStackTrace( err );
                }
                return 1;
            }
            finally {
                out.flush();
                err.flush();
            }
        }
        else {
            err.println( "\nNo such task: " + taskName );
            err.println( "\n" + getUsage() );
            err.flush();
            return 1;
        }
    }

    /**
     * Maybe writes some indication of the parameter values through the 
     * logging system.
     *
     * @param  taskName  task name
     * @param  env  execution environment
     */
    private void logParameterValues( String taskName,
                                     LineTableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer( taskName );
        String[] words = env.getAssignments();
        for ( int i = 0; i < words.length; i++ ) {
            sbuf.append( ' ' )
                .append( words[ i ] );
        }
        logger_.info( sbuf.toString() );
    }

    /**
     * If a command line represents a request for help, appropriate help
     * text is returned.  Otherwise, null is returned.
     *
     * @param   task   task
     * @param   taskName  task nickname
     * @param   taskArgs  argument list for task (not including task name)
     * @return  help text, or null
     */
    private String helpMessage( LineTableEnvironment env, Task task,
                                String taskName, String[] taskArgs ) {
        for ( int i = 0; i < taskArgs.length; i++ ) {
            String arg = taskArgs[ i ];
            String helpFor = null;
            if ( arg.equals( "-help" ) ||
                 arg.equals( "-h" ) ||
                 arg.equalsIgnoreCase( "help" ) ) {
                return getTaskUsage( task, taskName );
            }
            else if ( arg.toLowerCase().startsWith( "-help=" ) ) {
                helpFor = arg.substring( 6 ).trim().toLowerCase();
            }
            else if ( arg.toLowerCase().startsWith( "help=" ) ) {
                helpFor = arg.substring( 5 ).trim().toLowerCase();
            }
            else if ( arg.length() > 2 && arg.endsWith( "=?" ) ) {
                helpFor = arg.substring( 0, arg.length() - 2 );
            }
            if ( "*".equals( helpFor ) ) {
                Parameter [] params = task.getParameters();
                StringBuffer sbuf = new StringBuffer();
                for ( int j = 0; j < params.length; j++ ) {
                    sbuf.append( getParamHelp( env, taskName, params[ j ] ) );
                    sbuf.append( '\n' );
                }
                return sbuf.toString();
            }
            else if ( helpFor != null ) {

                /* Look for an exact (case-insensitive) match of the requested
                 * name with one of the task's parameters. */
                Parameter[] params = task.getParameters();
                for ( int j = 0; j < params.length; j++ ) {
                    Parameter param = params[ j ];
                    if ( env.paramNameMatches( helpFor, param ) ) {
                        return getParamHelp( env, taskName, param );
                    }
                }

                /* If that fails, treat the special case of plotting parameters
                 * which have per-table or per-subset suffixes.
                 * This allows you to get help for parameter xdata without
                 * giving the symbolic suffix (xdataN). */
                String stripper = new StringBuffer()
                    .append( '(' )
                    .append( PlotStateFactory.TABLE_VARIABLE )
                    .append( '|' )
                    .append( PlotStateFactory.SUBSET_VARIABLE )
                    .append( '|' )
                    .append( PlotStateFactory.AUX_VARIABLE )
                    .append( ')' )
                    .append( '*' )
                    .append( '$' )
                    .toString();
                for ( int j = 0; j < params.length; j++ ) {
                    Parameter param = params[ j ];
                    String pname = param.getName().replaceFirst( stripper, "" );
                    if ( LineTableEnvironment.normaliseName( helpFor )
                        .equals( LineTableEnvironment
                                .normaliseName( pname ) ) ) {
                        return getParamHelp( env, taskName, param );
                    }
                }
                return "No help for parameter: " + helpFor + "\n\n" 
                     + getTaskUsage( task, taskName );
            }
        }
        return null;
    }

    /**
     * Returns a usage string for a given topic, or for this invoker if
     * the topic is unknown.
     *
     * @param  topic  help topic, such as a task name
     */
    private String getUsage( String topic ) {
        if ( topic != null ) {
            try {
                Task task = (Task) taskFactory_.createObject( topic );
                return getTaskUsage( task, topic );
            }
            catch ( LoadException e ) {
            }
        }
        return getUsage();
    }

    /**
     * Outputs a description of a throwable to the environment.
     *
     * @param   env  execution environment
     * @param   e    error
     */
    private void reportError( TableEnvironment env, Throwable e ) {
        if ( env.isDebug() ) {
            e.printStackTrace( env.getErrorStream() );
        }
        else {
            List<String> msgList = new ArrayList<String>();
            for ( ; e != null; e = e.getCause() ) {
                String msg = e.getMessage();
                if ( msg == null ) {
                    msg = e.toString();
                }
                int nm = msgList.size();
                if ( nm == 0 || ! msg.equals( msgList.get( nm - 1 ) ) ) {
                    msgList.add( msg );
                }
            }
            String[] msgs = msgList.toArray( new String[ 0 ] );
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "Error: " );
            for ( int im = 0; im < msgs.length; im++ ) {
                if ( im > 0 ) {
                    sbuf.append( '\n' );
                    for ( int j = 0; j < im; j++ ) {
                        sbuf.append( "    " );
                    }
                    sbuf.append( '(' );
                }
                sbuf.append( msgs[ im ] );
            }
            for ( int j = 1; j < msgs.length; j++ ) {
                sbuf.append( ')' );
            }
            env.getErrorStream().println( sbuf.toString() );
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
            .append( "Usage:\n" );
        String pad1 = "   ";
        String pad = ( pad1 + toolName_ ).replaceAll( ".", " " );

        sbuf.append( pad1 )
            .append( toolName_ )
            .append( " [-help]" )
            .append( " [-version]" )
            .append( " [-verbose]" )
            .append( " [-memory]" )
            .append( " [-disk]" )
            .append( " [-debug]" )
            .append( '\n' )
            .append( pad )
            .append( " [-prompt]" )
            .append( " [-batch]" )
            .append( " [-bench]" )
            .append( " [-memgui]" )
            .append( " [-checkversion <vers>]" )
            .append( '\n' )
            .append( pad )
            .append( " [-stdout <file>]" )
            .append( " [-stderr <file>]" )
            .append( '\n' )
            .append( pad )
            .append( " <task-name> <task-args>" )
            .append( '\n' )
            .append( '\n' );

        sbuf.append( pad1 )
            .append( toolName_ )
            .append( " <task-name> help[=<param-name>|*]" )
            .append( '\n' )
            .append( '\n' );

        sbuf.append( "   Known tasks:\n" );
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
     * @return   usage string
     */
    private static String getTaskUsage( Task task, String taskName ) {
        String prefix = "Usage: " + taskName;
        StringBuffer usage = new StringBuffer();
        usage.append( getPrefixedTaskUsage( task, prefix ) );
        String pad = prefix.replaceAll( ".", " " );
     // usage.append( pad )
     //      .append( " [help=<arg-name>]" )
     //      .append( '\n' );
        return usage.toString();
    }

    /**
     * Returns a usage string for a task, prefixed by a given string.
     *
     * @param   task   task object
     * @param   prefix   string to prepend to the first line
     * @return   usage string
     */
    public static String getPrefixedTaskUsage( Task task, String prefix ) {

        /* Assemble two lists of usage elements: one for parameters 
         * which must be specified by name, and another for parameters
         * which can be specified only by position. */
        Parameter[] params = task.getParameters();
        List namedWords = new ArrayList();
        List numberedWords = new ArrayList();
        int iPos = 0;
        for ( int i = 0; i < params.length; i++ ) {
            StringBuffer word = new StringBuffer();
            word.append( ' ' );
            Parameter param = params[ i ];
            int pos = param.getPosition();
            boolean byPos = false;
            if ( param.getPosition() > 0 ) {
                if ( pos == ++iPos ) {
                    byPos = true;
                }
                else {
                    logger_.warning( "Parameter positions out of sync for " +
                                     param );
                }
            }
            if ( byPos ) {
                word.append( '[' );
            }
            word.append( param.getName() )
                .append( '=' );
            if ( byPos ) {
                word.append( ']' );
            }
            word.append( param.getUsage() );
            (byPos ? numberedWords : namedWords).add( word.toString() );
        }

        /* Start the usage string, noting the amount of padding required
         * at the head to accommodate the prefix on lines after the first. */
        StringBuffer usage = new StringBuffer();
        StringBuffer line = new StringBuffer();
        line.append( prefix );
        String pad = line.toString().replaceAll( ".", " " );

        /* Add the named usage elements. */
        if ( namedWords.size() > 0 ) {
            for ( Iterator it = namedWords.iterator(); it.hasNext(); ) {
                String word = (String) it.next();
                if ( line.length() + word.length() > 78 ) {
                    usage.append( line )
                         .append( '\n' );
                    line = new StringBuffer( pad );
                }
                line.append( word );
            }
            usage.append( line )
                 .append( '\n' );
            line = new StringBuffer( pad );
        }

        /* Add the numbered usage elements. */
        if ( numberedWords.size() > 0 ) {
            for ( Iterator it = numberedWords.iterator(); it.hasNext(); ) {
                String word = (String) it.next();
                if ( line.length() + word.length() > 78 ) {
                    usage.append( line )
                         .append( '\n' );
                    line = new StringBuffer( pad );
                }
                line.append( word );
            }
            usage.append( line )
                 .append( '\n' );
            line = new StringBuffer( pad );
        }

        /* If there are unappended words in line, append them now.
         * This will probably only happen if there were no normal parameters. */
        if ( line.toString().trim().length() > 0 ) {
            usage.append( line )
                 .append( '\n' );
        }

        /* Return the final usage string. */
        return usage.toString();
    }

    /**
     * Returns a help string for a parameter of one of the tasks known
     * to this application.  May include extended usage information.
     * Consider the result to be a formatted string, that is, one which
     * contains newlines to keep line lengths down to a reasonable level.
     *
     * @param  env  execution environment
     * @param  taskName  task nickname - may be null if heading is not required
     * @param  param   parameter for which usage information is required
     * @return   usage message
     */
    public static String getParamHelp( TableEnvironment env, String taskName,
                                       Parameter param ) {
        boolean byPos = param.getPosition() > 0;
        boolean isOptional = param.getDefault() != null 
                          || param.isNullPermitted();
        StringBuffer sbuf = new StringBuffer();
        if ( taskName != null ) {
            sbuf.append( "Help for parameter " )
                .append( param.getName().toUpperCase() )
                .append( " in task " )
                .append( taskName.toUpperCase() )
                .append( '\n' )
                .append( sbuf.toString().replaceAll( ".", "-" ) );
        }
        sbuf.append( "\n   Name:\n" )
            .append( "      " )
            .append( param.getName() )
            .append( "\n\n   Usage:\n" )
            .append( "      " )
            .append( isOptional ? "[" : "" )
            .append( byPos ? "[" : "" )
            .append( param.getName() )
            .append( '=' )
            .append( byPos ? "]" : "" )
            .append( param.getUsage() )
            .append( isOptional ? "]" : "" )
            .append( "\n\n   Summary:\n" )
            .append( "      " )
            .append( param.getPrompt() )
            .append( "\n\n   Description:\n" );
        try {
            sbuf.append( new Formatter()
                        .formatXML( param.getDescription(), 6 ) );
        }
        catch ( SAXException e ) {
            sbuf.append( "      ???" );
        }
        if ( param.getDefault() != null ||
             param.isNullPermitted() ) {
            sbuf.append( "\n\n   Default:\n" )
                .append( "      " )
                .append( param.getDefault() );
        }
        if ( param instanceof ExtraParameter ) {
            sbuf.append( "\n\n" )
                .append( ((ExtraParameter) param).getExtraUsage( env ) );
        }   
        sbuf.append( '\n' );
        return sbuf.toString();
    }

    /**
     * Returns a warning string appropriate for the case when one or more
     * words on the command line are never used by a task.
     *
     * @param  unused   unused words
     * @return  warning lines
     */
    private static String getUnusedWarning( String[] unused ) {
        StringBuffer sbuf = new StringBuffer( "Unused arguments:" );
        for ( int i = 0; i < unused.length; i++ ) {
            sbuf.append( ' ' )
                .append( unused[ i ] );
        }
        sbuf.append( '\n' );
        return sbuf.toString();
    }

    /**
     * Displays and returns a Swing Frame that provides continuous display
     * of memory usage.
     *
     * @param  title  window title
     */
    private static JFrame startMemoryMonitor( String title ) {
        JFrame frame = new JFrame( title );
        MemoryMonitor memmon = new MemoryMonitor();
        memmon.setPreferredSize( new Dimension( 200, 24 ) );
        frame.getContentPane().add( memmon );
        frame.pack();
        frame.setVisible( true );
        return frame;
    }
}
