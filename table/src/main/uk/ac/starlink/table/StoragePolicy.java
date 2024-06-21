package uk.ac.starlink.table;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;
import uk.ac.starlink.table.jdbc.JDBCStarTable;
import uk.ac.starlink.table.storage.AdaptiveByteStore;
import uk.ac.starlink.table.storage.ByteStoreStoragePolicy;
import uk.ac.starlink.table.storage.ListRowStore;
import uk.ac.starlink.table.storage.DiscardByteStore;
import uk.ac.starlink.table.storage.DiscardRowStore;
import uk.ac.starlink.table.storage.DiskRowStore;
import uk.ac.starlink.table.storage.FileByteStore;
import uk.ac.starlink.table.storage.MemoryByteStore;
import uk.ac.starlink.table.storage.SidewaysRowStore;
import uk.ac.starlink.util.Loader;

/**
 * Defines storage methods for bulk data.
 * If the table handling system needs to cache bulk data somewhere,
 * for instance because it is reading a table from a stream but needs
 * to make it available for random access, it will use a StoragePolicy
 * object to work out how to do it. 
 *
 * <p>Code which has no preferences about how to store data can obtain
 * an instance of this class using the {@link #getDefaultPolicy} method.
 * The initial value of this may be selected by setting the 
 * system property named by the string {@link #PREF_PROPERTY}
 * ("startable.storage").
 * You may also use the name of a class which extends <code>StoragePolicy</code>
 * and has a no-arg constructor, in which case one of these will be
 * instantiated and used.
 * The default, if not otherwise set, corresponds to "<code>adaptive</code>".
 *
 * <p>Code which wants to store data in a particular way may use one of
 * the predefined policies {@link #ADAPTIVE}, {@link #PREFER_MEMORY},
 * {@link #PREFER_DISK} {@link #SIDEWAYS} or {@link #DISCARD},
 * or may implement their own policy by extending this class.
 * If you want more control, you can always create instances of the 
 * public {@link RowStore} implementations directly.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class StoragePolicy {

    private static StoragePolicy defaultInstance_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );
    private static boolean defaultLogged_;

    /**
     * Smallest number of cells that will get written to disk by default.
     * Any less than this, and it's not considered ever writing to a disk
     * file.
     */
    private static final int MIN_DISK_CELLS = 1000;

    /**
     * Name of the system property which can be set to indicate the
     * initial setting of the default storage policy ({@value}).
     * Currently recognised values are "adaptive", "memory", "disk",
     * "sideways", and "discard".
     * Alternatively, the classname of a StoragePolicy implementation
     * with a no-arg constructor may be supplied.
     */
    public static final String PREF_PROPERTY = "startable.storage";

    /**
     * Returns the default storage policy for this JVM.
     *
     * @return   default storage policy
     */
    public static StoragePolicy getDefaultPolicy() {
        if ( defaultInstance_ == null ) {
            try {
                String pref = System.getProperty( PREF_PROPERTY );
                if ( "adaptive".equals( pref ) ) {
                    defaultInstance_ = ADAPTIVE;
                }
                else if ( "memory".equals( pref ) ) {
                    defaultInstance_ = PREFER_MEMORY;
                }
                else if ( "disk".equals( pref ) ) {
                    defaultInstance_ = PREFER_DISK;
                }
                else if ( "sideways".equals( pref ) ) {
                    defaultInstance_ = SIDEWAYS;
                }
                else if ( "discard".equals( pref ) ) {
                    defaultInstance_ = DISCARD;
                }
                else {
                    StoragePolicy named =
                        Loader.getClassInstance( pref, StoragePolicy.class );
                    defaultInstance_ = named != null ? named
                                                     : ADAPTIVE;
                }
            }
            catch ( SecurityException e ) {
                defaultInstance_ = ADAPTIVE;
            }
        }
        if ( ! defaultLogged_ ) {
            defaultLogged_ = true;
            logger_.config( "Initial default StoragePolicy is "
                          + defaultInstance_ );
        }
        return defaultInstance_;
    }

    /**
     * Sets the default storage policy used for this JVM.
     *
     * @param  policy  new default storage policy
     */
    public static void setDefaultPolicy( StoragePolicy policy ) {
        defaultInstance_ = policy;
    }

    /**
     * Returns a new ByteStore object which can be used to
     * provide a destination for general purpose data storage.
     *
     * @return  new byte store
     */
    abstract public ByteStore makeByteStore();

    /**
     * Returns a new <code>RowStore</code> object which can be used to
     * provide a destination for random-access table storage.
     *
     * @return   a RowStore object
     */
    abstract public RowStore makeRowStore();

    /**
     * Creates a new RowStore and primes it by calling
     * {@link uk.ac.starlink.table.RowStore#acceptMetadata} on it.
     *
     * @param   meta  template giving the metadata which describes the rows
     *          that will have to be stored
     * @return  a RowStore on which <code>acceptMetadata(meta)</code> has been
     *          called
     */
    abstract public RowStore makeConfiguredRowStore( StarTable meta );

    /**
     * Returns a table based on a given table and guaranteed to have
     * random access.  If the original table <code>table</code> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * @param  table  original table
     * @return  a table with the same data as <code>table</code> and with
     *          <code>isRandom()==true</code>
     */
    public StarTable randomTable( StarTable table ) throws IOException {

        /* If it's random already, we don't need to do any work. */
        if ( table.isRandom() ) {
            return table;
        }

        /* Otherwise get a suitable row store and stream the rows into it. */
        return copyTable( table );
    }

    /**
     * Returns a random-access deep copy of the given table.
     * This utility method is like {@link #randomTable} except 
     * that a copy is made even if the original is already random access.
     * It can be useful if you want a copy of the table known to have
     * the resource usage or performance characteristics defined by 
     * this policy.
     *
     * @param   table  input table
     * @return  deep copy of <code>table</code>
     */
    public StarTable copyTable( StarTable table ) throws IOException {
        RowStore store = makeConfiguredRowStore( table );
        RowSequence rseq = table.getRowSequence();
        try {
            while ( rseq.next() ) {
                store.acceptRow( rseq.getRow() );
            }
        }
        finally {
            rseq.close();
        }
        store.endRows();

        /* Return the resulting random table. */
        StarTable out = store.getStarTable();
        assert out.isRandom();
        return out;
    }

    /**
     * Storage policy which will always store table data in memory.
     * Table cells are stored as objects, which will be fast to write,
     * and can cope with any object type, but may be expensive on memory.
     */
    public static final StoragePolicy PREFER_MEMORY = new StoragePolicy() {
        public ByteStore makeByteStore() {
            return new MemoryByteStore();
        }
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
     * directory, which is the value of the <code>java.io.tmpdir</code>
     * system property.  These files will be deleted when the JVM exits,
     * if not before.  They will <em>probably</em> be deleted around the
     * time they are no longer needed (when the RowStore in question is 
     * garbage collected), though this cannot be guaranteed since it
     * depends on the details of the JVM's GC implementation.
     */
    public static final StoragePolicy PREFER_DISK =
        new DiskStoragePolicy( "PREFER_DISK", MIN_DISK_CELLS ) {
            protected RowStore makeDiskRowStore() throws IOException {
                return new DiskRowStore();
            }
        };

    /**
     * Storage policy which will normally store table data in scratch disk
     * files in such a way that cells from the same column are contiguous
     * on disk.  This may be more efficient for certain access patterns
     * for tables which are very large and, in particular, very wide.
     * It's generally more expensive on system resources than 
     * {@link #PREFER_DISK} however, so it is only the best choice in
     * rather specialised circumstances.
     * If it's impossible for some reason to store the data in this way,
     * or if the number of cells requested is small, it will fall back
     * to using memory storage.
     * Temporary disk files (at least one per column) are written 
     * in the default temporary directory, which is the value of the 
     * <code>java.io.tmpdir</code> system property.  
     * These files will be deleted when the JVM exits,
     * if not before.  They will <em>probably</em> be deleted around the
     * time they are no longer needed (when the RowStore in question is 
     * garbage collected), though this cannot be guaranteed since it
     * depends on the details of the JVM's GC implementation.
     */
    public static final StoragePolicy SIDEWAYS =
        new DiskStoragePolicy( "SIDEWAYS", MIN_DISK_CELLS ) {
            protected RowStore makeDiskRowStore() throws IOException {
                return new SidewaysRowStore();
            }
        };

    /**
     * Storage policy which just throws away the rows it is given.
     * Tables obtained from its row stores will have no rows.
     * Obviously, this has rather limited application.
     */
    public static final StoragePolicy DISCARD = new StoragePolicy() {
        public ByteStore makeByteStore() {
            return new DiscardByteStore();
        }
        public RowStore makeRowStore() {
            return new DiscardRowStore();
        }
        public RowStore makeConfiguredRowStore( StarTable meta ) {
            DiscardRowStore store = new DiscardRowStore();
            store.acceptMetadata( meta );
            return store;
        }
        public String toString() {
            return "StoragePolicy.DISCARD";
        }
    };

    /**
     * Storage policy which will store small amounts of data in an array
     * in memory, and larger amounts in a scratch disk file.
     * Temporary disk files are written in the default temporary 
     * directory, which is the value of the <code>java.io.tmpdir</code>
     * system property.  These files will be deleted when the JVM exits,
     * if not before.  They will <em>probably</em> be deleted around the
     * time they are no longer needed (when the RowStore in question is 
     * garbage collected), though this cannot be guaranteed since it
     * depends on the details of the JVM's GC implementation.
     */
    public static final StoragePolicy ADAPTIVE = new ByteStoreStoragePolicy() {
        protected ByteStore attemptMakeByteStore() throws IOException {
            return new AdaptiveByteStore();
        } 
        public String toString() {
            return "StoragePolicy.ADAPTIVE";
        }
    };

    /**
     * Abstract superclass of storage policies which use disk-based storage
     * for tables larger than a certain threshold.
     * If disk-based storage can't be used for one reason or another, 
     * or for small tables, it falls back to dispensing 
     * <code>ListRowStore</code>s.
     */
    private abstract static class DiskStoragePolicy extends StoragePolicy {

        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  public name for this policy
         * @param  minDiskCells   threshold of number of cells in a table 
         *         below which memory based storage will be used
         */
        DiskStoragePolicy( String name, int minDiskCells ) {
            name_ = name;
        }

        /**
         * Constructs a disk-based row store.
         *
         * @return  disk row store
         */
        protected abstract RowStore makeDiskRowStore() throws IOException;

        public ByteStore makeByteStore() {
            Exception error;
            try {
                return new FileByteStore();
            }
            catch ( SecurityException e ) {
                error = e;
            }
            catch ( IOException e ) {
                error = e;
            }
            assert error != null;
            logger_.warning( "Failed to create disk storage: " + error
                           + " - using memory instead" );
            return new MemoryByteStore();
        }

        public RowStore makeRowStore() {
            Exception error;
            try {
                return makeDiskRowStore();
            }
            catch ( SecurityException e ) {
                error = e;
            }
            catch ( IOException e ) {
                error = e;
            }
            assert error != null;
            logger_.warning( "Failed to create disk storage: " + error
                           + " - using memory instead" );
            return new ListRowStore();
        }

        public RowStore makeConfiguredRowStore( StarTable meta ) {
            long nrow = meta.getRowCount();
            if ( nrow > 0 && nrow * meta.getColumnCount() < MIN_DISK_CELLS ) {
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
                    logger_.warning( "Disk store " + this
                                   + " unsuitable for table: " + e
                                   + " - using memory instead" );
                    ListRowStore store = new ListRowStore();
                    store.acceptMetadata( meta );
                    return store;
                }
            }
        }

        public String toString() {
            return "StoragePolicy." + name_;
        }
    }
}
