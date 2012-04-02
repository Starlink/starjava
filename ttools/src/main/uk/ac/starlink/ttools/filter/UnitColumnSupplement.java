package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Trivial ColumnSupplement implementation which contains all the
 * columns of a base table unchanged.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2012
 */
public class UnitColumnSupplement implements ColumnSupplement {

    private final StarTable table_;

    /**
     * Constructor.
     *
     * @param   table  base table
     */
    public UnitColumnSupplement( StarTable table ) {
        table_ = table;
    }

    public int getColumnCount() {
        return table_.getColumnCount();
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return table_.getColumnInfo( icol );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return table_.getCell( irow, icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return table_.getRow( irow );
    }

    public SupplementSequence createSequence( final RowSequence rseq )
            throws IOException {
        return new SupplementSequence() {
            public Object getCell( long irow, int icol ) throws IOException {
                return rseq.getCell( icol );
            }
            public Object[] getRow( long irow ) throws IOException {
                return rseq.getRow();
            }
        };
    }
}
