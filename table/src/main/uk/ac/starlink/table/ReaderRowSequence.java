package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Partial implementation of <tt>RowSequence</tt> suitable for subclassing
 * by classes which can read a row at a time and don't know when the
 * row stream will come to an end.
 * Concrete subclasses have to implement the {@link #readRow} method.
 * They may also want to override {@link #close}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ReaderRowSequence implements RowSequence {

    private Object[] nextRow;
    private Object[] row;
    private IOException pendingException;
    long lrow;

    public ReaderRowSequence() throws IOException {
        nextRow = readRow();
        lrow = -1L;
    }

    /**
     * Acquires the next row of objects from the input stream.
     * When there are no more rows to read this method must return
     * <tt>null</tt>; note it should <em>not</em> throw an
     * <tt>EOFException</tt>.
     * Otherwise it must return an array of objects representing 
     * the row content for the next row.
     *
     * @return  the next row, or <tt>null</tt> if there are no more
     * @throws  IOException if there is trouble reading the data
     */
    protected abstract Object[] readRow() throws IOException;

    public void next() throws IOException {
        if ( ! hasNext() ) {
            throw new IllegalStateException( "No more rows" );
        }
        row = nextRow;
        nextRow = readRow();
        lrow++;
    }

    public boolean hasNext() {
        return nextRow != null;
    }

    public void advance( long nrows ) throws IOException {
        if ( nrows >= 0 ) {
            for ( long irow = 0; irow < nrows; irow++ ) {
                row = nextRow;
                nextRow = readRow();
                lrow++;
                if ( row == null ) {
                    throw new IOException( 
                        "Attempt to read beyond the last row" );
                }
            }
        }
    }

    public Object getCell( int icol ) {
        if ( row == null ) {
            throw new IllegalStateException( "No current row" );
        }
        return row[ icol ];
    }

    public Object[] getRow() {
        if ( row == null ) {
            throw new IllegalStateException( "No current row" );
        }
        return row;
    }

    public long getRowIndex() {
        return lrow;
    }

    /**
     * The <tt>ReaderRowSequence</tt> implementation does nothing.
     */
    public void close() throws IOException {
    }

}
