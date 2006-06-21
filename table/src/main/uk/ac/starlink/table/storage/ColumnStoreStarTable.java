package uk.ac.starlink.table.storage;

import java.io.IOException;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * StarTable implementation which retrieves its data from 
 * {@link ColumnStore} objects.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
class ColumnStoreStarTable extends WrapperStarTable {

    private final long nrow_;
    private final int ncol_;
    private final ColumnStore[] colStores_;

    /**
     * Constructor.
     *
     * @param  template   template table supplying metadata
     * @param  nrow       number of rows in this table
     * @param  colStores  array of ColumnStore objects, one for each 
     *                    column in the table
     */
    public ColumnStoreStarTable( StarTable template, long nrow,
                                 ColumnStore[] colStores ) {
        super( template );
        nrow_ = nrow;
        ncol_ = template.getColumnCount();
        colStores_ = colStores;
    }

    public boolean isRandom() {
        return true;
    }

    public long getRowCount() {
        return nrow_;
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        return colStores_[ icol ].readCell( lrow );
    }

    public Object[] getRow( long lrow ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = getCell( lrow, icol );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        return new RandomRowSequence( this );
    }
}
