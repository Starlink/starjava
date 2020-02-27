package uk.ac.starlink.feather;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import uk.ac.bristol.star.feather.BufUtils;
import uk.ac.bristol.star.feather.FeatherType;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;

/**
 * StarColumnWriter implementations for variable-length values.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2020
 */
public abstract class VariableStarColumnWriter extends StarColumnWriter {

    private final PointerSize psize_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.feather" );

    /**
     * Constructor.
     *
     * @param  table  input table
     * @param  icol   column index
     * @param  ftype  output data type
     * @param  isNullable  if true, nulls will be marked as invalid;
     *                     if false, they will just be represented
     *                     as zero length
     * @param  psize  pointer size
     */
    protected VariableStarColumnWriter( StarTable table, int icol,
                                        FeatherType ftype, boolean isNullable,
                                        PointerSize psize ) {
        super( table, icol, ftype, isNullable );
        psize_ = psize;
    }

    /**
     * Returns the number of bytes that a given item will occupy in
     * the output stream.
     *
     * @param  item   writable value
     * @return   byte count
     */
    public abstract int getItemSize( Object item );

    /**
     * Writes a value to the output stream.
     * The output value must be consistent with {@link #getItemSize}.
     *
     * @param  out   destination stream
     * @param  item  writable value
     * @return  number of bytes written
     */
    public abstract int writeItemBytes( OutputStream out, Object item )
            throws IOException;

    public DataStat writeDataBytes( OutputStream out ) throws IOException {
        final int icol = getColumnIndex();

        /* Write offsets. */
        IndexStatus ixStat;
        RowSequence irseq = getTable().getRowSequence();
        try {
            ixStat = writeOffsets( out, irseq );
        }
        finally {
            irseq.close();
        }
        long ixb = psize_.nbyte_ * ( ixStat.rowCount_ + 1 );
        long indexBytes = ixb + BufUtils.align8( out, ixb );

        /* Write data. */
        long entryLimit = ixStat.entryCount_;
        RowSequence drseq = getTable().getRowSequence();
        try {
            for ( long ir = 0; drseq.next() && ir < entryLimit; ir++ ) {
                writeItemBytes( out, drseq.getCell( icol ) );
            }
        }
        finally {
            drseq.close();
        }
        long db = ixStat.byteCount_;
        long dataBytes = db + BufUtils.align8( out, db );

        long nbyte = indexBytes + dataBytes;
        long nrow = ixStat.rowCount_;
        return new DataStat( nbyte, nrow );
    }

    public ItemAccumulator createItemAccumulator( StoragePolicy storage ) {
        final ByteStore indexStore = storage.makeByteStore();
        final ByteStore dataStore = storage.makeByteStore();
        final OutputStream indexOut =
            new BufferedOutputStream( indexStore.getOutputStream() );
        final OutputStream dataOut =
            new BufferedOutputStream( dataStore.getOutputStream() );
        return new AbstractItemAccumulator( storage, isNullable() ) {
            long ioff;
            boolean hasOverflowed;
            long nrow;
            public void addDataItem( Object item ) throws IOException {
                nrow++;
                psize_.writeOffset( indexOut, ioff );
                long ioff1 = ioff + writeItemBytes( dataOut, item );
                if ( ! psize_.isOverflow( ioff1 ) ) {
                    ioff = ioff1;
                }
                else {
                    if ( ! hasOverflowed ) {
                        hasOverflowed = true;
                        logger_.warning( "Pointer overflow - "
                                       + "empty values in column "
                                       + getTable()
                                        .getColumnInfo( getColumnIndex() )
                                        .getName()
                                       + " past row " + nrow );
                    }
                }
            }
            public long writeDataBytes( OutputStream out ) throws IOException {
                psize_.writeOffset( indexOut, ioff );
                long ixb = psize_.nbyte_ * ( nrow + 1 );
                long indexBytes = ixb + BufUtils.align8( indexOut, ixb );
                indexOut.close();
                indexStore.copy( out );
                indexStore.close();
                dataOut.close();
                dataStore.copy( out );
                dataStore.close();
                return indexBytes + ioff;
            }
            public void closeData() throws IOException {
                indexOut.close();
                indexStore.close();
                dataOut.close();
                dataStore.close();
            }
        };
    }

    /**
     * Writes the index block giving start positions of each item in
     * the output data block.
     *
     * @param  out  destination stream
     * @param  rseq   row sequence containing data to write
     * @return   details about what was actually written
     */
    private IndexStatus writeOffsets( OutputStream out, RowSequence rseq )
            throws IOException {
        final int icol = getColumnIndex();
        long nrow = 0;
        long ioff = 0;
        while ( rseq.next() ) {
            psize_.writeOffset( out, ioff );
            long ioff1 = ioff + getItemSize( rseq.getCell( icol ) );
            if ( psize_.isOverflow( ioff1 ) ) {
                logger_.warning( "Pointer overflow - empty values in column "
                               + getTable().getColumnInfo( icol ).getName()
                               + " past row " + nrow );
                long entryCount = nrow;
                do {
                    psize_.writeOffset( out, ioff );
                    nrow++;
                } while ( rseq.next() );
                return new IndexStatus( nrow, entryCount, ioff );
            }
            ioff = ioff1;
            nrow++;
        }
        psize_.writeOffset( out, ioff );
        return new IndexStatus( nrow, nrow, ioff );
    }

    /**
     * Returns a column writer for variable-length string values.
     *
     * @param  table  input table
     * @param  icol   column index
     * @param  isNullable  if true, nulls will be marked as invalid;
     *                     if false, they will just be represented
     *                     as zero length strings
     * @param  psize  pointer size
     */
    public static VariableStarColumnWriter
            createStringWriter( StarTable table, int icol,
                                boolean isNullable, PointerSize psize ) {
        return new VariableStarColumnWriter( table, icol, psize.utf8Type_,
                                             isNullable, psize ) {
            public int getItemSize( Object item ) {
                return item == null
                     ? 0
                     : BufUtils.utf8Length( item.toString() );
            }
            public int writeItemBytes( OutputStream out, Object item )
                    throws IOException {
                if ( item != null ) {
                    byte[] bytes = item.toString().getBytes( BufUtils.UTF8 );
                    out.write( bytes );
                    return bytes.length;
                }
                else {
                    return 0;
                }
            }
        };
    }

    /**
     * Returns a column writer for variable-length byte array values.
     *
     * @param  table  input table
     * @param  icol   column index
     * @param  isNullable  if true, nulls will be marked as invalid;
     *                     if false, they will just be represented
     *                     as zero length arrays
     * @param  psize  pointer size
     */
    public static VariableStarColumnWriter
            createByteArrayWriter( StarTable table, int icol,
                                   boolean isNullable, PointerSize psize ) {
        return new VariableStarColumnWriter( table, icol, psize.binaryType_,
                                             isNullable, psize ) {
            public int getItemSize( Object item ) {
                return item instanceof byte[]
                     ? ((byte[]) item).length
                     : 0;
            }
            public int writeItemBytes( OutputStream out, Object item )
                    throws IOException {
                if ( item instanceof byte[] ) {
                    byte[] bytes = (byte[]) item;
                    out.write( bytes );
                    return bytes.length;
                }
                else {
                    return 0;
                }
            }
        };
    }

    /**
     * Enumeration for pointer size.
     */
    public enum PointerSize {

        /** Short (32-bit) pointer size. */
        I32( 4, FeatherType.UTF8, FeatherType.BINARY ) {
            void writeOffset( OutputStream out, long ioff ) throws IOException {
                BufUtils.writeLittleEndianInt( out, (int) ioff );
            }
            boolean isOverflow( long ioff ) {
                return ioff >= Integer.MAX_VALUE;
            }
        },

        /** Long (64-bit) pointer size. */
        I64( 8, FeatherType.LARGE_UTF8, FeatherType.LARGE_BINARY ) {
            void writeOffset( OutputStream out, long ioff ) throws IOException {
                BufUtils.writeLittleEndianLong( out, ioff );
            }
            boolean isOverflow( long ioff ) {
                return false;
            }
        };

        final int nbyte_;
        final FeatherType utf8Type_;
        final FeatherType binaryType_;

        /**
         * Constructor.
         *
         * @param  nbyte  bytes per pointer
         * @param  utf8Type  feather type for UTF8 strings
         * @param  binaryType  feather tyep for byte array strings
         */
        private PointerSize( int nbyte, FeatherType utf8Type,
                             FeatherType binaryType ) {
            nbyte_ = nbyte;
            utf8Type_ = utf8Type;
            binaryType_ = binaryType;
        }
        abstract void writeOffset( OutputStream out, long ioff )
                throws IOException;
        abstract boolean isOverflow( long ioff );
    }

    /**
     * Aggregates information about an index block that has been written.
     */
    private static class IndexStatus {
        final long rowCount_;
        final long entryCount_;
        final long byteCount_;

        /**
         * Constructor.
         *
         * @param  rowCount  number of rows
         * @param  entryCount  number of data values to actually write
         *                     (may differ from nrow in case of overflow)
         * @param  byteCount  byte count for data block
         */
        IndexStatus( long rowCount, long entryCount, long byteCount ) {
            rowCount_ = rowCount;
            entryCount_ = entryCount;
            byteCount_ = byteCount;
        }
    }
}
