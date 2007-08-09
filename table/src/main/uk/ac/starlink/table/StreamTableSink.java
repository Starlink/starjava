package uk.ac.starlink.table;

import java.io.IOException;
import java.io.OutputStream;

/**
 * TableSink which turns its accepted data into a one-pass table and 
 * does something with it (calls {@link #scanTable} on it).  
 * This effectively allows you to 'push' table data by writing it to this 
 * TableSink rather than 'pulling' it by implementing a
 * {@link uk.ac.starlink.table.RowSequence}; the latter may be a less
 * obvious or more fiddly way to work.
 * Note however this will only work for <code>scanTable</code> implementations
 * which need only a single pass of the data.
 *
 * <p>See {@link uk.ac.starlink.table.TableSink} for usage rules.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2007
 */
public abstract class StreamTableSink implements TableSink {

    private RowPipe rowPipe_;
    private Thread writerThread_;
    private Throwable writerError_;

    /**
     * Does something or other with the table whose data is being pushed
     * into this sink.  The only data access which will work is a single
     * call to {@link uk.ac.starlink.table#getRowSequence()}.  Subsequent
     * <code>getRowSequence</code> calls will result in a 
     * {@link uk.ac.starlink.table.UnrepeatableSequenceException}.
     * This method is called in a separate thread than the one in which
     * the <code>accept*</code> calls are made; but any exceptions thrown
     * here are passed back to the latter thread.
     *
     * @param  table    table to consume
     */
    protected abstract void scanTable( StarTable table ) throws IOException;

    public void acceptMetadata( StarTable meta ) throws TableFormatException {
        rowPipe_ = new OnceRowPipe();
        rowPipe_.acceptMetadata( meta );
        final StarTable outTable;
        try {
            outTable = rowPipe_.waitForStarTable();
        }
        catch ( IOException e ) {
            throw new AssertionError( e );
        }
        writerThread_ = new Thread( "Table Writer" ) {
            public void run() {
                try {
                    scanTable( outTable );
                }
                catch ( Throwable e ) {
                    writerError_ = e;
                }
            }
        };
        writerThread_.start();
    }

    public void acceptRow( Object[] row ) throws IOException {
        rowPipe_.acceptRow( row );
    }

    public void endRows() throws IOException {
        rowPipe_.endRows();
        try {
            writerThread_.join();
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException( "Thread interrupted" )
                               .initCause( e );
        }
        if ( writerError_ != null ) {
            throw (IOException)
                  new IOException( "Write error: " + writerError_.getMessage() )
                 .initCause( writerError_ );
        }
    }
}
