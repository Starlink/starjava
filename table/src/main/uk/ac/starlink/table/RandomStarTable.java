package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Partial implementation of the <tt>StarTable</tt> interface based 
 * on a random access data model.
 * Concrete subclasses may provide data access by implementing only the 
 * {@link #doGetRow} method, in terms of which the <tt>getRow</tt>
 * and <tt>getCell</tt> methods are implemented, but may wish to 
 * override the <tt>getCell</tt> methods as well if they can be
 * implemented more efficiently than by reading a whole row.
 * The {@link #getRowCount} method must be implemented with a known
 * table length as well.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class RandomStarTable extends AbstractStarTable {

    private long current = -1L;
    private long cachedRowIndex;
    private Object[] cachedRowData;

    /**
     * Returns true.
     *
     * @return  true
     */
    public boolean isRandom() {
        return true;
    }

    public boolean hasNext() {
        return current < getCheckedRowCount() - 1L;
    }

    public void setCurrent( long irow ) {
        long rc = getCheckedRowCount();
        if ( irow < -1L || irow >= rc ) {
            throw new IllegalArgumentException( 
                "Row " + irow + " out of range (-1.." + rc + ")" );
        }
        current = irow;
    }

    public long getCurrent() {
        return current;
    }

    public void next() {
        if ( hasNext() ) {
            current++;
        }
        else {
            throw new IllegalStateException( "End of table has been reached" );
        }
    }

    public void advanceCurrent( long offset ) {
        long rc = getCheckedRowCount();
        if ( current + offset < rc ) {
            current += offset;
        }
        else {
            throw new IllegalArgumentException(
                "Attempt to advance beyond end of table (" +
                current + " + " + offset + " > " + rc + ")" );
        }
    }

    /**
     * Invokes {@link #getRow(long)} on the current row.
     */
    public Object[] getRow() throws IOException {
        return getRow( current );
    }

    /**
     * Invokes {@link #doGetRow} unless the last row read was <tt>irow</tt>,
     * in which case it uses that.
     */
    public Object[] getRow( long irow ) throws IOException {
        setCurrent( irow );
        assert current == irow;
        if ( cachedRowIndex != current || cachedRowData == null ) {
            cachedRowData = doGetRow( current );
            cachedRowIndex = current;
        }
        return cachedRowData;
    }

    /**
     * Invokes <tt>getRow[icol]</tt>.
     */
    public Object getCell( int icol ) throws IOException {
        return getRow()[ icol ];
    }

    /**
     * Invokes <tt>getRow(irow)[icol]</tt>.
     */
    public Object getCell( long irow, int icol ) throws IOException {
        return getRow( irow )[ icol ];
    }
    
    private long getCheckedRowCount() {
        long rc = getRowCount();
        if ( rc < 0L ) {
            throw new IllegalStateException(
                this + ".getRowCount() illegally returns negative value " +
                "(see RandomStarTable.getRowCount())" );
        }
        return rc;
    }

    /**
     * Subclasses must implement this method to return a positive number
     * indicating how many rows this table has.  It is used by 
     * <tt>RandomStarTable</tt>'s implementations of <tt>hasNext</tt>
     * and the row positioning methods.
     *
     * @return  the number of rows in this table (not -1)
     */
    abstract public long getRowCount();

    /**
     * Returns the contents of a given row of the table, in the same format
     * as the result of the {@link #getRow} method.
     *
     * @return  the objects in the column of the next row as an array
     * @throws  IOException  if some I/O error occurs
     */
    abstract protected Object[] doGetRow( long irow ) throws IOException;
}
