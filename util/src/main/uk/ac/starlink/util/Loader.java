package uk.ac.starlink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utilities associated with loading resources.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Loader {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.util" );
    private static boolean propsLoaded = false;

    /** 
     * Name of the file in the user's home directory from which properties
     * are loaded.
     */
    public static final String PROPERTIES_FILE = ".starjava.properties";

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
        Matcher matcher = Pattern.compile( "^jar:(file:.*?)!.*" )
                                 .matcher( classURL.toString() );
        if ( matcher.matches() ) {
            URI jaruri;
            try {
                jaruri = new URI( matcher.group( 1 ) );
            }
            catch ( URISyntaxException e ) {
                logger.warning( "Unexpected URI syntax exception + e" );
                return null;
            }
            File jarfile = new File( jaruri );
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
        else {
            logger.warning( "Loader.class location " 
                          + classURL + " unexpected" );
        }
        return null;
    }

    /**
     * Loads a native library given its name.  If it is not found on
     * java.library.path, the architecture-specific lib directory in 
     * the installed Starlink system is searched.
     *
     * @param  libname the name of the library (not including system-specifics 
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
        String filename = System.mapLibraryName( libname );

        /* Try to load it. */
        File libfile = new File( archdir, filename );
        try {
            System.load( libfile.getCanonicalPath() );
        }
        catch (IOException e) {
            throw new UnsatisfiedLinkError( e.getMessage() );
        }
    }

    /**
     * Returns the name of the file from which properties will be loaded
     * by this class.
     *
     * @return  a file called {@link #PROPERTIES_FILE} in the directory
     *          given by the System property "<tt>user.home</tt>".
     */
    public static File getPropertiesFile() {
        return new File( System.getProperty( "user.home" ),
                         PROPERTIES_FILE );
    }

    /**
     * Ensures that the user's customised properties have been loaded;
     * these are read once from the file returned by the 
     * {@link #getPropertiesFile} method and incorporated into 
     * the System properties.
     * Calling this method after the first time has no effect.
     *
     * @see  java.lang.System#getProperties
     */
    public static synchronized void loadProperties() {

        /* No action required if we have already done this. */
        if ( propsLoaded ) {
            return;
        }

        /* Otherwise try to load them. */
        InputStream pstrm = null;
        File propfile = getPropertiesFile();
        try {
            pstrm = new FileInputStream( propfile );
            Properties starProps = new Properties();
            starProps.load( new FileInputStream( propfile ) );
            System.getProperties().putAll( starProps );
            logger.config( "Properties read from " + propfile );
        }
        catch ( FileNotFoundException e ) {
            logger.config( "No properties file " + propfile + " found" );
        }
        catch ( IOException e ) {
            logger.warning( "Error reading properties from " + propfile 
                          + " " + e );
        }
        finally {
            if ( pstrm != null ) {
                try {
                    pstrm.close();
                }
                catch ( IOException e ) {
                    // no action
                }
            }
        }
    }

}
