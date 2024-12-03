package uk.ac.starlink.parquet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnReader;
import org.apache.parquet.column.ColumnReadStore;
import org.apache.parquet.column.impl.ColumnReaderImpl;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.schema.MessageType;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.util.IOSupplier;

/**
 * ParquetStarTable concrete subclass that provides sequential access only.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class SequentialParquetStarTable extends ParquetStarTable {

    /**
     * Constructor.
     *
     * @param  pfrSupplier  access to parquet data file
     */
    public SequentialParquetStarTable( IOSupplier<ParquetFileReader>
                                       pfrSupplier )
            throws IOException {
        super( pfrSupplier );
    }

    public boolean isRandom() {
        return false;
    }

    public RowSequence getRowSequence() throws IOException {
        return new ParquetRowSplittable();
    }

    public RowSplittable getRowSplittable() throws IOException {
        return new ParquetRowSplittable();
    }

    /**
     * Returns an array giving the number of rows in each row block
     * for a parquet file.
     *
     * @param   pfr  file reader
     * @return   array giving row counts per row group
     */
    private static long[] getBlockSizes( ParquetFileReader pfr ) {
        return pfr.getRowGroups().stream()
                  .mapToLong( blk -> blk.getRowCount() )
                  .toArray();
    }

    /**
     * RowSequence implementation for ParquetStarTable.
     */
    private class ParquetRowSplittable implements RowSplittable {

        final ParquetFileReader pfr_;
        final int ncol_;
        final MessageType schema_;
        final long[] irows_;
        final long[] blockSizes_;
        int iblock_;
        int iblockEnd_;
        ColAccess<?>[] colAccesses_;
        long irow_;
        long irGroupEnd_;

        /**
         * Constructs a splittable over the whole table.
         */
        public ParquetRowSplittable() throws IOException {
            this( -1, -1 );
        }

        /**
         * Constructor for internal use, constructs a sub-splittable.
         *
         * @param  iblock  index before first block to be processed
         * @param  iblockEnd  index after last block to be processed,
         *                    or -1 for last block in table
         */
        private ParquetRowSplittable( int iblock, int iblockEnd )
                throws IOException {
            pfr_ = getParquetFileReader();
            ncol_ = getColumnCount();
            schema_ = getSchema();
            blockSizes_ = getBlockSizes( pfr_ );
            irows_ = new long[ ncol_ ];
            iblock_ = -1;
            irow_ = -1;
            iblockEnd_ = iblockEnd >= 0 ? iblockEnd : blockSizes_.length;
            skipBlocks( iblock + 1 );
        }

        public LongSupplier rowIndex() {
            return () -> irow_;
        }

        public ParquetRowSplittable split() {
            if ( colAccesses_ == null && iblockEnd_ - iblock_ > 2 ) {
                int mid = ( 1 + iblock_ + iblockEnd_ ) / 2;
                ParquetRowSplittable split;
                try {
                    split = new ParquetRowSplittable( iblock_, mid );
                    skipBlocks( mid - 1 - iblock_ );
                }
                catch ( IOException e ) {
                    return null;
                }
                return split;
            }
            else {
                return null;
            }
        }

        public long splittableSize() {
            long nr = 0;
            for ( int ib = iblock_ + 1; ib < iblockEnd_; ib++ ) {
                nr += blockSizes_[ ib ];
            }
            return nr;
        }

        public boolean next() throws IOException {
            assert irow_ < irGroupEnd_;
            if ( irow_ + 1 == irGroupEnd_ ) {
                assert iblock_ < iblockEnd_;
                if ( iblock_ + 1 == iblockEnd_ ) {
                    return false;
                }
                nextReadStore();
                Arrays.fill( irows_, irow_ );
            }
            irow_++;
            return true;
        }

        public Object getCell( int icol ) {
            ColAccess<?> colAccess;
            try {
                colAccess = colAccesses_[ icol ];
            }
            catch ( NullPointerException e ) {
                throw new IllegalStateException( "next() not called" );
            }

            /* Make sure that we have read or skipped to the current
             * position before reading the value of the cell.
             * By doing it on demand here rather than during the next call,
             * we can avoid reading any data at all for those columns that
             * are never read in a given row group. */
            long nadv = irow_ - irows_[ icol ];
            if ( nadv > 0 ) {
                if ( nadv > 1 ) {
                    colAccess.skip( nadv - 1 );
                }
                colAccess.clear();
            }
            irows_[ icol ] = irow_;
            return colAccess.read();
        }

        public Object[] getRow() {
            Object[] row = new Object[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                row[ ic ] = getCell( ic );
            }
            return row;
        }

        public void close() throws IOException {
            pfr_.close();
        }

        /**
         * Reads the next row group and prepares column handlers
         * ready to supply its content.
         */
        private boolean nextReadStore() throws IOException {
            PageReadStore pageStore = pfr_.readNextRowGroup();
            if ( pageStore == null ) {
                return false;
            }
            iblock_++;
            assert pageStore.getRowCount() == blockSizes_[ iblock_ ];
            irGroupEnd_ += pageStore.getRowCount();
            ColumnReadStore crstore = getColumnReadStore( pageStore, schema_ );
            colAccesses_ = new ColAccess<?>[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                colAccesses_[ ic ] =
                    createColAccess( crstore, getInputColumn( ic ) );
            }
            return true;
        }

        /**
         * Skips over a given number of row blocks from the current position
         * without reading the data.
         *
         * @param  nb  number of blocks to skip
         */
        private void skipBlocks( int nb ) throws IOException {
            for ( int ib = 0; ib < nb; ib++ ) {
                if ( ! pfr_.skipNextRowGroup() ) {
                    throw new IOException( "Failed to skip row group" );
                }
                iblock_++;
                long nr = blockSizes_[ iblock_ ];
                irow_ += nr;
                irGroupEnd_ += nr;
            }
        }
    }

    /**
     * Creates a ColAccess for accessing a given column.
     *
     * @param   crstore  provides column data
     * @param   incol   identifies column to read
     * @return  column access object
     */
    private static <T> ColAccess<T>
            createColAccess( ColumnReadStore crstore,
                             final InputColumn<T> incol ) {

        /* <p>This implementation was written with reference to
         * org.apache.parquet.tools.command.DumpCommand.
         * There is a certain amount of guesswork involved in use
         * of the generally under-documented parquet-mr data access API. */

        /* Prepare to read. */
        ColumnDescriptor cdesc = incol.getColumnDescriptor();
        final int cdefmax = cdesc.getMaxDefinitionLevel();
        final Decoder<T> decoder = incol.createDecoder();
        final ColumnReader crdr = crstore.getColumnReader( cdesc );

        /* The readValue method is cleaner and a bit faster, so use that
         * where possible, but we don't have compile-time guarantees that
         * it's available, so fall back to advancing by hand if required. */
        final Runnable readValue = crdr instanceof ColumnReaderImpl
                                 ? ((ColumnReaderImpl) crdr)::readValue
                                 : () -> {
                                       decoder.clearValue();
                                       decoder.readItem( crdr );
                                   };
        return new ColAccess<T>() {
            private boolean hasValue_;
            private T value_;
            public void clear() {
                hasValue_ = false;
            }
            public void skip( long n ) {
                for ( long i = 0; i < n; i++ ) {

                    /* From the scant parquet-mr API documentation I would have
                     * thought that just a consume call here would be the
                     * right thing to do, but it seems you need to read
                     * the value as well. */
                    do {
                        if ( crdr.getCurrentDefinitionLevel() == cdefmax ) {
                            readValue.run();
                        }
                        crdr.consume();
                    } while ( crdr.getCurrentRepetitionLevel() > 0 );
                }
            }
            public T read() {
                if ( ! hasValue_ ) {
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
                    } while ( crdr.getCurrentRepetitionLevel() > 0 );
                    value_ = decoder.getValue();
                    hasValue_ = true;
                }
                return value_;
            }
        };
    }

    /**
     * Manages reading, caching and skipping values from a column.
     */
    private static interface ColAccess<T> {

        /**
         * Indicates that the most recently-read value will no longer be
         * required.
         */
        void clear();

        /**
         * Skips over a given number of column entries.
         *
         * @param  n  number of entries to skip.
         */
        void skip( long n );

        /**
         * Returns the current value, reading it from the column data
         * if it has not already been read.
         *
         * @return  entry value
         */
        T read();
    }
}
