package uk.ac.starlink.table;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Streaming <code>RowPipe</code> implementation which provides a one-shot
 * table.  
 * The returned table is unusual in that it
 * can only return a <code>RowSequence</code> once.
 * This violates the normal rules of the <code>StarTable</code> interface.
 * Any calls beyond the first to
 * <code>waitForStarTable().getRowSequence()</code> 
 * will throw a {@link uk.ac.starlink.table.UnrepeatableSequenceException}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Feb 2005
 */
public class OnceRowPipe implements RowPipe, RowSequence {

    private final LinkedList<Object[]> rowQueue_;
    private final int queueSize_;
    private StarTable table_;
    private Object[] seqRow_;
    private boolean seqClosed_;
    private boolean seqEnded_;
    private volatile IOException error_;

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
     *         write and read before <code>acceptRow</code> will block
     */
    public OnceRowPipe( int queueSize ) {
        queueSize_ = queueSize;
        rowQueue_ = new LinkedList<Object[]>();
    }

    public synchronized void setError( IOException error ) {
        rowQueue_.clear();
        if ( error_ == null ) {
            error_ = error;
            notifyAll();
        }
    }

    public synchronized void acceptMetadata( StarTable meta ) {
        table_ = new WrapperStarTable( meta ) {
            RowSequence rseq_ = OnceRowPipe.this;
            public boolean isRandom() {
                return false;
            }
            public RowAccess getRowAccess() {
                throw new UnsupportedOperationException();
            }
            public RowSplittable getRowSplittable() throws IOException {
                return Tables.getDefaultRowSplittable( this );
            }
            public RowSequence getRowSequence() throws IOException {
                synchronized ( OnceRowPipe.this ) {
                    if ( rseq_ == null ) {
                        throw new UnrepeatableSequenceException(
                                      "Can't re-read data from stream");
                    }
                    else {
                        checkError();
                        RowSequence rseq = rseq_;
                        rseq_ = null;
                        return new WrapperRowSequence( rseq ) {
                            public boolean next() throws IOException {
                                checkError();
                                return super.next();
                            }
                            public Object getCell( int icol )
                                    throws IOException {
                                checkError();
                                return super.getCell( icol );
                            }
                            public Object[] getRow() throws IOException {
                                checkError();
                                return super.getRow();
                            }
                            public void close() throws IOException {
                                checkError();
                                super.close();
                            }
                        };
                    }
                }
            }
        };
    }

    public synchronized void acceptRow( Object[] row ) throws IOException {
        if ( seqClosed_ ) {
            throw new IOException( "Stream closed at reading end" );
        }
        checkError();
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
     * <code>getRowSequence</code> will return a sequence that steps through
     * the same rows which are being written to this sink.
     * The <code>getRowSequence</code> method can only be called once;
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
        checkError();
        return table_;
    }

    public synchronized boolean next() throws IOException {
        checkError();
        if ( seqEnded_ ) {
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
        seqRow_ = rowQueue_.removeFirst();
        if ( seqRow_ == END_ROWS ) {
            seqEnded_ = true;
        }
       
        notifyAll();
        return ! seqEnded_;
    }

    public synchronized Object[] getRow() {
        if ( seqRow_ != null ) {
            return seqRow_;
        }
        else {
            throw new IllegalStateException( "next() has not been called yet" );
        }
    }

    public Object getCell( int icol ) {
        return getRow()[ icol ];
    }

    public synchronized void close() {
        seqClosed_ = true;
    }

    /**
     * Throws an IOException if there is one pending.
     */
    private synchronized void checkError() throws IOException {
        if ( error_ != null ) {
            String msg = error_.getMessage();
            if ( msg == null || msg.length() == 0 ) {
                msg = error_.toString();
            }
            throw (IOException) new IOException( msg ).initCause( error_ );
        }
    }
}
