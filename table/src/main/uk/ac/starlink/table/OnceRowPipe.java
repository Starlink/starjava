package uk.ac.starlink.table;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Streaming <code>RowPipe</code> implementation which provides a one-shot
 * table.  
 * The returned table is unusual in that it
 * can only return a <code>RowSequence</code> once.
 * This violates the normal rules of the <code>StarTable</code> interface.
 * Any calls beyond the first to <tt>waitForStarTable().getRowSequence()</tt> 
 * will throw a {@link uk.ac.starlink.table.UnrepeatableSequenceException}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Feb 2005
 */
public class OnceRowPipe implements RowPipe, RowSequence {

    private final LinkedList rowQueue_;
    private final int queueSize_;
    private StarTable table_;
    private Object[] seqRow_;
    private boolean seqClosed_;
    private boolean seqEnded_;
    private IOException error_;

    private static final Object[] END_ROWS = new Object[ 0 ];

    /**
     * Constructs a new streaming row store with a default buffer size.
     */
    public OnceRowPipe() {
        this( 1024 );
    }

    /**
     * Constructs a new streaming row store with a given buffer size.
     *
     * @param  queueSize  the maximum number of rows buffered between 
     *         write and read before <tt>acceptRow</tt> will block
     */
    public OnceRowPipe( int queueSize ) {
        queueSize_ = queueSize;
        rowQueue_ = new LinkedList();
    }

    public synchronized void setError( IOException error ) {
        error_ = error;
        notifyAll();
    }

    public synchronized void acceptMetadata( StarTable meta ) {
        table_ = new WrapperStarTable( meta ) {
            RowSequence rseq_ = OnceRowPipe.this;
            public boolean isRandom() {
                return false;
            }
            public RowSequence getRowSequence() throws IOException {
                synchronized ( OnceRowPipe.this ) {
                    if ( rseq_ == null ) {
                        throw new UnrepeatableSequenceException(
                                      "Can't re-read data from stream");
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
        if ( table_ == null ) {
            setError( new IOException( "No data in table" ) );
        }
        notifyAll();
    }

    /**
     * Returns a non-random table whose first call to 
     * <tt>getRowSequence</tt> will return a sequence that steps through
     * the same rows which are being written to this sink.
     * The <tt>getRowSequence</tt> method can only be called once;
     * any subsequent attempts to call it will result in a
     * {@link UnrepeatableSequenceException}.
     * This method will block until {@link #acceptMetadata} has been called.
     *
     * @return   one-shot streaming sequential table
     * @throws   IOException  if one has previously been set using 
     *           {@link #setError}
     */
    public synchronized StarTable waitForStarTable() throws IOException {
        try {
            while ( table_ == null && error_ == null ) {
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
        else if ( seqEnded_ ) {
            notifyAll();
            return false;
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
        if ( seqRow_ == END_ROWS ) {
            seqEnded_ = true;
        }
       
        notifyAll();
        return ! seqEnded_;
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
