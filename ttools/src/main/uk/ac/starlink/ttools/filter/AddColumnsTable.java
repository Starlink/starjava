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

    private final StarTable baseTable_;
    private final ColumnSupplement colSup_;
    private final int ncol_;
    private final boolean[] jtabs_; // false for base, true for supplement
    private final int[] jcols_;     // column index in table indicated by jtabs_

    /**
     * Constructs a table in which the added columns are placed at a
     * given position.
     *
     * @param  baseTable  base table
     * @param  colSup   object supplying columns to be added
     * @param  ipos  column index within the output table at which
     *               the first <code>colSup</code> column should appear
     */
    public AddColumnsTable( StarTable baseTable, ColumnSupplement colSup,
                            int ipos ) {
        super( baseTable );
        baseTable_ = baseTable;
        colSup_ = colSup;
        int nc0 = baseTable_.getColumnCount();
        int nc1 = colSup_.getColumnCount();
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
     * @param  colSup   object supplying columns to be added
     */
    public AddColumnsTable( StarTable baseTable, ColumnSupplement colSup ) {
        this( baseTable, colSup, baseTable.getColumnCount() );
    }

    public int getColumnCount() {
        return jcols_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        int jcol = jcols_[ icol ];
        return jtabs_[ icol ] ? colSup_.getColumnInfo( jcol )
                              : baseTable_.getColumnInfo( jcol );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        int jcol = jcols_[ icol ];
        return jtabs_[ icol ] ? colSup_.getCell( irow, jcol )
                              : baseTable_.getCell( irow, jcol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return combineRows( baseTable_.getRow( irow ), colSup_.getRow( irow ) );
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
        final RowSequence baseSeq = baseTable_.getRowSequence();
        final SupplementSequence supSeq = colSup_.createSequence( baseSeq );
        return new RowSequence() {
            long lrow_ = -1;
            public boolean next() throws IOException {
                if ( baseSeq.next() ) {
                    lrow_++;
                    return true;
                }
                else {
                    return false;
                }
            }
            public Object getCell( int icol ) throws IOException {
                if ( lrow_ >= 0 ) {
                    int jcol = jcols_[ icol ];
                    return jtabs_[ icol ] ? supSeq.getCell( lrow_, jcol )
                                          : baseSeq.getCell( jcol );
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public Object[] getRow() throws IOException {
                return combineRows( baseSeq.getRow(),
                                    supSeq.getRow( lrow_ ) );
            }
            public void close() throws IOException {
                baseSeq.close();
            }
        };
    }
}
