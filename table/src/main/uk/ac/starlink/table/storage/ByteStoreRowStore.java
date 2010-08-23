package uk.ac.starlink.table.storage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.util.IntList;

/**
 * RowStore based on a ByteStore.
 * It uses custom serialization to encode some basic data types
 * (primitives, Strings, and arrays of them) as a byte sequence.
 * Therefore it may not cope with tables containing exotic objects.
 * The actual storage destination is controlled by the 
 * {@link uk.ac.starlink.table.ByteStore} implementation used.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2009
 */
public class ByteStoreRowStore implements RowStore {

    private final ByteStore byteStore_;
    private final DataOutputStream out_;
    private int ncol_;
    private int nrow_;
    private Codec[] codecs_;
    private IntList[] colSizeLists_;
    private Offsets offsets_;
    private StarTable template_;
    private StarTable storedTable_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Constructor.
     *
     * @param  byteStore  storage container used by this row store
     */
    public ByteStoreRowStore( ByteStore byteStore ) {
        byteStore_ = byteStore;
        out_ = new DataOutputStream(
                   new BufferedOutputStream( byteStore.getOutputStream() ) );
    }

    /**
     * Returns the underlying storage for this row store.
     *
     * @return  buffer holding byte data
     */
    public ByteStore getByteStore() {
        return byteStore_;
    }

    public void acceptMetadata( StarTable meta ) throws TableFormatException {
        if ( template_ != null ) {
            throw new IllegalStateException( "Metadata already submitted" );
        }
        template_ = meta;
        ncol_ = meta.getColumnCount();
        codecs_ = new Codec[ ncol_ ];
        colSizeLists_ = new IntList[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo cinfo = meta.getColumnInfo( icol );
            Codec codec = Codec.getCodec( cinfo );
            if ( codec == null ) {
                throw new TableFormatException( "No codec available for " +
                                                cinfo );
            }
            codecs_[ icol ] = codec;
            if ( codec.getItemSize() < 0 ) {
                colSizeLists_[ icol ] = new IntList();
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
                colSizeLists_[ icol ].add( nbyte );
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
        boolean someVariable = false;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnWidth cw;
            if ( colSizeLists_[ icol ] == null ) {
                cw = ColumnWidth
                    .constantColumnWidth( codecs_[ icol ].getItemSize() );
            }
            else {
                cw = ColumnWidth.variableColumnWidth( colSizeLists_[ icol ]
                                                     .toIntArray() );
                colSizeLists_[ icol ] = null;
            }
            colWidths[ icol ] = cw;
        }
        colSizeLists_ = null;
        offsets_ = Offsets.getOffsets( colWidths, nrow_ );
        logger_.config( "Offset type is "
                      + ( offsets_.isFixed() ? "fixed" : "variable" ) );

        /* Create a new StarTable instance based on the data we've cached. */
        ByteBuffer[] bbufs = byteStore_.toByteBuffers();
        long nbyte = 0;
        for ( int ib = 0; ib < bbufs.length; ib++ ) {
            nbyte += bbufs[ ib ].limit();
        }
        logger_.config( nrow_ + " rows stored in " + nbyte + " bytes" );
        ByteStoreAccess access = NioByteStoreAccess.createAccess( bbufs );
        storedTable_ = new ByteStoreStarTable( template_, nrow_, codecs_,
                                               offsets_, access );
    }

    public StarTable getStarTable() {
        if ( storedTable_ == null ){
            throw new IllegalStateException( "endRows not called" );
        }
        return storedTable_;
    }

    protected void finalize() throws Throwable {
        try {
            byteStore_.close();
        }
        finally {
            super.finalize();
        }
    }

    /**
     * Returns the offsets object.  This package-private method is only
     * intended for testing.
     */
    Offsets getOffsets() {
        return offsets_;
    }

    /**
     * StarTable implementation based on a ByteBuffer.
     */
    private static class ByteStoreStarTable extends WrapperStarTable {
        private final ByteStoreAccess access_;
        private final long nrow_;
        private final int ncol_;
        private final Codec[] codecs_;
        private final Offsets offsets_;

        /**
         * Constructor.
         *
         * @param  template  template table giving column metadata etc
         * @param  nrow    row count
         * @param  codecs  per-column de/serializer array
         * @param  offsets  information about row offsets into the byte store
         * @param  access  byte store reader
         * @param  bbufs   buffers holding byte data
         */
        ByteStoreStarTable( StarTable template, long nrow, Codec[] codecs,
                            Offsets offsets, ByteStoreAccess access ) {
            super( template );
            nrow_ = nrow;
            ncol_ = template.getColumnCount();
            codecs_ = codecs;
            offsets_ = offsets;
            access_ = access;
        }

        public boolean isRandom() {
            return true;
        }

        public long getRowCount() {
            return nrow_;
        }

        public Object[] getRow( long lrow ) throws IOException {
            Object[] row = new Object[ ncol_ ];
            synchronized ( access_ ) {
                access_.seek( offsets_.getRowOffset( lrow ) );
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = codecs_[ icol ].decode( access_ );
                }
            }
            return row;
        }

        public Object getCell( long lrow, int icol ) throws IOException {
            final Object cell;
            synchronized ( access_ ) {
                access_.seek( offsets_.getCellOffset( lrow, icol ) );
                cell = codecs_[ icol ].decode( access_ );
            }
            return cell;
        }

        public RowSequence getRowSequence() throws IOException {
            return new RandomRowSequence( this );
        }
    }
}
