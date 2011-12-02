package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table which adds another table to it by placing all the
 * columns of the added table together with the existing columns.
 * The added columns may be placed anywhere, but they stay together.
 * Table metadata is just that of the base table.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2011
 */
public class AddColumnsTable extends WrapperStarTable {

    private final StarTable table0_;
    private final StarTable table1_;
    private final int ncol_;
    private final boolean[] jtabs_; // false for t0 (base), true for t1 (added)
    private final int[] jcols_;     // column index in table indicated by jtabs_

    /**
     * Constructs a table in which the added columns are placed at a
     * given position.
     *
     * @param  baseTable  base table
     * @param  addTable  table whose columns are to be added
     * @param  ipos  column index within the output table at which
     *               the first <code>addTable</code> column should appear
     */
    public AddColumnsTable( StarTable baseTable, StarTable addTable,
                            int ipos ) {
        super( baseTable );
        table0_ = baseTable;
        table1_ = addTable;
        int nc0 = table0_.getColumnCount();
        int nc1 = table1_.getColumnCount();
        ncol_ = nc0 + nc1;

        /* Store a lookup table for where to find output columns. */
        jtabs_ = new boolean[ ncol_ ];
        jcols_ = new int[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            int ic1 = ic - ipos;
            final int jcol;
            final boolean jtab;
            if ( ic1 < 0 ) {
                jtab = false;
                jcol = ic;
            }
            else if ( ic1 >= nc1 ) {
                jtab = false;
                jcol = ic - nc1;
            }
            else {
                jtab = true;
                jcol = ic1;
            }
            jcols_[ ic ] = jcol;
            jtabs_[ ic ] = jtab;
        }
    }

    /**
     * Constructs a table in which the added columns come after all the
     * columns of the base table.
     *
     * @param   baseTable   base table
     * @param  addTable  table whose columns are to be added
     */
    public AddColumnsTable( StarTable baseTable, StarTable addTable ) {
        this( baseTable, addTable, baseTable.getColumnCount() );
    }

    public int getColumnCount() {
        return jcols_.length;
    }

    public long getRowCount() {
        return Math.min( table0_.getRowCount(), table1_.getRowCount() );
    }

    public boolean isRandom() {
        return table0_.isRandom() && table1_.isRandom();
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return ( jtabs_[ icol ] ? table1_ : table0_ )
              .getColumnInfo( jcols_[ icol ] );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return ( jtabs_[ icol ] ? table1_ : table0_ )
              .getCell( irow, jcols_[ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        return combineRows( table0_.getRow( irow ), table1_.getRow( irow ) );
    }

    /**
     * Takes corresponding rows from the two input tables and
     * produces a row for this table.
     *
     * @param  row0  base table row
     * @param  row1  added table row
     * @return  output row
     */
    private Object[] combineRows( Object[] row0, Object[] row1 ) {
        assert row0.length + row1.length == ncol_;
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = ( jtabs_[ icol ] ? row1 : row0 )[ jcols_[ icol ] ];
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        final RowSequence rseq0 = table0_.getRowSequence();
        final RowSequence rseq1 = table1_.getRowSequence();
        return new RowSequence() {
            public boolean next() throws IOException {
                return rseq0.next() && rseq1.next();
            }
            public Object getCell( int icol ) throws IOException {
                return ( jtabs_[ icol ] ? rseq1 : rseq0 )
                      .getCell( jcols_[ icol ] );
            }
            public Object[] getRow() throws IOException {
                return combineRows( rseq0.getRow(), rseq1.getRow() );
            }
            public void close() throws IOException {
                rseq0.close();
                rseq1.close();
            }
        };
    }
}
