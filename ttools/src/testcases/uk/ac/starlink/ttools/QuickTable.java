package uk.ac.starlink.ttools;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;

/**
 * StarTable implementation that's easy to create and populate.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2005
 */
public class QuickTable extends ColumnStarTable {

    private final int nrow_;
    private final String sval_;

    /**
     * Constructs and populates a new table.
     *
     * @param   nrow  number of rows
     * @param   colDatas  array of ColumnData objects, one for each column.
     *          Each one should be able to provide data for <code>nrows</code>
     *          rows
     */
    public QuickTable( int nrow, ColumnData[] colDatas ) {
        this( nrow, colDatas, null );
    }

    /**
     * Constructs and populates a new table with a given value of the
     * toString function.
     *
     * @param   nrow  number of rows
     * @param   colDatas  array of ColumnData objects, one for each column.
     *          Each one should be able to provide data for <code>nrows</code>
     *          rows
     * @param   sval  return value of toString method
     */
    public QuickTable( int nrow, ColumnData[] colDatas, String sval ) {
        nrow_ = nrow;
        sval_ = sval;
        for ( int i = 0; i < colDatas.length; i++ ) {
            addColumn( colDatas[ i ] );
        }
    }

    public long getRowCount() {
        return (long) nrow_;
    }

    @Override
    public String toString() {
        return sval_ != null ? sval_ : super.toString();
    }
}
