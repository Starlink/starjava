package uk.ac.starlink.parquet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnReadStore;
import org.apache.parquet.column.ColumnReader;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.storage.Codec;
import uk.ac.starlink.table.storage.ColumnStore;
import uk.ac.starlink.table.storage.ColumnStoreStarTable;
import uk.ac.starlink.table.storage.IndexedStreamColumnStore;
import uk.ac.starlink.table.storage.StreamColumnStore;
import uk.ac.starlink.util.IOSupplier;

/**
 * ParquetStarTable implementation that does a parallel read of
 * all the column data at construction time.
 *
 * @author   Mark Taylor
 * @since    2 Mar 2021
 */
public class CachedParquetStarTable extends ParquetStarTable {

    private final ColumnStoreStarTable dataTable_;
    private final Path basePath_;
    private final Collection<File> tmpFiles_; 
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.parquet" );

    /**
     * Constructor.
     *
     * @param  pfrSupplier  access to parquet data file
     * @param  nthread   number of threads to use for concurrent column reads;
     *                   if &lt;=0, a value is chosen based on the number
     *                   of available processors
     */
    @SuppressWarnings("this-escape")
    public CachedParquetStarTable( IOSupplier<ParquetFileReader> pfrSupplier,
                                   int nthread )
            throws IOException {
        super( pfrSupplier );

        /* Determine number of read threads. */
        if ( nthread <= 0 ) {
            nthread = getDefaultThreadCount();
        }

        /* Manage temporary file storage.  Note this does not use a
         * pluggable storage policy.  It probably should do. */
        basePath_ = Files.createTempDirectory( "CacheTable" );
        basePath_.toFile().deleteOnExit();
        tmpFiles_ =
            Collections.newSetFromMap( new ConcurrentHashMap<File,Boolean>() );
        logger_.info( "Will cache parquet data in " + basePath_ );

        /* Submit one job to read each column.  Parquet is column-oriented,
         * so this makes sense in terms of file access.  The ExecutorService
         * will manage things so that only a fixed number of these jobs
         * is executing concurrently. */
        ExecutorService executor = Executors.newFixedThreadPool( nthread );
        List<Future<ColumnStore>> futures = new ArrayList<>();
        int ncol = getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            final int ic = icol;
            Callable<ColumnStore> reader = () -> readColumn( ic );
            futures.add( executor.submit( reader ) );
        }

        /* Read the column data concurrently.  This will block until all
         * the results are in. */
        List<ColumnStore> colStores = new ArrayList<>();
        try {
            for ( Future<ColumnStore> future : futures ) {
                colStores.add( future.get() );
            }
        }
        catch ( InterruptedException | ExecutionException e ) {
            executor.shutdownNow();
            deleteFiles();
            throw new IOException( "Parallel read failure", e );
        }
        executor.shutdown();

        /* Prepare an object that manages access to the cached column data. */
        ColumnStore[] cstores = colStores.toArray( new ColumnStore[ 0 ] );
        dataTable_ = new ColumnStoreStarTable( this, getRowCount(), cstores );
    }

    public boolean isRandom() {
        return true;
    }

    public RowSequence getRowSequence() throws IOException {
        return dataTable_.getRowSequence();
    }

    public RowAccess getRowAccess() throws IOException {
        return dataTable_.getRowAccess();
    }

    public RowSplittable getRowSplittable() throws IOException {
        return dataTable_.getRowSplittable();
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return dataTable_.getCell( irow, icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return dataTable_.getRow( irow );
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        }
        finally {
            deleteFiles();
        }
    }

    /**
     * Reads the contents of a column from this table's parquet file
     * into a random-access data structure.
     *
     * @param   icol  index of the column in this table to read
     * @return   cached column data
     */
    private ColumnStore readColumn( int icol ) throws IOException {
        InputColumn<?> incol = getInputColumn( icol );
        ColumnInfo cinfo = getColumnInfo( icol );
        List<File> tmpFiles = new ArrayList<>();

        /* Prepare storage for the column data. */
        Codec codec = Codec.getCodec( cinfo );
        int itemSize = codec.getItemSize();
        boolean fixedSize = itemSize >= 0;
        File dataFile = createTempFile( icol, "dat" );
        tmpFiles.add( dataFile );
        final ColumnStore colStore;
        if ( fixedSize ) {
            colStore = new StreamColumnStore( codec, dataFile );
        }
        else {
            File indexFile = createTempFile( icol, "idx" );
            tmpFiles.add( indexFile );
            colStore =
                new IndexedStreamColumnStore( codec, dataFile, indexFile );
        }
        logger_.config( "Caching data for column " + cinfo.getName()
                      + " " + tmpFiles );

        /* Prepare the ParquetFileReader so that it only reads the
         * metadata for the column under consideration.  If you don't do
         * this it still works, but the metadata read can be very slow
         * and have a very large memory footprint.  It took me a long
         * time exploring the essentially undocumented parquet-mr API
         * to work out this is what you have to do. */
        String[] cpath = incol.getColumnDescriptor().getPath();
        List<Type> types = new ArrayList<>();
        MessageType schema = getSchema();
        for ( int ip = 1; ip <= cpath.length; ip++ ) {
            String[] subpath = new String[ ip ];
            System.arraycopy( cpath, 0, subpath, 0, ip );
            types.add( schema.getType( subpath ) );
        }
        MessageType projSchema =
            new MessageType( "col_" + cinfo.getName(), types );
        ParquetFileReader pfr = getParquetFileReader();
        pfr.setRequestedSchema( projSchema );

        /* Read the column data into the storage, and return it. */
        ColumnDescriptor cdesc = incol.getColumnDescriptor();
        final int cdefmax = cdesc.getMaxDefinitionLevel();
        for ( PageReadStore pageStore;
              ( pageStore = pfr.readNextRowGroup() ) != null; ) {
            ColumnReadStore crstore =
                getColumnReadStore( pageStore, projSchema );
            Decoder<?> decoder = incol.createDecoder();
            ColumnReader crdr = crstore.getColumnReader( cdesc );
            long nr = pageStore.getRowCount();
            for ( long ir = 0; ir < nr; ir++ ) {
                decoder.clearValue();
                do {
                    int cdef = crdr.getCurrentDefinitionLevel();
                    if ( cdef == cdefmax ) {
                        decoder.readItem( crdr );
                    }
                    else if ( cdef == cdefmax - 1 ) {
                       decoder.readNull();
                    }
                    crdr.consume();
                }
                while ( crdr.getCurrentRepetitionLevel() > 0 );
                colStore.acceptCell( decoder.getValue() );
            }
        }
        colStore.endCells();
        return colStore;
    }

    /**
     * Returns a temporary file in which data can be stored.
     * Steps are taken to delete the file on table closure or JVM shutdown.
     *
     * @param  icol  column index
     * @param  ftype   extension string without "."
     */
    private File createTempFile( int icol, String ftype ) throws IOException {
        Path fpath = basePath_.getFileSystem()
                    .getPath( basePath_.toString(),
                              "col-" + icol + "." + ftype );
        FileAttribute<?> permissions =
            PosixFilePermissions
           .asFileAttribute( PosixFilePermissions.fromString( "rw-------" ) );
        Files.createFile( fpath, permissions );
        File file = fpath.toFile();
        file.deleteOnExit();
        tmpFiles_.add( file );
        return file;
    }

    /**
     * Attempts to delete any files that have been written by this object.
     */
    private void deleteFiles() {
        for ( Iterator<File> it = tmpFiles_.iterator(); it.hasNext(); ) {
            File file = it.next();
            if ( ! file.delete() ) {
                logger_.warning( "Failed to remove temp file " + file );
            }
            it.remove();
        }
        if ( ! basePath_.toFile().delete() ) {
            logger_.warning( "Failed to remove temp dir " + basePath_ );
        }
    }

    /**
     * Returns the default number of read threads if not specified explicitly.
     *
     * @return  read thread count
     */
    static int getDefaultThreadCount() {
        return Math.max( 1, Runtime.getRuntime().availableProcessors() - 1 );
    }
}
