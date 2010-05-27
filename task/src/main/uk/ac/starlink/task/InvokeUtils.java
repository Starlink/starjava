package uk.ac.starlink.task;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides some utility functions used by classes which invoke tasks.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2007
 */
public class InvokeUtils {

    /**
     * Sets up the logging system.
     *
     * @param  verbosity  number of levels greater than default to set
     * @param  debug   whether debugging mode is on
     */
    public static void configureLogging( int verbosity, boolean debug ) {

        /* Try to acquire the root logger. */
        Logger rootLogger = Logger.getLogger( "" );

        /* Work out the logging level to which the requested verbosity
         * corresponds. */
        int verbInt = Math.max( Level.ALL.intValue(),
                                Level.WARNING.intValue()
                                - verbosity *
                                  ( Level.WARNING.intValue() -
                                    Level.INFO.intValue() ) );
        Level verbLevel = Level.parse( Integer.toString( verbInt ) );

        /* Get the root logger's console handler.  By default
         * it has one of these; if it doesn't then some custom
         * logging is in place and we won't mess about with it. */
        Handler[] rootHandlers = rootLogger.getHandlers();
        if ( rootHandlers.length > 0 &&
             rootHandlers[ 0 ] instanceof ConsoleHandler ) {
            rootHandlers[ 0 ].setLevel( verbLevel );
            rootHandlers[ 0 ].setFormatter( new LineFormatter( debug ) );
        }
        rootLogger.setLevel( verbLevel );

        /* Filter out an annoying message that Axis issues. */
        Logger.getLogger( "org.apache.axis.utils.JavaUtils" )
              .setLevel( Level.SEVERE );
    }

    /**
     * Returns the JVM version, without throwing any exceptions.
     *
     * @return   java version
     */
    public static String getJavaVersion() {
        try {
            return System.getProperty( "java.version" );
        }
        catch ( SecurityException e ) {
            return "???";
        }
    }

    /**
     * Returns the JVM name and version string, without throwing any exceptions.
     *
     * @return   JVM description
     */
    public static String getJavaVM() {
        try {
            return System.getProperty( "java.vm.name", "???" )
                 + " version "
                 + System.getProperty( "java.vm.version", "???" );
        }
        catch ( SecurityException e ) {
            return "???";
        }
    }


    /**
     * Writes a summary of a (possibly nested) exception to a given
     * output stream.
     *
     * @param   error  exception
     * @param   out  destination stream
     */
    public static void summariseError( Throwable error, PrintStream out ) {
        String msg = error.getMessage();
        if ( msg == null || msg.trim().length() == 0 ) {
            msg = error.toString();
        }
        else {
            out.println( msg );
        }
        Throwable cause = error.getCause();
        if ( cause != null ) {
            summariseError( cause, out );
        }
    }

    /**
     * Sorts a list of Parameter objects.  Numbered ones are followed by
     * unnumbered ones.
     *
     * @param   params  input list
     * @return  output list
     */
    public static Parameter[] sortParameters( Parameter[] params ) {
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
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }
}
