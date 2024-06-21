package uk.ac.starlink.table;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Alternative implementation of OnceRowPipe.
 *
 * <p>This should provide identical behaviour to {@link OnceRowPipe},
 * but it uses <code>java.util.concurrent</code> classes rather than
 * doing everything by hand with java language primitives
 * (synchronized/wait/notify).  This implementation has not been much
 * tested, but I think it looks more respectable.
 * It may have different bugs to OnceRowPipe.
 *
 * <p>Streaming <code>RowPipe</code> implementation which provides a one-shot
 * table.  
 * The returned table is unusual in that it
 * can only return a <code>RowSequence</code> once.
 * This violates the normal rules of the <code>StarTable</code> interface.
 * Any calls beyond the first to
 * <code>waitForStarTable().getRowSequence()</code>
 * will throw a {@link uk.ac.starlink.table.UnrepeatableSequenceException}.
 *  
 * @author   Mark Taylor (Starlink)
 * @since    14 Nov 2014 
 */         
class OnceRowPipe2 implements RowPipe, RowSequence {

    private final BlockingQueue<Object[]> rowQueue_;
    private final CountDownLatch tableLatch_;
    private StarTable table_;
    private volatile IOException error_;
    private volatile boolean seqClosed_;
    private Object[] seqRow_;

    private static final Object[] END_ROWS = new Object[ 0 ];

    /**
     * Constructs a new streaming row store with a default buffer size.
     */
    public OnceRowPipe2() {
        this( 1024 );
    }

    /**
     * Constructs a new streaming row store with a given buffer size.
     *
     * @param  queueSize  the maximum number of rows buffered between
     *         write and read before <code>acceptRow</code> will block
     */
    public OnceRowPipe2( int queueSize ) {
        rowQueue_ = new LinkedBlockingQueue<Object[]>( queueSize );
        tableLatch_ = new CountDownLatch( 1 );
    }

    public void setError( IOException error ) {
        rowQueue_.clear();
        if ( error_ == null ) {
            error_ = error;
        }
    }

    public void acceptMetadata( StarTable meta ) {
        table_ = new WrapperStarTable( meta ) {
            RowSequence rseq_ = OnceRowPipe2.this;
            public boolean isRandom() {
                return false;
            }
            public RowAccess getRowAccess() {
                throw new UnsupportedOperationException();
            }
            public RowSplittable getRowSplittable() throws IOException {
                return Tables.getDefaultRowSplittable( this );
            }
            public synchronized RowSequence getRowSequence()
                    throws IOException {
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
        };
        tableLatch_.countDown();
    }

    public void acceptRow( Object[] row ) throws IOException {
        if ( seqClosed_ ) {
            throw new IOException( "Stream closed at reading end" );
        }
        checkError();
        try {
            rowQueue_.put( row );
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException ( "Thread interrupted" )
                               .initCause( e );
        }
    }

    public void endRows() throws IOException {
        try {
            rowQueue_.put( END_ROWS );
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException ( "Thread interrupted" )
                               .initCause( e );
        }
        if ( table_ == null ) {
            setError( new IOException( "No data in table" ) );
        }
        tableLatch_.countDown();
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
    public StarTable waitForStarTable() throws IOException {
        try {
            tableLatch_.await();
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException( "Thread interrupted" )
                               .initCause( e );
        }
        checkError();
        return table_;
    }

    public boolean next() throws IOException {
        checkError();
        if ( seqRow_ != END_ROWS ) {
            try {
                seqRow_ = rowQueue_.take();
            }
            catch ( InterruptedException e ) {
                throw (IOException) new IOException( "Thread interrupted" )
                                   .initCause( e );
            }
        }
        return seqRow_ != END_ROWS;
    }

    public Object[] getRow() {
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

    public void close() {
        seqClosed_ = true;
    }

    /**
     * Throws an IOException if there is one pending.
     */
    private void checkError() throws IOException {
        if ( error_ != null ) {
            String msg = error_.getMessage();
            if ( msg == null || msg.length() == 0 ) {
                msg = error_.toString();
            }
            throw (IOException) new IOException( msg ).initCause( error_ );
        }
    }
}
