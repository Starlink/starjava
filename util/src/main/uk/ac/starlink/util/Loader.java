package uk.ac.starlink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
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
    private static Set warnings = new HashSet();

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
        Throwable err1;
        try {
            System.loadLibrary( libname );
            return;
        }
        catch ( SecurityException e ) {
            err1 = e;
        }
        catch ( LinkageError e ) {
            err1 = e;
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
            throw (UnsatisfiedLinkError)
                  new UnsatisfiedLinkError( "couldn't load library " + 
                                            libname + ": " + e.getMessage() )
                 .initCause( err1 );
        }
        catch ( LinkageError e ) {
            throw (UnsatisfiedLinkError)
                  new UnsatisfiedLinkError( "couldn't load library " +
                                            libname + ": " + e.getMessage() )
                 .initCause( err1 );
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
            propsLoaded = true;
        }
    }

    /**
     * Attempts to obtain an instance of a class with a given name which
     * is an instance of a given type.  If <tt>className</tt> is null or
     * empty, null is returned directly.  Otherwise, if the class 
     * <tt>className</tt>
     * can be found using the default class loader, and if it is assignable
     * from <tt>type</tt>, and if it has a no-arg constructor, an instance
     * of it is constructed and returned.  Otherwise, <tt>null</tt> is
     * returned, and a message may be written through the logging system.
     *
     * @param   className  name of the class to instantiate
     * @param   type   class which the instantiated class must be assignable
     *                 from
     * @return  new <tt>className</tt> instance, or <tt>null</tt>
     */
    public static Object getClassInstance( String className, Class type ) {
        if ( className == null || className.trim().length() == 0 ) {
            return null;
        }
        Class clazz;
        try {
            clazz = new Object().getClass().forName( className );
        }
        catch ( ClassNotFoundException e ) {
            warn( "Class " + className + " not found" );
            return null;
        }
        catch ( ExceptionInInitializerError e ) {
            warn( e.getCause() + " loading class " + className );
            return null;
        }
        catch ( LinkageError e ) {
            warn( e + " loading class " + className );
            return null;
        }
        if ( ! type.isAssignableFrom( clazz ) ) {
            warn( "Class " + clazz.getName() + " is not a " + type.getName() );
            return null;
        }
        try {
            return clazz.newInstance();
        }
        catch ( ExceptionInInitializerError e ) {
            warn( e.getCause() + " loading class " + className );
            return null;
        }
        catch ( Throwable th ) {
            warn( th + " instantiating " + clazz.getName() );
            return null;
        }
    }

    /**
     * Attempts to obtain instances of a class from a colon-separated list
     * of classnames in a named system property.  If the named property
     * does not exists or contains no strings, an empty list is returned.
     * Otherwise, {@link #getClassInstance} is called on each colon-separated
     * element of the property value, and if there is a non-null return, 
     * it is added to the return list.  For colon-separated elements which
     * do not correspond to usable classes, a message may be written 
     * through the logging system.
     *
     * @param   propertyName  name of a system property containing 
     *          colon-separated classnames
     * @param   type   class which instantiated classes must be assignable from
     * @return  list of new <tt>type</tt> instances (may be empty, but not null)
     */
    public static List getClassInstances( String propertyName, Class type ) {
        List instances = new ArrayList();

        /* Get the property value, if possible. */
        String propVal;
        try {
            propVal = System.getProperty( propertyName );
        }
        catch ( SecurityException e ) {
            return instances;
        }
        if ( propVal == null || propVal.trim().length() == 0 ) {
            return instances;
        }

        /* Try to get an instance from each colon-separated element in turn. */
        for ( StringTokenizer stok = new StringTokenizer( propVal, ":" );
              stok.hasMoreElements(); ) {
            String cname = stok.nextToken().trim();
            Object inst = getClassInstance( cname, type );
            if ( inst != null ) {
                instances.add( inst );
            }
        } 

        /* Return the list. */
        return instances;
    }

    /**
     * Register a warning.  Log it through the logger, unless we've said
     * the same thing already.
     * 
     * @param  message  warning message
     */
    private static void warn( String message ) {
        if ( ! warnings.contains( message ) ) {
            logger.warning( message );
            warnings.add( message );
        }
    }
}
