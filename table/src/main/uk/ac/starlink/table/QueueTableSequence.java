package uk.ac.starlink.table;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * TableSequence implementation for concurrent use.
 * Table load successes or failures may be added on one thread, and the
 * sequence used from another.  The <code>nextTable</code> method will
 * block until a result is available.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2010
 */
public class QueueTableSequence implements TableSequence {

    private final List queue_;

    /**
     * Constructor.
     */
    public QueueTableSequence() {
        queue_ = new LinkedList();
    }

    /**
     * Adds a table to the queue.
     * This will appear in the TableSequence as a table loadSuccess.
     *
     * @param  table  successfully loaded table
     */
    public synchronized void addTable( StarTable table ) {
        queue_.add( table );
        notifyAll();
    }

    /**
     * Adds a load error to the queue.
     * This will appear in the TableSequence as a table loadFailure.
     *
     * @param  error  reason for unsuccessful table load attempt
     */
    public synchronized void addError( Throwable error ) {
        queue_.add( error );
        notifyAll();
    }

    /**
     * Indicates that no more load success or failure indications will be
     * added by the queue writer.  Must be called, otherwise the reader
     * will never terminate.
     */
    public synchronized void endSequence() {
        queue_.add( null );
        notifyAll();
    }

    public synchronized StarTable nextTable() throws IOException {
        try {
            while ( queue_.isEmpty() ) {
                wait();
            }
            if ( queue_.get( 0 ) == null ) {
                return null;
            }
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException( "Interrupted" ).initCause( e );
        }
        Object obj = queue_.remove( 0 );

        if ( obj instanceof StarTable ) {
            return (StarTable) obj;
        }
        else if ( obj instanceof Throwable ) {
            Throwable err = (Throwable) obj;
            throw (IOException)
                  new IOException( err.getMessage() ).initCause( err );
        }
        else {
            throw new AssertionError();
        }
    }
}
