package uk.ac.starlink.parquet;

import java.io.IOException;
import java.util.Arrays;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnReader;
import org.apache.parquet.column.ColumnReadStore;
import org.apache.parquet.column.impl.ColumnReaderImpl;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.schema.MessageType;
import uk.ac.starlink.table.RowSequence;
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
        return new ParquetRowSequence( getParquetFileReader() );
    }

    /**
     * RowSequence implementation for ParquetStarTable.
     */
    private class ParquetRowSequence implements RowSequence {

        final ParquetFileReader pfr_;
        final int ncol_;
        final MessageType schema_;
        final long[] irows_;
        ColAccess<?>[] colAccesses_;
        long irow_;
        long irGroupEnd_;

        /**
         * Constructor.
         *
         * @param  pfr  file reader
         */
        ParquetRowSequence( ParquetFileReader pfr ) {
            pfr_ = pfr;
            ncol_ = getColumnCount();
            schema_ = getSchema();
            irows_ = new long[ ncol_ ];
            irow_ = -1;
        }

        public boolean next() throws IOException {
            assert irow_ < irGroupEnd_;
            if ( irow_ + 1 == irGroupEnd_ ) {
                if ( ! nextReadStore() ) {
                    return false;
                }
                Arrays.fill( irows_, irow_ );
            }
            irow_++;
            return true;
        }

        public Object getCell( int icol ) {
            if ( irow_ >= 0 ) {
                ColAccess<?> colAccess = colAccesses_[ icol ];

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
            else {
                throw new IllegalStateException( "Next not called" );
            }
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
            irGroupEnd_ += pageStore.getRowCount();
            ColumnReadStore crstore = getColumnReadStore( pageStore, schema_ );
            colAccesses_ = new ColAccess<?>[ ncol_ ];
            for ( int ic = 0; ic < ncol_; ic++ ) {
                colAccesses_[ ic ] =
                    createColAccess( crstore, getInputColumn( ic ) );
            }
            return true;
        }
    }

    /**
     * Creates a ColAccess for accessing a given column.
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
                        if ( crdr.getCurrentDefinitionLevel() == cdefmax ) {
                            decoder.readItem( crdr );
                        }
                        // I thought that I ought to be passing a null along
                        // in this case.  However, it does the wrong thing
                        // at least for some arrays.  Hmm.
                        // else {
                        //     decoder.readNull();
                        // }
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
