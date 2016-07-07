package uk.ac.starlink.gbin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides read services for an object implementing the
 * <code>gaia.cu1.tools.dal.gbin.GbinReader</code> interface.
 * This object can read the elements stored in a GBIN file.
 *
 * <p>Access is via reflection, so I don't need GaiaTools on the classpath
 * at build time, for several reasons:
 * <ul>
 * <li>The GbinReader class is targeted at Java 1.7, and at time of 
 *     writing STIL is targeted at java 1.5</li>
 * <li>GbinReader and its dependencies are probably large and complicated
 *     (though I haven't actually checked that)</li>
 * <li>To use this class for most actual GBIN files, you're going to
 *     need Gaia data model classes on the classpath which I have no
 *     intention of packaging with STIL.  If you have the data model
 *     classes on the path, there's a good chance that you've got
 *     the relevant bits of GaiaTools as well.</li>
 * <li>It can benefit from future GbinReader implementations without
 *     requiring updates to the code in STIL (as long as the basic
 *     interfaces don't change).</li>
 * </ul>
 *
 * @author   Mark Taylor
 * @since    13 Aug 2014
 */
public class GbinObjectReader {

    private final Object gbinReaderObj_;
    private final Method hasNextMethod_;
    private final Method nextMethod_;
    private static final Object[] ARGS0 = new Object[ 0 ];
    private static boolean isGaiaToolsInit_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.gbin" );

    /**
     * Constructor.
     *
     * @param  gbinReaderObj  object implementing 
     *                        <code>gaia.cu1.tools.dal.gbin.GbinReader</code>
     * @throws   IllegalArgumentException   if <code>gbinReaderObj</code>
     *           doesn't appear to be a GbinReader
     */
    public GbinObjectReader( Object gbinReaderObj ) {
        gbinReaderObj_ = gbinReaderObj;
        Class clazz = gbinReaderObj.getClass();
        hasNextMethod_ = getNoArgMethod( clazz, "hasNext", boolean.class );
        nextMethod_ = getNoArgMethod( clazz, "next", null );
    }

    /**
     * Indicates whether this reader can read another element.
     *
     * @return  true if another record will be returned by <code>next</code>
     */
    public boolean hasNext() throws IOException {
        return Boolean.TRUE.equals( invokeNoArgMethod( hasNextMethod_ ) );
    }

    /**
     * Reads the next record.
     *
     * @return  object implementing <code>gaia.cu1.tools.dm.GaiaRoot</code>
     */
    public Object next() throws IOException {
        return invokeNoArgMethod( nextMethod_ );
    }

    /**
     * Invokes a no-arg method on this reader's GbinReader object,
     * translating exceptions appropriately.
     *
     * @param  method  method to invoke with no arguments
     * @return   method return value
     * @throws  IOException  with an informative method if something
     *          went wrong
     */
    private Object invokeNoArgMethod( Method method ) throws IOException {
        try {
            return method.invoke( gbinReaderObj_, ARGS0 );
        }
        catch ( IllegalAccessException e ) {
            throw new AssertionError( e );
        }
        catch ( IllegalArgumentException e ) {
            throw new AssertionError( e );
        }
        catch ( InvocationTargetException e ) {
            Throwable targetEx = e.getTargetException();
            if ( targetEx instanceof ClassNotFoundException ) {
                throw (IOException)
                      new IOException( "Missing GBIN object class"
                                     + " - probably lacking DM jars" )
                     .initCause( e );
            }
            else {
                throw (IOException)
                      new IOException( targetEx.getMessage() )
                     .initCause( e );
            }
        }
    }

    /**
     * Returns a public instance method with no arguments and a given
     * signature for a given class.
     *
     * @param  clazz  class to query
     * @param  name   method name
     * @param  retClazz   method return type
     * @return  method object
     * @throws  IllegalArgumentException with informative message
     *                                   if method does not exist
     */
    private static Method getNoArgMethod( Class<?> clazz, String name,
                                          Class<?> retClazz ) {
        Method method;
        try {
            method = clazz.getMethod( name, new Class[ 0 ] );
            int mods = method.getModifiers();
            if ( Modifier.isStatic( mods ) ||
                 ! Modifier.isPublic( mods ) ||
                 ( retClazz != null &&
                   ! retClazz.equals( method.getReturnType() ) ) ) {
                method = null;
            }
        }
        catch ( NoSuchMethodException e ) {
            method = null;
        }
        if ( method == null ) {
            throw new IllegalArgumentException( "Object of class "
                                              + clazz.getName()
                                              + " is not a GbinReader"
                                              + " (no suitable " + name + "()"
                                              + " method)" );
        }
        return method;
    }

    /**
     * Attempts to construct a GbinObjectReader that can read records
     * from a given input stream.
     *
     * @param  in  input stream containing a GBIN file
     * @return   gbin object reader
     */
    public static GbinObjectReader createReader( InputStream in )
            throws IOException {
        initGaiaTools();
        return new GbinObjectReader( createGbinReaderObject( in ) );
    }

    /**
     * Indicates whether a buffer containing the first few bytes of a
     * file look like a GBIN magic number.
     *
     * <p>Note this has not been exhaustively tested with all known
     * GBIN variants.
     *
     * @param   intro  first few bytes to test
     * @return   true if buffer looks like it could be the start of a GBIN file
     */
    public static boolean isMagic( byte[] intro ) {
        if ( intro.length < 13 ) {
            return false;
        }
        long magic = ((intro[ 7 ] & 0xffL) << 56)
                   | ((intro[ 6 ] & 0xffL) << 48)
                   | ((intro[ 5 ] & 0xffL) << 40)
                   | ((intro[ 4 ] & 0xffL) << 32)
                   | ((intro[ 3 ] & 0xffL) << 24)
                   | ((intro[ 2 ] & 0xffL) << 16)
                   | ((intro[ 1 ] & 0xffL) <<  8)
                   | ((intro[ 0 ] & 0xffL) <<  0);

        /* Pre-V3 GBIN file - no magic number, but we can see if it's
         * a java serialization stream. */
        if ( ( magic & 0xffff ) == 0xedac ) {
            int jserVers = ((intro[ 2 ] & 0xff) << 8)
                         | ((intro[ 3 ] & 0xff) << 0);
            logger_.info( "Java serialised stream, may be GBIN v2 file"
                        + " (java serialization version " + jserVers + ")" );
            return true;
        }

        /* V3 or later GBIN file - see GAIA-C1-TN-ESAC-AH-004-1. */
        else if ( magic == 0x1a0a0d4e49424789L ) {
            int gbinVers = ((intro[  9 ] & 0xff) << 24)
                         | ((intro[ 10 ] & 0xff) << 16)
                         | ((intro[ 11 ] & 0xff) <<  8)
                         | ((intro[ 12 ] & 0xff) <<  0);
            logger_.info( "Post-v2 GBIN file identified"
                        + " (GBIN version " + gbinVers + ")" );
            return true;
        }

        /* No known GBIN format. */
        return false;
    }

    /**
     * Performs GaiaTools setup required before GbinReaders are used etc.
     * Called by {@link #createReader createReader}.
     * Calls after the first one do nothing, but are harmless and cheap.
     * Uses reflection.
     */
    public static synchronized void initGaiaTools() {
        if ( ! isGaiaToolsInit_ ) {
            String loaderClassName = "gaia.cu1.tools.util.props.PropertyLoader";
            String loaderMethodName = "load";
            String sig = loaderClassName + "." + loaderMethodName + "()";
            logger_.info( "Invoking " + sig );
            try {
                Class.forName( loaderClassName )
                     .getMethod( loaderMethodName, new Class[ 0 ] )
                     .invoke( null, new Object[ 0 ] );
            }
            catch ( Throwable e ) {
                logger_.log( Level.WARNING, "Failed to invoke " + sig, e );
            }
            isGaiaToolsInit_ = true;
        }
    }

    /**
     * Constructs a GbinReader from a given input stream.
     * Uses reflection to invoke
     * <code>gaia.cu1.tools.dal.gbin.GbinFactory.getGbinReader()</code>.
     *
     * @param   in  input stream assumed to contain a GBIN file
     * @return  object implementing
     *          <code>gaia.cu1.tools.dal.gbin.GbinReader</code> interface
     * @throws   IOException  with informative message if something goes wrong,
     *           including reflection trouble
     */
    public static Object createGbinReaderObject( InputStream in )
            throws IOException {
        try {
            Class<?> factClazz =
                Class.forName( "gaia.cu1.tools.dal.gbin.GbinFactory" );
            Method getReaderMethod =
                factClazz.getMethod( "getGbinReader",
                                     new Class[] { InputStream.class } );
            try {
                return getReaderMethod.invoke( null, in );
            }
            catch ( IllegalArgumentException e ) {
                throw (IOException)
                      new IOException( "Problem with GaiaTools classes" )
                     .initCause( e );
            }
        }
        catch ( ClassNotFoundException e ) {
            throw (IOException)
                  new IOException( "GaiaTools classes not available?" )
                 .initCause( e );
        }
        catch ( NoSuchMethodException e ) {
            throw (IOException)
                  new IOException( "Problem with GaiaTools classes" )
                 .initCause( e );
        }
        catch ( IllegalAccessException e ) {
            throw (IOException)
                  new IOException( "Problem with GaiaTools classes" )
                 .initCause( e );
        } 
        catch ( InvocationTargetException e ) {
            throw (IOException)
                  new IOException( e.getTargetException().getMessage() )
                 .initCause( e );
        } 
    }
}
