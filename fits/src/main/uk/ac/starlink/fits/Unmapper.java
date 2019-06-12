package uk.ac.starlink.fits;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.Loader;

/**
 * Attempts to free resources from a MappedByteBuffer.
 *
 * <p>This is a tricky business.  There is no good way to unmap the memory
 * mapped by a MappedByteBuffer.  The MappedByteBuffer javadocs say
 * <i>"A mapped byte buffer and the file mapping that it represents remain
 * valid until the buffer itself is garbage-collected."</i>,
 * and there is no explicit unmap method.
 * However, since the resources locked by memory mapping are separate
 * from the JVM heap, there is no guarantee that garbage collection will
 * happen even when mapped memory reaches crisis levels, and in practice
 * this can lead to cache thrashing and the OS locking up even when there
 * are many MappedByteBuffers without active references, at least on
 * some platforms (I see it on Scientific Linux 6.5 with 24Gb RAM, but
 * not SL 6.3 with 4Gb, but I'm not sure what the relevant differences are).
 *
 * <p>The preferred method used here is to employ classes in the
 * implementation-specific <code>sun.*</code> namespace to do the
 * unmapping.  This is clearly not guaranteed to work on all J2SE
 * implementations, and moreover it risks a JVM crash if the
 * buffer instance is used after the umapping has been done.
 * So use it WITH EXTREME CAUTION.
 * If the relevant classes are not available at runtime,
 * unmapping simply isn't done.
 *
 * <p>See also Java Bug
 * <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4724038"
 *                                                   >Java Bug #4724038</a>.
 *
 * <p>Forcing garbage collection with <code>System.gc()</code> calls would
 * be another possibility (may be added here at some point),
 * but it's not guaranteed to do anything, and doing it too often
 * would impact performance.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2014
 */
public abstract class Unmapper {

    private static Unmapper instance_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Name of system property to control buffer unmapping ({@value}).
     * Possible values are currently:
     * <ul>
     * <li>"<code>sun</code>": best-efforts sun.misc-based option</li>
     * <li>"<code>cleaner</code>": sun.misc.Cleaner-based option
     *     (available in Oracle java6 through java8)</li>
     * <li>"<code>unsafe</code>": sun.misc.Unsafe-based option
     *     (available in Oracle java9 and later?)</li>
     * <li>"<code>none</code>": no unmapping</li>
     * </ul>
     * The default is to use "<code>sun</code>" if available,
     * else fall back to none.
     * You can also give the classname of an <code>Unmapper</code>
     * concrete subclass with a no-arg constructor.
     */
    public static final String UNMAP_PROPERTY = "startable.unmap";

    /**
     * Attempts to release the resources (mapped memory) used by a given
     * MappedByteBuffer.  The buffer <strong>MUST NOT</strong> be read
     * after this call has been made.
     *
     * @param  buf  buffer
     * @return   true if unmapping was successfully performed
     */
    public abstract boolean unmap( MappedByteBuffer buf );

    /**
     * Returns an instance of this class.
     *
     * @return  instance
     */
    public synchronized static Unmapper getInstance() {
        if ( instance_ == null ) {
            instance_ = createInstance();
        }
        return instance_;
    }

    /**
     * Constructs an Unmapper instance suitable for the current platform.
     *
     * @return  instance
     */
    private static Unmapper createInstance() {
        String pref;
        try {
            pref = System.getProperty( UNMAP_PROPERTY );
        }
        catch ( SecurityException e ) {
            pref = null;
        }
        Option opt;
        if ( pref == null ) {
            logger_.info( "Buffer unmapping: "
                        + "attempt to use sun.misc implementation" );
            opt = Option.SUN;
        }
        else {
            opt = Option.getOption( pref );
            if ( opt != null ) {
                logger_.info( "Buffer unmapping: "
                            + "attempt to use " + opt
                            + " by explicit request" );
            }
        }
        if ( opt != null ) {
            try {
                Unmapper unmapper = opt.createUnmapper();
                logger_.config( "Buffer unmapping: using " + unmapper
                              + "; unmapping should work" );
                return unmapper;
            }
            catch ( Throwable e ) {
                logger_.log( Level.WARNING,
                             "Buffer unmapping failed, fall back to no-op", e );
                return new NopUnmapper();
            }
        }
        else if ( "none".equalsIgnoreCase( pref ) ) {
            logger_.info( "Buffer unmapping: "
                        + "using no-op unmapper by explicit request" );
            logger_.config( "Buffer unmapping: no explicit unmapping" );
            return new NopUnmapper();
        }
        else {
            Unmapper unmapper = Loader.getClassInstance( pref, Unmapper.class );
            if ( unmapper != null ) {
                logger_.info( "Using custom buffer unmapper " + pref
                            + " by explicit request" );
                return unmapper;
            }
            else {
                logger_.log( Level.WARNING,
                             "Can't use unknown unmapper " + pref
                           + ", fall back to no-op" );
                return new NopUnmapper();
            }
        }
    }

    /**
     * Returns an unmapper using one of the sun.misc-based classes,
     * according to what's available.
     */
    private static Unmapper createSunUnmapper() throws Exception {
        boolean hasCleaner;
        try {
            Class.forName( "sun.misc.Cleaner" );
            hasCleaner = true;
        }
        catch ( ClassNotFoundException e ) {
            hasCleaner = false;
        }
        return hasCleaner ? new CleanerUnmapper()
                          : new UnsafeUnmapper();
    }

    /**
     * Unmapper implementation using the Sun-specific sun.misc.Cleaner class.
     * This is present in Oracle java6 through java8.
     */
    private static class CleanerUnmapper extends Unmapper {
        private final Class<?> directBufferClazz_;
        private final Class<?> cleanerClazz_;
        private final Method cleanerMethod_;
        private final Method cleanMethod_;

        /**
         * Constructor.
         */
        CleanerUnmapper() throws Exception {
            directBufferClazz_ = Class.forName( "sun.nio.ch.DirectBuffer" );
            cleanerClazz_ = Class.forName( "sun.misc.Cleaner" );
            cleanerMethod_ = directBufferClazz_.getMethod( "cleaner" );
            cleanMethod_ = cleanerClazz_.getMethod( "clean" );
        }

        public boolean unmap( MappedByteBuffer buf ) {
            if ( directBufferClazz_.isAssignableFrom( buf.getClass() ) ) {
                try {
                    cleanMethod_.invoke( cleanerMethod_.invoke( buf ) );
                    return true;
                }
                catch ( Exception e ) {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "Cleaner";
        }
    }

    /**
     * Unmapper implementation using the Sun-specific sun.misc.Unsafe class.
     * This is present in Oracle java9.
     */
    private static class UnsafeUnmapper extends Unmapper {
        private final Object unsafe_;
        private final Method invokeCleanerMethod_;

        UnsafeUnmapper() throws Exception {
            Class<?> unsafeClazz = Class.forName( "sun.misc.Unsafe" );
            Field theUnsafe = unsafeClazz.getDeclaredField( "theUnsafe" );
            theUnsafe.setAccessible( true );
            unsafe_ = theUnsafe.get( null );
            invokeCleanerMethod_ =
                unsafeClazz.getDeclaredMethod( "invokeCleaner",
                                               ByteBuffer.class );
        }

        public boolean unmap( MappedByteBuffer buf ) {
            try {
                invokeCleanerMethod_.invoke( unsafe_, buf );
                return true;
            }
            catch ( Exception e ) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "Unsafe";
        }
    }

    /**
     * Unmapper that does nothing.
     */
    private static class NopUnmapper extends Unmapper {
        public boolean unmap( MappedByteBuffer buf ) {
            return false;
        }
        public String toString() {
            return "Nop";
        }
    }

    /**
     * Named user-selectable options for unmapping.
     */
    private enum Option {
        CLEANER() {
            Unmapper createUnmapper() throws Exception {
                return new CleanerUnmapper();
            }
        },
        UNSAFE() {
            Unmapper createUnmapper() throws Exception {
                return new UnsafeUnmapper();
            }
        },
        SUN() {
            Unmapper createUnmapper() throws Exception {
                return createSunUnmapper();
            }
        };

        /**
         * Attempts to create an unmapper.
         *
         * @return  new unmapper
         */
        abstract Unmapper createUnmapper() throws Exception;

        /**
         * Returns a member of this enum if it matches the given name.
         *
         * @param  name  enum name, case-insensitive
         * @return  named instance, or null
         */
        static Option getOption( String name ) {
            for ( Option opt : values() ) {
                if ( opt.toString().equalsIgnoreCase( name ) ) {
                    return opt;
                }
            }
            return null;
        }
    }
}
