package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Supplier;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StoragePolicy;

/**
 * ColumnFactory that stores column data in ByteStores.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2019
 */
public class ByteStoreColumnFactory implements CachedColumnFactory {

    private final Supplier<ByteStore> byteStoreSupplier_;

    /**
     * Constructs a ByteStoreColumnFactory based on a StoragePolicy.
     *
     * @param  storage  storage policy
     */
    public ByteStoreColumnFactory( StoragePolicy storage ) {
        this( () -> storage.makeByteStore() );
    }

    /**
     * Constructs a ByteStoreColumnFactory based on a ByteStore supplier.
     *
     * @param  byteStoreSupplier  supplier
     */
    public ByteStoreColumnFactory( Supplier<ByteStore> byteStoreSupplier ) {
        byteStoreSupplier_ = byteStoreSupplier;
    }

    public CachedColumn createColumn( StorageType type, long nrow ) {
        return ColumnStorage.getStorage( type )
                            .createColumn( byteStoreSupplier_ );
    }
}
