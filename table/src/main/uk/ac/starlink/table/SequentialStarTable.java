package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Partial implementation of the <tt>StarTable</tt> interface based
 * on a sequential stream of table data.
 * Concrete subclasses need implement only the abstract 
 * {@link #getNextRow} and {@link #hasNextRow} methods, though they
 * may override others if they can do so more efficiently than the
 * implementations described here.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class SequentialStarTable extends AbstractStarTable {

    private long current = -1L;
    private Object[] currentRowData;

    /**
     * Returns -1.  Subclasses should override this if they know how many
     * rows they have.
     *
     * @return  -1
     */
    public long getRowCount() {
        return -1L;
    }

    /**
     * Returns <tt>false</tt>.
     *
     * @return <tt>false</tt>
     */
    public boolean isRandom() {
        return false;
    }

    public long getCurrent() {
        return current;
    }

    /**
     * Advances the current row by repeated invocations of {@link #next}.
     *
     * @throws  IOException if <tt>offset&lt;0</tt>
     */
    public void advanceCurrent( long offset ) throws IOException {
        if ( offset < 0 ) {
            throw new IOException( "Random access not supported" );
        }
        assert offset >= 0;

        for ( long i = 0; i < offset; i++ ) {
            next();
        }
    }

    /**
     * Invokes <tt>advanceCurrent(irow-getCurrent())</tt>.
     */
    public void setCurrent( long irow ) throws IOException {
        advanceCurrent( irow - getCurrent() );
    }

    /**
     * Invokes {@link #getNextRow} and sets the current row from its return
     * value.
     */
    public void next() throws IOException {
        if ( hasNextRow() ) {
            currentRowData = getNextRow();
        }
        else {
            currentRowData = null;
            throw new IllegalStateException( "End of table has been reached" );
        }
    }

    /**
     * Invokes {@link #hasNextRow}.
     */
    public boolean hasNext() {
        return hasNextRow();
    }

    /**
     * Returns the requested cell from the current row.
     */
    public Object getCell( int icol ) throws IOException {
        if ( currentRowData == null ) {
            throw new IllegalStateException();
        }
        return currentRowData[ icol ];
    }

    /**
     * Returns the current row.
     */
    public Object[] getRow() throws IOException {
        if ( currentRowData == null ) {
            throw new IllegalStateException();
        }
        return currentRowData;
    }

    /**
     * Calls <tt>setCurrent(irow)</tt> followed by <tt>getCell(icol)</tt>.
     */
    public Object getCell( long irow, int icol ) throws IOException {
        setCurrent( irow );
        return getCell( icol );
    }

    /**
     * Calls <tt>setCurrent(irow)</tt> followed by <tt>getRow</tt>.
     */
    public Object[] getRow( long irow ) throws IOException {
        setCurrent( irow );
        return getRow();
    }

    /**
     * Returns the next row of the table, in the same format as the
     * result of the {@link #getRow} method.
     * This will only be invoked if {@link #hasNextRow} returns <tt>true</tt>
     *
     * @return  the objects in the column of the next row as an array
     * @throws  IOException  if some I/O error occurs
     */
    abstract protected Object[] getNextRow() throws IOException;

    /**
     * Indicates whether there are any more rows to be read using
     * {@link #getNextRow}.
     */
    abstract protected boolean hasNextRow();

}
