package uk.ac.starlink.table;

import java.io.IOException;
import java.sql.SQLException;
import uk.ac.starlink.table.jdbc.JDBCStarTable;
import uk.ac.starlink.table.storage.ListRowStore;
import uk.ac.starlink.table.storage.DiskRowStore;

/**
 * Defines how bulk data will be stored.
 * If the table handling system needs to cache bulk data somewhere,
 * for instance because it is reading a table from a stream but needs
 * to make it available for random access, it will use a StoragePolicy
 * object to work out how to do it.
 * The selection and behaviour of the default storage policy will
 * depend on system properties, security context, and possibly other things.
 * If you want more control, you can always create instances of the 
 * public {@link RowStore} implementations directly.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class StoragePolicy {

    private static StoragePolicy defaultInstance_;

    /**
     * Name of the system property which can be set to indicate the
     * initial setting of the default storage policy.
     * Currently recognised values are "disk" and "memory";
     */
    private static final String PREF_PROPERTY = "table.storage";

    /**
     * Returns the default storage policy for this JVM.
     *
     * @return   default storage policy
     */
    public static StoragePolicy getDefaultPolicy() {
        if ( defaultInstance_ == null ) {
            String pref = System.getProperty( PREF_PROPERTY );
            if ( "disk".equals( pref ) ) {
                defaultInstance_ = PREFER_DISK;
            }
            else if ( "memory".equals( pref ) ) {
                defaultInstance_ = PREFER_MEMORY;
            }
            else {
                defaultInstance_ = PREFER_MEMORY;
            }
        }
        return defaultInstance_;
    }

    /**
     * Returns a new <tt>RowStore</tt> object which can be used to
     * provide a destination for random-access table storage.
     *
     * @return   a RowStore object
     */
    abstract public RowStore makeRowStore();

    /**
     * Creates a new RowStore and primes it by calling
     * {@link RowStore#acceptMetadata} on it.
     *
     * @param   meta  template giving the metadata which describes the rows
     *          that will have to be stored
     * @return  a RowStore on which <tt>acceptMetadata(meta)</tt> has been
     *          called
     */
    abstract public RowStore makeConfiguredRowStore( StarTable meta );

    /**
     * Returns a table based on a given table and guaranteed to have
     * random access.  If the original table <tt>table</tt> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * @param  table  original table
     * @return  a table with the same data as <tt>table</tt> and with
     *          <tt>isRandom()==true</tt>
     */
    public StarTable randomTable( StarTable table ) throws IOException {

        /* If it's random already, we don't need to do any work. */
        if ( table.isRandom() ) {
            return table;
        }

        /* If it's JDBC we can try to turn it random. */
        if ( table instanceof JDBCStarTable ) {
            try {
                ((JDBCStarTable) table).setRandom();
                return table;
            }
            catch ( SQLException e ) {
                // drop through
            }
            catch ( OutOfMemoryError e ) {
                // drop through
            }
        }

        /* Otherwise get a suitable row store and stream the rows into it. */
        RowStore store = makeConfiguredRowStore( table );
        for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            store.acceptRow( rseq.getRow() );
        }
        store.endRows();

        /* Return the resulting random table. */
        StarTable out = store.getStarTable();
        assert out.isRandom();
        return out;
    }

    /**
     * Storage policy which will always store table data in memory.
     */
    public static final StoragePolicy PREFER_MEMORY = new StoragePolicy() {
        public RowStore makeRowStore() {
            return new ListRowStore();
        }
        public RowStore makeConfiguredRowStore( StarTable meta ) {
            ListRowStore store = new ListRowStore();
            store.acceptMetadata( meta );
            return store;
        }
        public String toString() {
            return "StoragePolicy.PREFER_MEMORY";
        }
    };

    /**
     * Storage policy which will normally store table data in a scratch
     * disk file.  If it's impossible for some reason (I/O error,
     * security restrictions) then it will fall back to using memory.
     * It might also use memory if it thinks it's got a small table 
     * to deal with.
     * Temporary disk files are written in the default temporary 
     * directory, which is the value of the <tt>java.io.tmpdir</tt>
     * system property.
     */
    public static final StoragePolicy PREFER_DISK = new StoragePolicy() {
        public RowStore makeRowStore() {
            try {
                return new DiskRowStore();
            }
            catch ( SecurityException e ) {
                return new ListRowStore();
            }
            catch ( IOException e ) {
                return new ListRowStore();
            }
        }
        public RowStore makeConfiguredRowStore( StarTable meta ) {
            long nrow = meta.getRowCount();
            if ( nrow > 0 && nrow * meta.getColumnCount() < 1000 ) {
                ListRowStore store = new ListRowStore();
                store.acceptMetadata( meta );
                return store;
            }
            else {
                try {
                    RowStore store = makeRowStore();
                    store.acceptMetadata( meta );
                    return store;
                }
                catch ( TableFormatException e ) {
                    ListRowStore store = new ListRowStore();
                    store.acceptMetadata( meta );
                    return store;
                }
            }
        }
        public String toString() {
            return "StoragePolicy.PREFER_DISK";
        }
    };
}
