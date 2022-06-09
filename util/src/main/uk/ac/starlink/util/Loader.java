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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
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
    private static Set<String> warnings = new HashSet<String>();
    private static Boolean is64Bit;

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

        /* Apparently Java 7 on OS X uses .dylib, whereas we use jnilib, let's
         * work around that until all Java's are 7. */
        if ( filename.endsWith( "dylib" ) ) {
            filename = filename.replace( ".dylib", ".jnilib" );
            logger.warning( "Replaced .dylib with .jnilib to fix Java 7" );
        }

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
    public static File getPropertiesFile() throws SecurityException {
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
        File propfile = null;
        try {
            propfile = getPropertiesFile();
            pstrm = new FileInputStream( propfile );
            Properties starProps = new Properties();
            InputStream propIn = new FileInputStream( propfile );
            starProps.load( propIn );
            propIn.close();

            /* Merge with system properties, but do not overwrite existing
             * system properties, since they were probably specified 
             * explicitly on the command line so should take precedence. */
            Properties sysProps = System.getProperties();
            for ( Map.Entry<Object,Object> starProp : starProps.entrySet() ) {
                String key = (String) starProp.getKey();
                if ( ! sysProps.containsKey( key ) ) {
                    sysProps.put( key, starProp.getValue() );
                }
            }
            logger.config( "Properties read from " + propfile );
        }
        catch ( FileNotFoundException e ) {
            logger.config( "No properties file " + propfile + " found" );
        }
        catch ( IOException e ) {
            logger.warning( "Error reading properties from " + propfile 
                          + " " + e );
        }
        catch ( SecurityException e ) {
            logger.warning( "Can't load properties " + e );
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
     * <p>A bean configuration parenthesis may be optionally appended,
     * as per {@link BeanConfig}.
     *
     * @param   classSpec  name of the class to instantiate
     * @param   type   class which the instantiated class must be assignable
     *                 from
     * @return  new <tt>className</tt> instance, or <tt>null</tt>
     */
    public static <T> T getClassInstance( String classSpec, Class<T> type ) {
        if ( classSpec == null || classSpec.trim().length() == 0 ) {
            return null;
        }
        BeanConfig config = BeanConfig.parseSpec( classSpec );
        String className = config.getBaseText();
        Class<?> clazz;
        try {
            clazz = Class.forName( className );
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
        final T target;
        try {
            target = type.cast( clazz.newInstance() );
        }
        catch ( ExceptionInInitializerError e ) {
            warn( e.getCause() + " loading class " + className );
            return null;
        }
        catch ( Throwable th ) {
            warn( th + " instantiating " + clazz.getName() );
            return null;
        }
        try {
            config.configBean( target );
        }
        catch ( LoadException e ) {
            warn( "Configuration failed for " + config.getConfigText()
                + ": " + e );
        }
        return target;
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
    public static <T> List<T> getClassInstances( String propertyName,
                                                 Class<T> type ) {
        List<T> instances = new ArrayList<T>();

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
            T inst = getClassInstance( cname, type );
            if ( inst != null ) {
                instances.add( inst );
            }
        } 

        /* Return the list. */
        return instances;
    }

    /**
     * Returns a list of class instances got from a combination of a 
     * default list of classnames and the name of a property which 
     * may contain a colon-separated list of other classnames.
     * The strings in each case must name classes which implement 
     * <tt>type</tt> and which have no-arg constructors.
     *
     * @param  defaultNames  array of string
     */
    public static <T> List<T> getClassInstances( String[] defaultNames, 
                                                 String propertyName,
                                                 Class<T> type ) {
        Loader.loadProperties();
        List<T> instances = new ArrayList<T>();
        for ( int i = 0; i < defaultNames.length; i++ ) {
            T instance = getClassInstance( defaultNames[ i ], type );
            if ( instance != null ) {
                instances.add( instance );
            }
        }
        instances.addAll( getClassInstances( propertyName, type ) );
        return instances;
    }

    /**
     * Tests whether the JVM appears to be 64-bit or not.
     * Not guaranteed reliable.
     *
     * @return  true if the JVM appears to be running in 64 bits
     *          (if false, presumably it's 32 bits)
     */
    public static boolean is64Bit() {
        if ( is64Bit == null ) {
            boolean is64;
            Loader.loadProperties();
            try {
                String archdm = System.getProperty( "sun.arch.data.model" );
                if ( archdm != null ) {
                    is64 = "64".equals( archdm );
                    logger.config( "sun.arch.data.model=" + archdm
                                 + ": assuming " + ( is64 ? "64" : "32" )
                                 + "-bit JVM" );
                }
                else {
                    String arch = System.getProperty( "os.arch", "unknown" );
                    is64 = arch.indexOf( "64" ) >= 0;
                    logger.config( "os.arch=" + arch + ": assuming "
                                 + ( is64 ? "64" : "32" ) + "-bit JVM" );
                }
            }
            catch ( SecurityException e ) {
                logger.info( "Can't determine os.arch (" + e 
                           + ") - assume 32-bit" );
                is64 = false;
            }
            is64Bit = Boolean.valueOf( is64 );
        }
        return is64Bit.booleanValue();
    }

    /**
     * Sets a system property to a given value unless it has already been set.
     * If it has a prior value, that is undisturbed.
     * Potential security exceptions are caught and dealt with.
     *
     * @param   key  property name
     * @param   value  suggested property value
     */
    public static void setDefaultProperty( String key, String value ) {
        String existingVal = System.getProperty( key );
        if ( existingVal == null || existingVal.trim().length() == 0 ) {
            try {
                System.setProperty( key, value );
            }
            catch ( SecurityException e ) {
                warn( "Security manager prevents setting of " + key );
            }
        }
    }

    /** 
     * Unless it's been set already, sets the value of the 
     * <tt>apple.laf.useScreenMenuBar</tt> system property to true.
     * This has the effect on Macintosh displays of causing menus to 
     * appear at the top of the screen rather than the top of the
     * windows they belong in.  This doesn't work on Dialog windows.
     * Setting this property has no effect on non-Mac platforms.
     * Probably(?) this call must be made before starting up the GUI
     * (haven't got a Mac to hand, can't test it).
     */
    public static void tweakGuiForMac() {
        setDefaultProperty( "apple.laf.useScreenMenuBar", "true" );
    }

    /**
     * Configures the "http.agent" system property.
     * This sets the value of the "User-Agent" request header in outgoing
     * HTTP requests.  It is good practice to set this to identify the
     * running application, so that external web servers can see who is
     * using their services.
     * If this property is not set, the User-Agent header will probably
     * just report that it's Java.
     * In the event that the http.agent property is already set on entry,
     * there is no effect.
     *
     * <p>According to RFC2616 sections 14.43 and 3.8, this string should
     * be a whitespace-separated sequence of product tokens.
     * A product token is of the form <tt>product-name/product-version</tt>.
     *
     * <p>This method must be called before the current JVM has opened 
     * any HTTP connections to have an effect.
     *
     * @param  productTokens  one or more (whitespace-separated) name/version
     *              application identifiers
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43"
     *         >RFC 2616, Section 14.43</a>
     */
    public static void setHttpAgent( String productTokens ) {
        setDefaultProperty( "http.agent", productTokens );
    }

    /**
     * Checks that the JRE contains classes that you'd expect it to.
     * This is chiefly useful for bailing out if we find ourself running
     * in Gnu GCJ, which in at least some early versions is rather incomplete.
     * In the case that J2SE classes are missing, an exception will be
     * thrown.  The text of this exception will be a user-friendly message
     * about what has gone wrong.
     *
     * @throws  ClassNotFoundException in case of a defective JRE
     */
    public static void checkJ2se() throws ClassNotFoundException {
        try {
            Class.forName( "java.util.LinkedHashMap" );
            Class.forName( "org.w3c.dom.Node" );
            Class.forName( "javax.swing.table.DefaultTableModel" );
        }
        catch ( ClassNotFoundException e ) {
            String msg = new StringBuffer()
                .append( "\n" )
                .append( "The runtime Java Runtime Environment (JRE) " )
                .append( "is missing some standard compile-time classes.\n" )
                .append( "A possible reason is that you are using " )
                .append( "an incomplete java such as\n" )
                .append( "certain versions of GNU gcj.\n" )
                .append( "The JVM you are using is " )
                .append( System.getProperty( "java.vm.name", "unknown" ) )
                .append( " version " )
                .append( System.getProperty( "java.vm.version", "?" ) )
                .append( ".\n" )
                .append( "The recommended JRE is Oracle's J2SE " )
                .append( "version 1.8 or greater,\n" )
                .append( "though others may also work.\n" )
                .toString();
             throw new ClassNotFoundException( msg, e );
        }
    }

    /**
     * Checks the reported vendor for this J2SE.
     * Depending on what is found, logging messages may be written.
     * In particular, if it looks like GNU a warning-level message is emitted.
     */
    public static void checkJ2seVendor() {
        String vendorProp = "java.vendor";
        String vendor = System.getProperty( vendorProp );
        String recommendedDownload = "http://java.sun.com/javase/downloads/";
        final Level vendorLevel;
        final String[] vendorMsgs;
        if ( vendor.matches( "[Ss]un\\b.*" ) ||
             vendor.matches( "[Oo]racle\\b.*" ) ||
             vendor.matches( "[Aa]pple\\b.*" ) ) {
            vendorLevel = Level.CONFIG;
            vendorMsgs = new String[ 0 ];
        }
        else if ( vendor.matches( ".*Free Software Foundation.*" ) ||
                  vendor.matches( ".*\\bFSF\\b.*" ) ||
                  vendor.matches( ".*\\bGNU\\b.*" ) ) {
            vendorLevel = Level.INFO;
            vendorMsgs = new String[] {
                "You appear to be running GNU/FSF Java",
                "In at least some versions, "
                + "this is an incomplete implementation",
                "The application may work incorrectly, or not at all",
                "You are advised to use Sun/Oracle's Java implementation "
                + "instead:",
                recommendedDownload,
            };
        }
        else {
            vendorLevel = Level.INFO;
            vendorMsgs = new String[] {
                "JRE is not Sun's - may or may not work properly",
            };
        }
        logger.log( vendorLevel, vendorProp + "=" + vendor );
        for ( int i = 0; i < vendorMsgs.length; i++ ) {
            logger.log( vendorLevel, vendorMsgs[ i ] );
        }
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
