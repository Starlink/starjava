package uk.ac.starlink.table;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Partial implementation of <code>RowSequence</code> suitable for subclassing
 * by classes which can read a row at a time and don't know when the
 * row stream will come to an end.
 * Concrete subclasses have to implement the {@link #readRow} method.
 * They may also want to override {@link #close}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ReaderRowSequence implements RowSequence {

    private Object[] row_;
    private boolean done_;

    /**
     * Acquires the next row of objects from the input stream.
     * When there are no more rows to read this method must return
     * <code>null</code>; note it should <em>not</em> throw an
     * exception to indicate this fact.
     * Otherwise it must return an array of objects representing 
     * the row content for the next row.
     *
     * @return  the next row, or <code>null</code> if there are no more
     * @throws  IOException if there is trouble reading the data
     */
    protected abstract Object[] readRow() throws IOException;

    public boolean next() throws IOException {
        if ( ! done_ ) {
            row_ = readRow();
            done_ = ( row_ == null );
        }
        return ! done_;
    }

    public Object getCell( int icol ) {
        if ( row_ == null ) {
            throw new IllegalStateException( "No current row" );
        }
        return row_[ icol ];
    }

    public Object[] getRow() {
        if ( row_ == null ) {
            throw new IllegalStateException( "No current row" );
        }
        return row_;
    }

    /**
     * The <code>ReaderRowSequence</code> implementation does nothing.
     */
    public void close() throws IOException {
    }

}
