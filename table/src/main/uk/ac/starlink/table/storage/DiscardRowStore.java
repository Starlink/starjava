package uk.ac.starlink.table.storage;

import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;

/**
 * Minimal implementation of <code>RowStore</code> which throws away the row
 * data it is given.  The table returned by {@link #getStarTable} has the
 * same metadata as the one passed in by the {@link #acceptMetadata},
 * but no rows.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Aug 2004
 */
public class DiscardRowStore implements RowStore {

    private StarTable table_;

    public void acceptMetadata( StarTable meta ) {
        table_ = new EmptyStarTable( meta );
    }

    public void acceptRow( Object[] row ) {
    }

    public void endRows() {
    }

    public StarTable getStarTable() {
        return table_; 
    }
}
