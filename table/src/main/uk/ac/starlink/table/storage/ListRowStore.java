package uk.ac.starlink.table.storage;

import java.io.IOException;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;

/**
 * Implementation of RowStore which stores data in memory.
 * The current implementation uses a
 * {@link uk.ac.starlink.table.RowListStarTable}.
 * This is better-behaved than the RowStore contract requires; it
 * is guaranteed to be able to store any StarTable object
 * ({@link #acceptMetadata} will not throw a <code>TableFormatException</code>).
 * Cautious users of this class will note the fact that for large tables,
 * {@link #acceptRow} may throw an <code>OutOfMemoryError</code>.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
public class ListRowStore implements RowStore {

    RowListStarTable store_;
    boolean ready_;

    public void acceptMetadata( StarTable meta ) {
        if ( store_ != null ) {
            throw new IllegalStateException( "Metadata already sumitted" );
        }
        store_ = new RowListStarTable( meta );
    }

    /**
     * Accepts a row.
     *
     * @throws   OutOfMemoryError  if it's run out of memory
     */
    public void acceptRow( Object[] row ) throws IOException {
        if ( store_ == null ) {
            throw new IllegalStateException( "acceptMetadata not yet called" );
        }
        if ( ready_ ) {
            throw new IllegalStateException( "endRows has been called" );
        }
        store_.addRow( row.clone() );
    }

    public void endRows() {
        ready_ = true;
    }

    public StarTable getStarTable() {
        if ( ready_ ) {
            return store_;
        }
        else {
            throw new IllegalStateException( "endRows not called yet" );
        }
    }
}
