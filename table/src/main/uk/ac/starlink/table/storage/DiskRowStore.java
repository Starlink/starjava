package uk.ac.starlink.table.storage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Implementation of RowStore which stores data on disk.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
public class DiskRowStore implements RowStore {

    private final File file_;
    private boolean isTempFile_;
    private StarTable template_;
    private Codec[] codecs_;
    private List[] colSizeLists_;
    private DataOutputStream out_;
    private long nrow_;
    private int ncol_;
    private Offsets offsets_;
    private StarTable storedTable_;

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

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
        file_ = file;
        out_ = new DataOutputStream(
                   new BufferedOutputStream( new FileOutputStream( file ) ) );
    }

    /**
     * Constructs a new DiskRowStore which uses a temporary file as
     * backing store.
     * The temporary file will be written to the default temporary 
     * directory, given by the value of the <tt>java.io.tmpdir</tt>
     * system property.
     *
     * @throws IOException  if there is some I/O-related problem with
     *         opening the file
     * @throws SecurityException  if the current security context does not
     *         allow writing to a temporary file
     */
    public DiskRowStore() throws IOException {
        this( File.createTempFile( "DiskRowStore", ".bin" ) );
        file_.deleteOnExit();
        isTempFile_ = true;
    }

    public void acceptMetadata( StarTable meta ) throws TableFormatException {
        if ( template_ != null ){ 
            throw new IllegalStateException( "Metadata already submitted" );
        }
        logger_.info( "Storing table data in " + file_ );
        template_ = meta;
        ncol_ = meta.getColumnCount();
        codecs_ = new Codec[ ncol_ ];
        colSizeLists_ = new List[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo cinfo = meta.getColumnInfo( icol );
            Codec codec = Codec.getCodec( cinfo );
            if ( codec == null ) {
                throw new TableFormatException( "No codec available for " + 
                                                cinfo );
            }
            codecs_[ icol ] = codec;
            if ( codec.getItemSize() < 0 ) {
                colSizeLists_[ icol ] = new ArrayList();
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
            int nbyte = codecs_[ icol ].encode( row[ icol ], out_ );
            if ( colSizeLists_[ icol ] != null ) {
                colSizeLists_[ icol ].add( new Integer( nbyte ) );
            }
        }
        nrow_++;
    }

    public void endRows() throws IOException {
        if ( template_ == null ) {
            throw new IllegalStateException( "acceptMetadata not called" );
        }
        if ( storedTable_ != null ) {
            throw new IllegalStateException( "endRows already called" );
        }

        /* Close the output stream. */
        out_.close();

        /* Calculate lookup tables for row and column start offsets. */
        ColumnWidth[] colWidths = new ColumnWidth[ ncol_ ];
        int[][] widthArrays = new int[ ncol_ ][];
        boolean someVariable = false;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnWidth cw;
            if ( colSizeLists_[ icol ] == null ) {
                cw = ColumnWidth
                    .constantColumnWidth( codecs_[ icol ].getItemSize() );
            }
            else {
                int[] widthArray = new int[ Tables.checkedLongToInt( nrow_ ) ];
                List widthList = colSizeLists_[ icol ];
                for ( int irow = 0; irow < nrow_; irow++ ) {
                    widthArray[ irow ] = ((Integer) widthList.get( irow ))
                                        .intValue();
                }
                cw = ColumnWidth.variableColumnWidth( widthArray );
                colSizeLists_[ icol ] = null;
            }
            colWidths[ icol ] = cw;
        }
        colSizeLists_ = null;
        offsets_ = Offsets.getOffsets( colWidths, nrow_ );

        /* Create a new StarTable instance based on the data we've cached. */
        long fileSize = offsets_.getLength();
        FileInputStream istrm = new FileInputStream( file_ );
        ByteBuffer bbuf = istrm.getChannel()
                         .map( FileChannel.MapMode.READ_ONLY, 0, fileSize );
        istrm.close();
        logger_.info( nrow_ + " rows stored in " + fileSize + " bytes" );
        SeekableDataInput in = new NioDataInput( bbuf );
        storedTable_ = new DiskStarTable( in );
    }

    public StarTable getStarTable() {
        if ( storedTable_ == null ){
            throw new IllegalStateException( "endRows not called" );
        }
        return storedTable_;
    }

    /**
     * Returns the offsets object.  This package-private method is only 
     * intended for testing.
     */
    Offsets getOffsets() {
        return offsets_;
    }

    /**
     * StarTable implementation which reads the data stored in this object.
     */
    private class DiskStarTable extends WrapperStarTable {
        final SeekableDataInput in_;

        DiskStarTable( SeekableDataInput in ) {
            super( template_ );
            in_ = in;
        }

        public boolean isRandom() {
            return true;
        }

        public long getRowCount() {
            return nrow_;
        }

        public Object[] getRow( long lrow ) throws IOException {
            Object[] row = new Object[ ncol_ ];
            synchronized ( in_ ) {
                in_.seek( offsets_.getRowOffset( lrow ) );
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = codecs_[ icol ].decode( in_ );
                }
            }
            return row;
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            Object cell;
            synchronized ( in_ ) {
                in_.seek( offsets_.getCellOffset( lrow, icol ) );
                cell = codecs_[ icol ].decode( in_ );
            }
            return cell;
        }

        public RowSequence getRowSequence() throws IOException {
            return new RandomRowSequence( this );
        }
    }
}
