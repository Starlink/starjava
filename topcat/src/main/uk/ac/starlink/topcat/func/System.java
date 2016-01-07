// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.io.IOException;

/**
 * Executes commands on the local operating system.  These are executed as
 * if typed in from the shell, or command line.
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
     * Executes an operating system command composed of one or more words.
     *
     * <p>Each supplied argument is passed to the execution like a single
     * (possibly quoted) word in a shell command.
     * The first one is the filename
     * (either a full pathname, or the name of a file on the current path)
     * of an executable file.
     *
     * @example  <code>exec("/home/mbt/bin/process_obj.py", OBJ_NAME)</code>
     * @example  <code>exec("process_coords.py", toString(RA), toString(DEC))
     *                 </code>
     *
     * @param   words  one or more words comprising the command
     * @return  short report message
     */
    public static String exec( String... words ) {
        return execute( Executor.createExecutor( words ) );
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
        return execute( Executor.createExecutor( line ) );
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
            int status = executor.executeSynchronously();
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
