package uk.ac.starlink.topcat.activate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Manages download of data that may be required in multiple places.
 *
 * @author   Mark Taylor
 * @since    24 Oct 2019
 */
public class Downloader<T> {

    private final String dataDescription_;
    private final Callable<T> supplier_;
    private final AtomicReference<Thread> workerRef_;
    private final List<ActionListener> listeners_;
    private volatile T result_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util.gui" );

    /**
     * Constructor.
     *
     * @param   dataDescription  short description of downloaded data,
     *                           may be used in logging messages
     * @param  supplier   supplier of downloaded data
     */
    public Downloader( String dataDescription, Callable<T> supplier ) {
        dataDescription_ = dataDescription;
        supplier_ = supplier;
        workerRef_ = new AtomicReference<Thread>();
        listeners_ = new ArrayList<ActionListener>();
    }

    /**
     * Ensures that this downloader has started to downloading the data.
     * If it has already started (and possibly finished), this has no effect.
     */
    public void start() {
        Thread worker = createWorkerThread();
        if ( workerRef_.compareAndSet( null, worker ) ) {
            worker.start();
        }
    }

    /**
     * Immediately returns the downloaded data, or null if it has not been
     * downloaded, or if a download has failed.
     *
     * @return  downloaded result, or null
     */
    public T getData() {
        return result_;
    }

    /**
     * Adds a listener that will be notified when the data has become
     * available.
     *
     * @param  l  listener
     */
    public void addActionListener( ActionListener l ) {
        listeners_.add( l );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  l  listener
     */
    public void removeActionListener( ActionListener l ) {
        listeners_.remove( l );
    }

    /**
     * Returns a thread that will update the result and inform listeners.
     * The returned thread is not started.
     *
     * @return  thread
     */
    private Thread createWorkerThread() {
        Thread worker = new Thread( dataDescription_ ) {
            @Override
            public void run() {
                final T result;
                try {
                    result = supplier_.call();
                }
                catch ( Throwable e ) {
                    logger_.log( Level.WARNING,
                                 "Failed to read " + dataDescription_, e );
                    return;
                }
                final Thread thr = this;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( workerRef_.get() == thr ) {
                            result_ = result;
                            ActionEvent evt =
                                new ActionEvent( this, 0, "Changed" );
                            for ( ActionListener listener : listeners_ ) {
                                listener.actionPerformed( evt );
                            }
                        }
                    }
                } );
            }
        };
        worker.setDaemon( true );
        return worker;
    }
}
