package uk.ac.starlink.table;

import java.io.IOException;

/**
 * RowSequence implementation based on a RowAccess.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2020
 */
public class AccessRowSequence implements RowSequence {

    private final RowAccess racc_;
    private final long nrow_;
    private long irow_;

    /**
     * Constructor.
     *
     * @param   racc  row access providing data access
     * @param   nrow  number of rows
     */
    public AccessRowSequence( RowAccess racc, long nrow ) {
        racc_ = racc;
        nrow_ = nrow;
        irow_ = -1;
    }

    public boolean next() throws IOException {
        if ( irow_ + 1 < nrow_ ) {
            racc_.setRowIndex( ++irow_ );
            return true;
        }
        else {
            return false;
        }
    }

    public Object getCell( int icol ) throws IOException {
        if ( irow_ >= 0 ) {
            return racc_.getCell( icol );
        }
        else {
            throw new IllegalStateException();
        }
    }

    public Object[] getRow() throws IOException {
        if ( irow_ >= 0 ) {
            return racc_.getRow();
        }
        else {
            throw new IllegalStateException();
        }
    }

    public void close() throws IOException {
        racc_.close();
    }

    /**
     * Constructs an AccessRowSequence based on a given table.
     *
     * @param   table  table
     * @return   new row access
     */
    public static AccessRowSequence createInstance( StarTable table )
            throws IOException {
        if ( ! table.isRandom() ) {
            throw new IllegalArgumentException(
                "Table " + table + " is not random access" );
        }
        long nrow = table.getRowCount();
        if ( nrow < 0L ) {
            throw new IllegalArgumentException(
                "Table " + table + " row count unknown " +
                "(getRowCount()=" + nrow + ")" );
        }
        return new AccessRowSequence( table.getRowAccess(), nrow );
    }
}
