package uk.ac.starlink.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a set of object references and corresponding cleaning actions.
 * It is used to manage end-of-life actions for an object,
 * in particular to replace use of finalizers, which at Java 21 are
 * deprecated for removal (and which are generally frowned upon).
 *
 * <p>This is a more-or-less drop-in replacement for the
 * <code>java.lang.ref.Cleaner</code> class introduced in Java 9.
 * If the target platform for this package gets upgraded to Java 9 or greater,
 * this class can be retired in favour of the JRE implementation.
 *
 * <p>The {@link #getInstance} method lazily constructs a singleton instance,
 * which is probably suitable for most purposes.  But you can construct
 * your own if you want.
 *
 * @author   Mark Taylor
 * @since    25 Jul 2024
 */
public class Cleaner {

    private final ReferenceQueue<Object> queue_;
    private final Thread cleanerThread_;
    private final Map<PhantomReference<Object>,Runnable> refMap_;
    private static Cleaner instance_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /**
     * Constructor.
     */
    public Cleaner() {
        queue_ = new ReferenceQueue<Object>();
        refMap_ = new HashMap<PhantomReference<Object>,Runnable>();
        cleanerThread_ = new Thread( this::serviceQueue, "cleaner" );
        cleanerThread_.setDaemon( true );
        cleanerThread_.start();
    }

    /**
     * Registers an object and a cleaning action to run when
     * the object becomes phantom reachable. 
     *
     * @param  obj  the object to monitor
     * @param  action  a Runnable to invoke when the object becomes
     *                 phantom reachable
     * @return  a Cleanable instance
     */
    public Cleanable register( Object obj, Runnable action ) {
        PhantomReference<Object> ref = new PhantomReference<>( obj, queue_ );
        refMap_.put( ref, action );
        return () -> clean( ref );
    }

    /**
     * If a given reference is registered for cleaning, perform its
     * cleaning action and unregister it.
     * No effect if the reference is not registered.
     *
     * @param  ref  reference to clean
     */
    private void clean( Reference<?> ref ) {
        Runnable action = refMap_.remove( ref );
        if ( action != null ) {
            action.run();
        }
    }

    /**
     * Wait for references to show up in the queue, and process them
     * by calling clean on them.  Does not terminate.
     */
    private void serviceQueue() {
        while ( true ) {
            try {
                clean( queue_.remove() );
            }
            catch ( InterruptedException e ) {
                logger_.log( Level.WARNING, "Clean loop interrupted", e );
            }
            catch ( Exception e ) {
                logger_.log( Level.WARNING, "Clean failed", e );
            }
        }
    }

    /**
     * Returns a lazily constructed singleton instance of this class.
     *
     * @return  singleton instance
     */
    public static Cleaner getInstance() {
        if ( instance_ == null ) {
            instance_ = new Cleaner();
        }
        return instance_;
    }

    /**
     * Cleanable represents an object and a cleaning action registered
     * in a Cleaner.
     */
    @FunctionalInterface
    public interface Cleanable {

        /**
         * Unregisters the cleanable and invokes the cleaning action.
         */
        void clean();
    }
}
