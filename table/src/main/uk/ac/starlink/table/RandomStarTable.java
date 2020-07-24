package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Implements a StarTable based on row and cell accessor methods
 * that are random access and thread-safe.
 *
 * <p>The <tt>isRandom</tt> method always returns true, and the 
 * <tt>getRowSequence</tt> method is implemented using the table's
 * (abstract) <tt>getCell</tt> and <tt>getRow</tt> methods,
 * which must be safe for concurrent use from multiple threads.
 * This implementation is only suitable where table data access is
 * naturally thread-safe; in cases where synchronization or some
 * other potentially expensive mechanism is used to secure thread-safety
 * of <code>getCell</code>, it is better to use a different implementation
 * with a better <code>getRowSequence</code> implementation.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class RandomStarTable extends AbstractStarTable {

    /**
     * Returns true.
     *
     * @return  true
     */
    public boolean isRandom() {
        return true;
    }

    /**
     * Returns a <tt>RowSequence</tt> object based on the random data
     * access methods of this table.
     *
     * @return  a row iterator
     */
    public RowSequence getRowSequence() {
        final StarTable table = this;
        final long nrow = getRowCount();
        return new RowSequence() {
            long irow_ = -1;
            public boolean next() {
                if ( irow_ < nrow - 1 ) {
                    irow_++;
                    return true;
                }
                else {
                    return false;
                }
            }
            public Object getCell( int icol ) throws IOException {
                if ( irow_ >= 0 ) {
                    return table.getCell( irow_, icol );
                }
                else {
                    throw new IllegalStateException( "No current row" );
                }
            }
            public Object[] getRow() throws IOException {
                if ( irow_ >= 0 ) {
                    return table.getRow( irow_ );
                }
                else {
                    throw new IllegalStateException( "No current row" );
                }
            }
            public void close() {
            }
        };
    }

    /**
     * Returns a <tt>RowAccess</tt> object based on the random data
     * access methods of this table.
     *
     * @return  a row access
     */
    public RowAccess getRowAccess() {
        final StarTable table = RandomStarTable.this;
        return new RowAccess() {
            long irow_ = -1;
            public void setRowIndex( long irow ) {
                irow_ = irow;
            }
            public Object getCell( int icol ) throws IOException {
                return table.getCell( irow_, icol );
            }
            public Object[] getRow() throws IOException {
                return table.getRow( irow_ );
            }
            public void close() {
            }
        };
    }

    /**
     * Implementations must supply a non-negative return value.
     *
     * @return  the number of rows in the table
     */
    abstract public long getRowCount();

    /**
     * Implementations of this method must be safe for concurrent calls
     * from multiple threads.
     */
    abstract public Object getCell( long irow, int icol ) throws IOException;
}
