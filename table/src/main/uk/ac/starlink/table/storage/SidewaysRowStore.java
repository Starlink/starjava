package uk.ac.starlink.table.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.Cleaner;

/**
 * RowStore implementation which stores data cell data in a column-oriented
 * fashion, so that members of the same column, rather than of the same row, 
 * are stored contiguously on disk.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class SidewaysRowStore implements RowStore {

    private final File file_;
    private final Set<File> tempFiles_;
    private final Cleaner.Cleanable tidier_;
    private int ncol_;
    private StarTable template_;
    private long lrow_;
    private ColumnStore[] colStores_;
    private StarTable storedTable_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Constructs a new row store with a given base path name to use for
     * temporary files.
     *
     * @param   file  base scratch file name
     */
    public SidewaysRowStore( File file ) throws IOException {
        file_ = file;
        tempFiles_ = new HashSet<File>();
        tidier_ = Cleaner.getInstance()
                 .register( this, new TidyAction( tempFiles_ ) );
    }

    /**
     * Constructs a new row store with an automatically chosen (and 
     * guaranteed unique) base pathname for scrach files.
     */
    public SidewaysRowStore() throws IOException {
        this( File.createTempFile( "SidewaysRowStore", ".bin" ) );
        file_.deleteOnExit();
    }

    public void acceptMetadata( StarTable meta ) throws TableFormatException {
        try {
            doAcceptMetadata( meta );
        }
        catch ( TableFormatException e ) {
            tidier_.clean();
            throw e;
        }
        catch ( IOException e ) {
            tidier_.clean();
            throw new TableFormatException( "I/O trouble during RowStore setup",
                                            e );
        }
    }

    /**
     * Does the work for preparing to store cell data.
     *
     * @param  meta   template (metadata) table
     */
    private void doAcceptMetadata( StarTable meta ) throws IOException {

        /* Check state. */
        if ( template_ != null ) {
            throw new IllegalStateException( "Metadata already submitted" );
        }
        logger_.info( "Storing table data in " + file_ );

        /* Prepare an array of encoder/decoder objects.  These know how to
         * write/read data of the type appropriate to each table column
         * to/from a stream or buffer. */
        template_ = meta;
        ncol_ = meta.getColumnCount();
        Codec[] codecs = new Codec[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo cinfo = meta.getColumnInfo( icol );
            Codec codec = Codec.getCodec( cinfo );
            if ( codec == null ) {
                throw new TableFormatException( "No codec available for " + 
                                                cinfo );
            }
            codecs[ icol ] = codec;
        }

        /* Prepare an array of ColumnStore objects.  Different implementations
         * must be selected according to whether the size of each item
         * is fixed or variable, and whether the number of rows is known
         * or unknown. */
        colStores_ = new ColumnStore[ ncol_ ];
        long nrow = meta.getRowCount();

        /* If the number of rows is known, we can use one big file
         * to hold all the table data, with a separater mapped
         * ByteBuffer for each column.
         * In the case of variable-sized items, we store only the
         * offsets of the start of each item in the main file,
         * and the ColumnStore will use an additional mapped file
         * for the data items themselves. */
        if ( nrow >= 0 ) {
            file_.deleteOnExit();
            tempFiles_.add( file_ );
            RandomAccessFile raf = new RandomAccessFile( file_, "rw" );
            FileChannel chan = raf.getChannel();
            long offset = 0L;
            for ( int icol = 0; icol < ncol_; icol++ ) {
                Codec codec = codecs[ icol ];
                int itemSize = codec.getItemSize();
                boolean fixedSize = itemSize >= 0;
                long storeSize = nrow * ( fixedSize ? itemSize : 8 );
                ByteBuffer bbuf = chan.map( FileChannel.MapMode.READ_WRITE,
                                            offset, storeSize );
                ColumnStore colStore;
                if ( fixedSize ) {
                    colStore = new MappedColumnStore( codec, bbuf );
                }
                else {
                    File file = new File( file_ + "_" + icol );
                    file.deleteOnExit();
                    tempFiles_.add( file );
                    colStore =
                        new IndexedMappedColumnStore( codec, bbuf, file );
                }
                offset += storeSize;
                colStores_[ icol ] = colStore;
            }
            raf.close();
            assert ! chan.isOpen();
        }

        /* If the number of rows is unknown, we have to stream to a separate
         * file for each column.  For items of variable size, two files are
         * required, one for the offsets and one for the data. */
        else {
            for ( int icol = 0; icol < ncol_; icol++ ) {
                Codec codec = codecs[ icol ];
                int itemSize = codec.getItemSize();
                boolean fixedSize = itemSize >= 0;
                File dataFile = new File( file_ + "_" + icol );
                dataFile.deleteOnExit();
                tempFiles_.add( dataFile );
                ColumnStore colStore;
                if ( fixedSize ) {
                    colStore = new StreamColumnStore( codec, dataFile );
                }
                else {
                    File indexFile = new File( dataFile + "_ix" );
                    indexFile.deleteOnExit();
                    tempFiles_.add( indexFile );
                    colStore = new IndexedStreamColumnStore( codec, dataFile,
                                                             indexFile );
                }
                colStores_[ icol ] = colStore;
            }
        }
    }

    public void acceptRow( Object[] row ) throws IOException {
        if ( template_ == null ) {
            throw new IllegalStateException( "acceptMetadata not called" );
        }
        if ( storedTable_ != null ) {
            throw new IllegalStateException( "endRows already called" );
        }
        for ( int icol = 0; icol < ncol_; icol++ ) {
            colStores_[ icol ].acceptCell( row[ icol ] );
        }
        lrow_++;
    }

    public void endRows() throws IOException {
        if ( template_ == null ) {
            throw new IllegalStateException( "acceptMetadata not called" );
        }
        if ( storedTable_ != null ) {
            throw new IllegalStateException( "endRows already called" );
        }
        for ( int icol = 0; icol < ncol_; icol++ ) {
            colStores_[ icol ].endCells();
        }
        storedTable_ = new ColumnStoreStarTable( template_, lrow_, colStores_ );
    }

    public StarTable getStarTable() {
        if ( storedTable_ == null ) {
            throw new IllegalStateException( "endRows not called" );
        }
        return storedTable_;
    }

    /**
     * Callback to delete temporary files when this object is no longer needed.
     */
    private static class TidyAction implements Runnable {
        final Collection<File> files_;
        TidyAction( Collection<File> files ) {
            files_ = files;
        }
        public void run() {
            for ( Iterator<File> it = files_.iterator(); it.hasNext(); ) {
                File file = it.next();
                if ( file.exists() ) {
                    if ( file.delete() ) {
                        logger_.info( "Deleted temporary file " + file );
                        it.remove();
                    }
                    else {
                        logger_.warning( "Failed to delete temporary file "
                                       + file );
                    }
                }
            }
        }
    }
}
