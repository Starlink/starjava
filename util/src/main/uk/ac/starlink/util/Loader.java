package uk.ac.starlink.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utilities associated with loading resources.
 * Perhaps this functionality should be recast as a Starlink ClassLoader
 * at some point.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Loader {

    /**
     * Returns the location of the main Starlink java directory which 
     * contains the lib, bin, etc, source directories and others.
     * It gets this by working out what jar file this class
     * has been loaded from - there may be circumstances under which 
     * this doesn't work? but it's a best guess.
     * 
     * <p>If for some reason the directory cannot be located, null
     * is returned.
     *
     * @return   the top level starlink java directory, or <tt>null</tt>
     *           if it can't be found
     */
    public static File starjavaDirectory() {
        URL classURL = Loader.class.getResource( "Loader.class" );
        Matcher matcher = Pattern.compile( "jar:file:(.*)!.*" )
                                 .matcher( classURL.toString() );
        if ( matcher.matches() ) {
            File jarfile = new File( matcher.group( 1 ) );
            if ( jarfile.exists() ) {
                File sjfile;
                try {
                    sjfile = jarfile.getCanonicalFile()  // util.jar
                                    .getParentFile()     // util
                                    .getParentFile()     // lib
                                    .getParentFile();    // java

                }
                catch ( IOException e ) {
                    return null;
                }
                catch ( NullPointerException e ) {
                    return null;
                }
                if ( sjfile.exists() ) {
                    return sjfile;
                }
            }
        }
        return null;
    }

    /**
     * Loads a native library given its name.  If it is not found on
     * java.library.path, the architecture-specific lib directory in 
     * the installed Starlink system is searched.
     *
     * @param  the name of the library (not including system-specifics 
     *         such as 'lib' or '.so')
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkLink</tt> method doesn't allow loading of the 
     *         specified dynamic library
     * @throws UnsatisfiedLinkError  if the library does not exist
     * @see    java.lang.System#loadLibrary
     */
    public static void loadLibrary( String libname )
            throws SecurityException, UnsatisfiedLinkError {

        /* Try to pick the library up off the path in the usual way. */
        try {
            System.loadLibrary( libname );
            return;
        }
        catch ( SecurityException e ) {
            // drop through
        }
        catch ( UnsatisfiedLinkError e ) {
            // drop through
        }

        /* If we have arrived here, it's not on the path.  Try to find it
         * in the installed Starlink library location. */

        /* Get base library location. */
        File libdir = new File( starjavaDirectory(), "lib" );

        /* Get architecture-specific library location. */
        String arch = System.getProperty( "os.arch" );
        File archdir = new File( libdir, arch );

        /* Get the name of the library file. */
        String filename;
        if ( arch.equals( "x86" ) ) {
            filename = libname + ".dll";
        }
        else if ( arch.equals( "alpha" ) ) {
            filename = "lib" + libname + ".so";
        }
        else if ( arch.equals( "sparc" ) ) {
            filename = "lib" + libname + ".so";
        }
        else if ( arch.equals( "i386" ) ) {
            filename = "lib" + libname + ".so";
        }
        else {  // best guess
            filename = "lib" + libname + ".so";
        }

        /* Try to load it. */
        File libfile = new File( archdir, filename );
        System.load( libfile.toString() );
    }
}
