package uk.ac.starlink.ttools.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Writes invocation scripts for classes with main() methods in 
 * the TTOOLS package. 
 * It does this by making simple substitutions to the text of a template
 * script on the basis of reflection on classes.
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
        cmdName_ = (String)
                   clazz.getDeclaredMethod( "getCommandName", new Class[ 0 ] )
                        .invoke( null, new Object[ 0 ] );
    }

    /**
     * Writes the invocation script for this writer's command.
     * It is written to a file named after the command's name in the
     * current directory.
     */
    private void write() throws IOException {
        BufferedReader in = 
            new BufferedReader( 
                new InputStreamReader( getClass()
                                      .getResource( "template.script" )
                                      .openStream() ) );
        Writer out =
            new BufferedWriter( new FileWriter( new File( cmdName_ ) ) );
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
        for ( int i = 0; i < args.length; i++ ) {
            new ScriptWriter( Class.forName( args[ i ] ) ).write();
        }
    }
}
