package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Contains the data from a table in easy to digest form (no IOExceptions).
 * Suitable for holding small tables.
 *
 * @author   Mark Taylor
 * @since    9 Oct 2018
 */
public abstract class TableData {

    private final StarTable table_;

    /**
     * Constructor.
     *
     * @param  table  table
     */
    TableData( StarTable table ) {
        table_ = table;
    }

    /**
     * Returns number of rows.
     *
     * @return  row count, &gt;=0
     */
    public abstract int getRowCount();

    /**
     * Returns the value of a cell.
     *
     * @param  irow  row index
     * @param  icol  column index
     */
    public abstract Object getCell( int irow, int icol );

    /**
     * Returns the content of one column of the table as an array.
     *
     * @param  icol  column index
     * @return   array with one element per table row
     */
    public Object[] getColumn( int icol ) {
        int nrow = getRowCount();
        Object[] col = new Object[ nrow ];
        for ( int ir = 0; ir < nrow; ir++ ) {
            col[ ir ] = getCell( ir, icol );
        }
        return col;
    }

    /**
     * Returns the table on which this object is based.
     *
     * @return  table
     */
    public StarTable getTable() {
        return table_;
    }

    /**
     * Adapts a StarTable to a TableData.  In case of trouble, null is
     * returned and messages are reported as appropriate.
     *
     * @param  reporter  reporter
     * @param  table  input table
     * @return   table data
     */
    public static TableData createTableData( Reporter reporter,
                                             StarTable table ) {
        if ( table == null ) {
            return null;
        }
        else {
            final List<Object[]> rowList = new ArrayList<Object[]>();
            try {
                RowSequence rseq = table.getRowSequence();
                try {
                    while ( rseq.next() ) {
                        rowList.add( rseq.getRow() );
                    }
                }
                finally {
                    rseq.close();
                }
            }
            catch ( IOException e ) {
                reporter.report( FixedCode.F_TIOF,
                                 "Error reading result table", e );
                return null;
            }
            return new TableData( table ) {
                public int getRowCount() {
                    return rowList.size();
                }
                public Object getCell( int irow, int icol ) {
                    return rowList.get( irow )[ icol ];
                }
            };
        }
    }
}
