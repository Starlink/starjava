// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.io.IOException;
import uk.ac.starlink.topcat.Executor;

/**
 * Functions for executing shell commands on the local operating system
 * and other system-level operations.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    8 Jun 2005
 */
public class System {

    /**
     * Private constructor prevents instantiation.
     */
    private System() {
    }

    /**
     * Executes an operating system command composed of a command and
     * one or more arguments.
     *
     * <p>Each of the <code>words</code> values is treated as a single
     * (possibly quoted) word in a shell command.
     * The first argument is the filename
     * (either a full pathname, or the name of a file on the current path)
     * of an executable file.
     * These values can be numeric, or strings, or something else, and
     * are converted automatically to string values.
     *
     * @example  <code>exec("/home/mbt/bin/process_obj.py", OBJ_NAME)</code>
     * @example  <code>exec("process_skycoords.py", RA, DEC)</code>
     * @example  <code>exec("process_sphericalcoords.sh", RA, DEC, 1.0)</code>
     *
     * @param   words  one or more words composing a shell command;
     *                 first is command and others are arguments
     * @return  short report message
     */
    public static String exec( Object... words ) {
        String[] argv = new String[ words.length ];
        for ( int i = 0; i < words.length; i++ ) {
            Object word = words[ i ];
            argv[ i ] = word == null ? "null" : word.toString();
        }
        return execute( Executor.createExecutor( argv ) );
    }

    /**
     * Executes a string as an operating system command.
     * Any spaces in the string are taken to delimit words (the first
     * word is the name of the command).
     *
     * @param  line  command line to execute
     * @return  short report message
     * @example  <code>exec("do_stuff.py " + RA + " " + DEC)</code>
     */
    public static String exec( String line ) {
        return exec( (Object[]) line.trim().split( " +" ) );
    }

    /**
     * Waits for a specified number of milliseconds.
     *
     * @param  millis  number of milliseconds to wait
     */
    public static void sleepMillis( int millis ) {
        try {
            Thread.sleep( millis );
        }
        catch ( InterruptedException e ) {
        }
    }

    /**
     * Waits for a specified number of seconds.
     *
     * @param  secs  number of seconds to wait
     */
    public static void sleep( int secs ) {
        sleepMillis( 1000 * secs );
    }

    /**
     * Performs the actual execution.
     *
     * @param   executor   object which can supply the execution process
     * @return  short report string
     */
    static String execute( Executor executor ) {
        String msg = executor.getLine();
        try {
            int status = executor.executeSynchronously( true );
            String out = executor.getOut();
            String err = executor.getErr();
            if ( out.length() > 0 ) {
                java.lang.System.out.print( out );
            }
            if ( err.length() > 0 ) {
                java.lang.System.err.print( err );
            }
            msg += status == 0 ? " (ok)" : " (error)";
        }
        catch ( InterruptedException e ) {
            msg += " (interrupted)";
        }
        catch ( IOException e ) {
            msg += " ("
                 + ( e.getMessage() == null ? e.toString() : e.getMessage() )
                 + ")";
        }
        return msg;
    }
}
