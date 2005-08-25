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

    /**
     * Constructs and populates a new table.
     *
     * @param   nrow  number of rows
     * @param   colDatas  array of ColumnData objects, one for each column.
     *          Each one should be able to provide data for <code>nrows</code>
     *          rows
     */
    public QuickTable( int nrow, ColumnData[] colDatas ) {
        nrow_ = nrow;
        for ( int i = 0; i < colDatas.length; i++ ) {
            addColumn( colDatas[ i ] );
        }
    }

    public long getRowCount() {
        return (long) nrow_;
    }
}
