package uk.ac.starlink.table.storage;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;

/**
 * Wrapper storage policy which derives its functionality from an
 * existing ("base") policy, but additionally passes row storage events
 * to a supplied TableSink.
 *
 * @author   Mark Taylor
 * @since    24 Aug 2010
 * @see      uk.ac.starlink.table.gui.ProgressBarTableSink
 */
public class MonitorStoragePolicy extends StoragePolicy {

    private final StoragePolicy base_;
    private final TableSink sink_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table" );

    /**
     * Constructor.
     *
     * @param  base  base policy
     * @param  sink  recipient for row storage events associated with this
     *               policy
     */
    public MonitorStoragePolicy( StoragePolicy base, TableSink sink ) {
        base_ = base;
        sink_ = sink;
    }

    /**
     * Returns the base storage policy.
     *
     * @return  base policy
     */
    public StoragePolicy getBasePolicy() {
        return base_;
    }

    public RowStore makeRowStore() {
        return new TeeRowStore( base_.makeRowStore(), sink_ );
    }

    public RowStore makeConfiguredRowStore( StarTable meta ) {
        RowStore store =
            new TeeRowStore( base_.makeConfiguredRowStore( meta ), sink_ );
        try {
            sink_.acceptMetadata( meta );
        }
        catch ( TableFormatException e ) {
            logger_.warning( "Table monitor failed: " + e );
        }
        return store;
    }

    public ByteStore makeByteStore() {
        return base_.makeByteStore();
    }

    /**
     * RowStore implementation which wraps an existing row store but
     * additionally messages a second sink with row storage events.
     */
    private static class TeeRowStore implements RowStore {

        private final RowStore baseStore_;
        private final TableSink sink_;
        private long irow_;

        /**
         * Constructor.
         *
         * @param  base  base row store
         * @param  sink  additional sink
         */
        TeeRowStore( RowStore base, TableSink sink ) {
            baseStore_ = base;
            sink_ = sink;
        }

        public void acceptMetadata( StarTable meta )
                throws TableFormatException {
            baseStore_.acceptMetadata( meta );
            sink_.acceptMetadata( meta );
        }

        public void acceptRow( Object[] row ) throws IOException {
            baseStore_.acceptRow( row );
            sink_.acceptRow( row );
        }

        public void endRows() throws IOException {
            baseStore_.endRows();
            sink_.endRows();
        }

        public StarTable getStarTable() {
            return baseStore_.getStarTable();
        }
    }
}
