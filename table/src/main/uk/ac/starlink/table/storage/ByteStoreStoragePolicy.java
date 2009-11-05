package uk.ac.starlink.table.storage;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;

/**
 * Abstract StoragePolicy implementation based on a ByteStore.
 * The {@link #attemptMakeByteStore} method must be implemented,
 * and the other methods are implemented in terms of that.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2009
 */
public abstract class ByteStoreStoragePolicy extends StoragePolicy {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Returns a ByteStore object to hold data.
     * If an exception is thrown, this implementation will fall back
     * to memory-based techniques.
     *
     * @return  new byte store
     */
    protected abstract ByteStore attemptMakeByteStore() throws IOException;
   
    public ByteStore makeByteStore() {
        Throwable error;
        try {
            return attemptMakeByteStore();
        }
        catch ( IOException e ) {
            error = e;
        }
        catch ( SecurityException e ) {
            error = e;
        }
        catch ( OutOfMemoryError e ) {
            error = e;
        }
        assert error != null;
        logger_.warning( "Failed to create byte storage: " + error
                       + " - using memory instead" );
        return new MemoryByteStore();
    }

    public RowStore makeRowStore() {
        return new ByteStoreRowStore( makeByteStore() );
    }

    public RowStore makeConfiguredRowStore( StarTable meta ) {
        RowStore store = makeRowStore();
        try {
            store.acceptMetadata( meta );
            return store;
        }
        catch ( TableFormatException e ) {
            logger_.warning( "Row store " + store + " unsuitable for table: " 
                           + e + " - using memory instead" );
            ListRowStore memStore = new ListRowStore();
            memStore.acceptMetadata( meta );
            return memStore;
        }
    }
}
