package uk.ac.starlink.ttools;

import java.io.IOException;
import java.util.LinkedList;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * TableSink implementation whose returned table reads from a stream.
 * The iterator got from <tt>getStarTable().getRowSequence()</tt>
 * will return the rows fed to this object's <tt>acceptRow</tt> method.
 * Some of the methods block, and the reading and writing have to be 
 * done in different threads.
 *
 * <p>The returned table is unusual in that (for obvious reasons) it
 * can only return a RowSequence once.  This violates the normal rules
 * of the StarTable interface.  Any calls beyond the first to
 * <tt>getStarTable().getRowSequence()</tt> will throw a
 * {@link StreamRereadException}.
 *
 * <p>This serves almost the same purpose as a 
 * {@link uk.ac.starlink.table.RowStore}, but does not quite implement
 * that interface.  Instead of RowStore's 
 * {@link uk.ac.starlink.table.RowStore#getStarTable} method, it provides a
 * {@link #waitForStarTable} method.  This blocks until the metadata
 * has been supplied, and also throws an IOException.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Feb 2005
 */
public class StreamRowStore implements TableSink, RowSequence {

    private final LinkedList rowQueue_;
    private final int queueSize_;
    private StarTable table_;
    private Object[] seqRow_;
    private boolean seqClosed_;
    private IOException error_;

    private static final Object[] END_ROWS = new Object[ 0 ];

    /**
     * Constructs a new streaming row store with a default buffer size.
     */
    public StreamRowStore() {
        this( 1024 );
    }

    /**
     * Constructs a new streaming row store with a given buffer size.
     *
     * @param  queueSize  the maximum number of rows buffered between 
     *         write and read before <tt>acceptRow</tt> will block
     */
    public StreamRowStore( int queueSize ) {
        queueSize_ = queueSize;
        rowQueue_ = new LinkedList();
    }

    /**
     * Registers an exception which has taken place in supplying the
     * data to this row store.  This error will be stored and re-thrown
     * from the <tt>next</tt> method of the row sequence next time it
     * is called.
     *
     * @param  error  stored exception
     */
    public synchronized void setError( IOException error ) {
        error_ = error;
        notifyAll();
    }

    public synchronized void acceptMetadata( StarTable meta ) {
        table_ = new WrapperStarTable( meta ) {
            RowSequence rseq_ = StreamRowStore.this;
            public boolean isRandom() {
                return false;
            }
            public RowSequence getRowSequence() throws IOException {
                synchronized ( StreamRowStore.this ) {
                    if ( rseq_ == null ) {
                        throw new StreamRereadException();
                    }
                    else if ( error_ != null ) {
                        throw error_;
                    }
                    else {
                        RowSequence rseq = rseq_;
                        rseq_ = null;
                        return rseq;
                    }
                }
            }
        };
    }

    public synchronized void acceptRow( Object[] row ) throws IOException {
        if ( seqClosed_ ) {
            throw new IOException( "Stream closed at reading end" );
        }
        try {
            while ( rowQueue_.size() > queueSize_ ) {
                wait();
            }
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException ( "Thread interrupted" )
                               .initCause( e );
        }
        rowQueue_.addLast( row );
        notifyAll();
    }

    public synchronized void endRows() {
        rowQueue_.addLast( END_ROWS );
        notifyAll();
    }

    /**
     * Returns a non-random table whose first call to 
     * <tt>getRowSequence</tt> will return a sequence that steps through
     * the same rows which are being written to this sink.
     * The <tt>getRowSequence</tt> method can only be called once;
     * any subsequent attempts to call it will result in a
     * {@link StreamRereadException}.
     * This method will block until {@link #acceptMetadata} has been called.
     *
     * @return   one-shot streaming sequential table
     */
    public synchronized StarTable waitForStarTable() throws IOException {
        try {
            while ( table_ == null && error_ == null) {
                wait();
            }
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( "Thread interrupted", e );
        }
        if ( error_ != null ) {
            throw error_;
        }
        else {
            return table_;
        }
    }

    public synchronized boolean next() throws IOException {
        if ( error_ != null ) {
            throw error_;
        }
        try {
            while ( rowQueue_.size() == 0 ) {
                wait();
            }
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException( "Thread interrupted" )
                               .initCause( e );
        }
        seqRow_ = (Object[]) rowQueue_.removeFirst();
        notifyAll();
        return seqRow_ != END_ROWS;
    }

    public synchronized Object[] getRow() {
        return seqRow_;
    }

    public Object getCell( int icol ) {
        return getRow()[ icol ];
    }

    public synchronized void close() {
        seqClosed_ = true;
    }

}
