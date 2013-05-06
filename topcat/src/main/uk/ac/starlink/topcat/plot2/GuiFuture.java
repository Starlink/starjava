package uk.ac.starlink.topcat.plot2;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * FutureTask which passes the computation's result when ready
 * on to a consumer method in the Event Dispatch Thread.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public abstract class GuiFuture<V> extends FutureTask<V> {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot2" );

    /**
     * Constructs a GuiFuture using a Callable.
     *
     * @param   callable  performs the computation
     */
    public GuiFuture( Callable<V> callable ) {
        super( callable );
    }

    /**
     * Constructs a GuiFugure using a Factory.
     *
     * @param  factory   performs the computation
     */
    public GuiFuture( final Factory<V> factory ) {
        this( new Callable<V>() {
            public V call() {
                return factory.getItem();
            }
        } );
    }

    @Override
    protected void done() {
        boolean success;
        V value;
        if ( isCancelled() ) {
            success = false;
            value = null;
        }
        else {
            try {
                value = get();
                success = true;
            }
            catch ( InterruptedException e ) {
                assert false : "if interrupted, shouldn't it be cancelled??";
                Thread.currentThread().interrupt();
                value = null;
                success = false;
            }
            catch ( ExecutionException e ) {
                value = null;
                success = false;
                logger_.log( Level.WARNING,
                             "Error in worker thread: " + e.getCause(), e );
            }
        }
        final V value1 = value;
        final boolean success1 = success;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                acceptValue( value1, success1 );
            }
        } );
    }

    /**
     * Callback method invoked on the event dispatch thread when
     * the computation has completed (with or without success).
     * If success is false, value is bound to be null.
     *
     * @param   value   result of computation;
     *                  in case of failure (success=false) this will be null
     * @param   success   true iff the computation completed without error
     *                    or cancellation
     */
    protected abstract void acceptValue( V value, boolean success );
}
