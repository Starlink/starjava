package uk.ac.starlink.table.storage;

import java.io.File;
import java.io.IOException;

/**
 * Implementation of RowStore which stores data on disk.
 * The temporary file is deleted by the finalizer (if it runs) or failing
 * that at JVM exit.  Since there's no guarantee when or if the finalizer
 * will run even after this object is a candidate for garbage collection,
 * this does raise the possibility that large numbers of potentially large
 * temporary files will accumulate during JVM operation.
 * It depends on your GC.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
public class DiskRowStore extends ByteStoreRowStore {

    /**
     * Constructs a new DiskRowStore which uses the given file as a
     * backing store.  Nothing is done to mark this file as temporary.
     * Since the storage format is not public, specifying the file like
     * this isn't very useful except for test purposes.
     *
     * @param  file   location of the backing file which will be used
     * @throws IOException  if there is some I/O-related problem with
     *         opening the file
     * @throws SecurityException  if the current security context does not
     *         allow writing to a temporary file
     */
    public DiskRowStore( File file ) throws IOException {
        super( new FileByteStore( file ) );
    }

    /**
     * Constructs a new DiskRowStore which uses a temporary file as
     * backing store.
     * The temporary file will be written to the default temporary
     * directory, given by the value of the <code>java.io.tmpdir</code>
     * system property.
     *
     * @throws IOException  if there is some I/O-related problem with
     *         opening the file
     * @throws SecurityException  if the current security context does not
     *         allow writing to a temporary file
     */
    public DiskRowStore() throws IOException {
        this( File.createTempFile( "DiskRowStore", ".bin" ) );
        ((FileByteStore) getByteStore()).getFile().deleteOnExit();
    }
}
