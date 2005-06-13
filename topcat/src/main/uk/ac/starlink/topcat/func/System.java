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
     * Executes an operating system command with one argument.
     *
     * @param   cmd  name of command
     * @param   arg1  the first argument
     * @return  short log message
     */
    public static String exec( String cmd, String arg1 ) {
        return exec( new String[] { cmd, arg1 } );
    }

    /**
     * Executes an operating system command with two arguments.
     *
     * @param   cmd  name of command
     * @param   arg1  the first argument
     * @param   arg2  the second argument
     * @return  short log message
     */
    public static String exec( String cmd, String arg1, String arg2 ) {
        return exec( new String[] { cmd, arg1, arg2 } );
    }

    /**
     * Executes an operating system command with three arguments.
     *
     * @param   cmd  name of command
     * @param   arg1  the first argument
     * @param   arg2  the second argument
     * @param   arg3  the third argument
     * @return  short log message
     */
    public static String exec( String cmd, String arg1, String arg2, 
                               String arg3 ) {
        return exec( new String[] { cmd, arg1, arg2, arg3 } );
    }

    /**
     * Executes an array of strings to be interpreted as the words of
     * a system command.
     *
     * @param   argv  argument vector
     * @return  short report message
     */
    private static String exec( String[] argv ) {
        return execute( Executor.createExecutor( argv ) );
    }

    /**
     * Executes a string as an operating system command.
     * Any spaces in the string are taken to delimit words (the first
     * word is the name of the command).
     *
     * @param  line  command line to execute
     * @return  short report message
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
        String msg = "";
        try {
            int status = executor.executeSynchronously();
            String out = executor.getOut();
            String err = executor.getErr();
            if ( err.length() > 0 ) {
                msg = "\n" + err;
            }
            else if ( out.length() > 0 ) {
                msg = "\n" + out;
            }
            else if ( status != 0 ) {
                msg = " (error)";
            }
        }
        catch ( InterruptedException e ) {
            msg = " (interrupted)";
        }
        catch ( IOException e ) {
            msg = " ("
                + ( e.getMessage() == null ? e.toString() : e.getMessage() )
                + ")";
        }
        return executor.getLine() + msg;
    }
}
