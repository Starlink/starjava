package uk.ac.starlink.ttools.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Writes invocation scripts for classes with main() methods in 
 * the TTOOLS package. 
 * It does this by making simple substitutions to the text of a template
 * script on the basis of reflection on classes.
 *
 * <p>The substitutions are:
 * <dl>
 *
 * <dt>@cmdname@
 * <dd>Changed to the result of the static <tt>getCommandName()</tt> method
 *
 * <dt>@CMDNAME@
 * <dd>Changed to the upper-cased version of @cmdname@
 *
 * <dt>@classname@
 * <dd>Changed to the name of the class
 *
 * </dl>
 * </p>
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Apr 2005
 */
public class ScriptWriter {

    private final String className_;
    private final String cmdName_;

    /**
     * Constructs a ScriptWriter which can write a script to invoke
     * the command based on the class <code>clazz</code>.
     * <code>clazz</code> must have methods with the following signatures:
     * <ol>
     * <li><code>public static void main(String[] args);</code></li>
     * <li><code>public static String getCommandName();</code></li>
     * </ol>
     *
     * @param   clazz  class
     */
    private ScriptWriter( Class clazz )
            throws NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException {
        className_ = clazz.getName();
        clazz.getDeclaredMethod( "main", new Class[] { String[].class } );
        cmdName_ = (String)
                   clazz.getDeclaredMethod( "getCommandName", new Class[ 0 ] )
                        .invoke( null, new Object[ 0 ] );
    }

    /**
     * Writes the invocation script for this writer's command.
     * The script is written to a file named <tt>cmdname</tt> in
     * a given directory.
     *
     * @param  template  template script 
     * @param  dir       destination directory
     */
    private void write( File template, File dir ) throws IOException {
        BufferedReader in = 
            new BufferedReader( new FileReader( template ) );
        Writer out =
            new BufferedWriter( new FileWriter( new File( dir, cmdName_ ) ) );
        out.write( "\n#\n#  THIS SCRIPT AUTOMATICALLY FROM A TEMPLATE" 
                    + "\n#  BY " + getClass().getName() + "\n#\n" );
        for ( String line; ( line = in.readLine() ) != null; ) {
            line = line.replaceAll( "@cmdname@", cmdName_ )
                       .replaceAll( "@CMDNAME@", cmdName_.toUpperCase() )
                       .replaceAll( "@classname@", className_ );
            out.write( line + "\n" );
        }
        in.close();
        out.close();
    }

    /**
     * Writes invocation scripts for commands based on a list of named
     * classes.  They are written to scripts named after the commands
     * in the current directory.
     *
     * @param   args   array of class names
     */
    public static void main( String[] args )
            throws IOException, NoSuchMethodException, ClassNotFoundException,
                   IllegalAccessException, InvocationTargetException {
        String usage = "Usage: ScriptWriter -dir <dir> -template <template> "
                     + "<classname> ...";
        List argList = new ArrayList( Arrays.asList( args ) );
        File dir = null;
        File template = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-dir" ) ) {
                it.remove();
                dir = new File( (String) it.next() );
                it.remove();
            }
            else if ( arg.equals( "-template" ) ) {
                it.remove();
                template = new File( (String) it.next() );
                it.remove();
            }
        }
        if ( dir == null || template == null ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            Class clazz = Class.forName( (String) it.next() );
            new ScriptWriter( clazz ).write( template, dir );
        }
    }
}
