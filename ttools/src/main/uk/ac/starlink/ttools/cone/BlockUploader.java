package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Works with an UploadMatcher dividing the input table into chunks and
 * uploading them separately to produce an arbitrarily large result
 * while each upload/match operation is of a limited size.
 *
 * @author   Mark Taylor
 * @since    15 May 2014
 */
public class BlockUploader {

    private final UploadMatcher umatcher_;
    private final int blocksize_;
    private final long maxrec_;
    private final String outName_;
    private final JoinFixAction uploadFixAct_;
    private final JoinFixAction remoteFixAct_;
    private final boolean remoteUnique_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param   umatcher  upload matcher
     * @param   blocksize  maximum number of rows per uploaded block
     * @param   maxrec     maximum number of output rows accepted in total
     * @param   outName   name of output table
     * @param   uploadFixAct  name deduplication policy for upload table
     * @param   remoteFixAct  name deduplication policy for remote table
     * @param   remoteUnique  true iff a requirement of the match is that
     *                        each remote row appears only once in the result
     */
    public BlockUploader( UploadMatcher umatcher, int blocksize, long maxrec,
                          String outName, JoinFixAction uploadFixAct,
                          JoinFixAction remoteFixAct, boolean remoteUnique ) {
        umatcher_ = umatcher;
        blocksize_ = blocksize;
        maxrec_ = maxrec;
        outName_ = outName;
        uploadFixAct_ = uploadFixAct;
        remoteFixAct_ = remoteFixAct;
        remoteUnique_ = remoteUnique;
        if ( blocksize <= 0 ) {
            throw new IllegalArgumentException( "Non-positive blocksize" );
        }
    }

    /**
     * Performs an upload join in blocks.
     *
     * <p>As currently implemented, the input table needs to have random
     * access, and the output table has random access.
     * It would be possible to stream on input and output to some extent,
     * though each input chunk will still have to be random access.
     *
     * @param  inTable  input table, must have random access
     * @param  qsFact   object to generate positional queries when applied
     *                  to a table
     * @param  storage  storage policy for storing raw result table
     */
    public StarTable runMatch( StarTable inTable, QuerySequenceFactory qsFact,
                               StoragePolicy storage ) throws IOException {
        if ( ! inTable.isRandom() ) {
            throw new IllegalArgumentException( "non-random input table" );
        }

        /* Set up input and output streams.  It's important to get the input
         * rows from a single RowSequence rather than by random access
         * so that monitor filters can report on progress. */
        ConeQueryRowSequence coneSeq = qsFact.createQuerySequence( inTable );
        RowStore rawResultStore = storage.makeRowStore();

        /* Work out how to do the blocking. */
        long nrow = inTable.getRowCount();
        long nb = 1 + ( nrow - 1 ) / blocksize_;
        int nblock = (int) nb;
        if ( nblock != nb ) {
            throw new IllegalArgumentException( "Too many blocks: " + nb );
        }
        OffsetMapperFactory mapperFact = new OffsetMapperFactory( nrow );

        /* Perform an upload/match operation for each block of rows.
         * Each block takes its input from the next lot of rows from the
         * complete input query sequence, and appends its output to the
         * same single row store. */
        int nOverflow = 0;
        long totOut = 0;
        boolean done = false;
        for ( int iblock = 0;
              iblock < nblock && ( maxrec_ < 0 || totOut < maxrec_ );
              iblock++ ) {
            BlockSequence blockSeq = new BlockSequence( coneSeq, blocksize_ );
            BlockSink blockSink = new BlockSink( rawResultStore, iblock == 0 );
            long nRemain = maxrec_ >= 0 ? maxrec_ - totOut : -1;

            /* Generate the raw result using an RowMapper which uses row
             * indices offset by the starting row index of this block.
             * That allows us to assemble an output raw result table that
             * has been built in blocks, but looks the same as if it
             * had been built by a single upload/match operation. */
            RowMapper blockMapper =
                mapperFact.createOffsetMapper( iblock * blocksize_ );
            boolean over =
                umatcher_.streamRawResult( blockSeq, blockSink, blockMapper,
                                           nRemain );
            int nIn = blockSeq.getCount();
            long nOut = blockSink.getCount();
            done = blockSeq.isBaseDone();
            nOverflow += over ? 1 : 0;
            logger_.info( "Match block " + ( iblock + 1 ) + "/" + nblock + ": "
                        + nIn + " uploaded, " + nOut + " received"
                        + ( over ? " (truncated)" : "" ) );
            totOut += nOut;
        };
        coneSeq.close();
        rawResultStore.endRows();
        if ( nOverflow > 0 ) {
            logger_.warning( "Truncations in " + nOverflow + "/" + nblock
                           + " blocks" );
        }
        StarTable rawResult = rawResultStore.getStarTable();

        /* There is a problem here for best-remote matching.
         * If the intent is to find the local catalogue row that best
         * matches each remote catalogue row, the same remote catalogue
         * row may show up once per block, rather than just once per
         * full match.  So in that case I ought to deduplicate the rows.
         * This need not be very computationally intensive since there
         * won't be many of them, but to identify which ones they are
         * I need from each raw result row: (1) an identifier or some
         * other way to tell that two rows are the same (full-row hash?)
         * and (2) a way to determine the score (match distance)
         * associated with that result row.  Either the remote catalogue
         * RA/Dec or the angDist columns would do.
         * If I had all that I could just add a deduplication step here. */
        if ( remoteUnique_ && nblock > 1 ) {
            logger_.warning( "Bug: a remote row may appear up to "
                           + nblock + " times, not just once, in the result" );
        }

        /* Deduplicate column names as required. */
        ColumnInfo[] rawCols = Tables.getColumnInfos( rawResult );
        ColumnInfo[] inCols = Tables.getColumnInfos( inTable );
        Tables.fixColumns( new ColumnInfo[][] { rawCols, inCols },
                           new JoinFixAction[] { remoteFixAct_,
                                                 uploadFixAct_ } );

        /* Generate the actual output table from the raw result and the
         * input table, and return it. */
        StarTable outTable =
            umatcher_.createOutputTable( new ColTable( rawResult, rawCols ),
                                         new ColTable( inTable, inCols ),
                                         mapperFact.createOffsetMapper( 0 ) );
        outTable.setName( outName_ );
        return outTable;
    }

    /**
     * Wrapper StarTable implementation that allows custom substitution
     * of column metadata.
     */
    private static class ColTable extends WrapperStarTable {
        private final ColumnInfo[] colInfos_;

        /**
         * Constructor.
         *
         * @param  base  base table
         * @param  array of column metadata objects, must be the same length
         *         as the column count
         */
        ColTable( StarTable base, ColumnInfo[] colInfos ) {
            super( base );
            colInfos_ = colInfos;
        }
        public ColumnInfo getColumnInfo( int ic ) {
            return colInfos_[ ic ];
        }
    }

    /**
     * ConeQueryRowSequence implementation that wraps an existing one
     * but returns only a limited number of its rows.
     * If maxrec rows are dispensed, subsequent calls to <code>next</code>
     * will return false.  The base sequence is never <code>close</code>d.
     */
    private static class BlockSequence extends WrapperQuerySequence {
        private final int maxrow_;
        private int nrow_;
        private boolean baseDone_;

        /**
         * Constructor.
         *
         * @param  baseSeq  base query sequence
         * @param  maxrec   maximum number of rows this sequence will dispense
         */
        BlockSequence( ConeQueryRowSequence baseSeq, int maxrow ) {
            super( baseSeq );
            maxrow_ = maxrow;
        }
        @Override
        public boolean next() throws IOException {
            if ( nrow_ < maxrow_ ) {
                if ( super.next() ) {
                    nrow_++;
                    return true;
                }
                else {
                    baseDone_ = true;
                    return false;
                }
            }
            else {
                return false;
            }
        }
        @Override
        public void close() throws IOException {
            // no action
        }

        /**
         * Returns the number of rows dispensed by this sequence.
         *
         * @return  row count
         */
        public int getCount() {
            return nrow_;
        }

        /**
         * Indicates whether the base sequence is known to have terminated.
         *
         * @return  true iff base sequence has no more rows
         */
        public boolean isBaseDone() {
            return baseDone_;
        }
    }

    /**
     * TableSink implementation that wraps another sink and appends rows to it.
     * The base sink never has <code>endRows</code> called on it.
     */
    private static class BlockSink implements TableSink {
        private final TableSink baseSink_;
        private final boolean isFirst_;
        private long nrow_;

        /**
         * Constructor.
         *
         * @param  baseSink  base table sink
         * @param  isFirst   true iff this is the first to write to the base;
         *                   in that case metadata will be written
         */
        public BlockSink( TableSink baseSink, boolean isFirst ) {
            baseSink_ = baseSink;
            isFirst_ = isFirst;
        }
        public void acceptMetadata( StarTable meta )
                throws TableFormatException {
            if ( isFirst_ ) {
                baseSink_.acceptMetadata( meta );
            }
        }
        public void acceptRow( Object[] row ) throws IOException {
            baseSink_.acceptRow( row );
            nrow_++;
        }
        public void endRows() {
            // no action
        }

        /**
         * Returns the number of rows written to this sink.
         *
         * @return   row count
         */
        public long getCount() {
            return nrow_;
        }
    }

    /**
     * Produces RowMapper instances with row offsets.
     */
    private static class OffsetMapperFactory {
        private final boolean useInt_;

        /**
         * Constructor.
         *
         * @param  nrow  maximum number of rows for which this mapper
         *               will be used; may be negative if not known
         */
        OffsetMapperFactory( long nrow ) {
            useInt_ = nrow >= 0 && nrow < Integer.MAX_VALUE;
        }

        /**
         * Returns a mapper with a given row offset.
         *
         * @param  index0  row index of the first row the mapper will
         *                 be used for
         * @return  offset mapper
         */
        RowMapper<?> createOffsetMapper( long index0 ) {
            return useInt_ ? new IntegerOffsetMapper( index0 + 1 )
                           : new LongOffsetMapper( index0 + 1 );
        }
    }

    /**
     * RowMapper that uses Integer objects as IDs.
     */
    private static class IntegerOffsetMapper implements RowMapper<Integer> {
        private final int index0_;

        /**
         * Constructor.
         *
         * @param  index0  row index offset
         */
        IntegerOffsetMapper( long index0 ) {
            index0_ = toInt( index0 );
        }
        public Class<Integer> getIdClass() {
            return Integer.class;
        }
        public long rowIdToIndex( Integer id ) {
            return id.longValue() - index0_;
        }
        public Integer rowIndexToId( long index ) {
            return new Integer( toInt( index + index0_ ) );
        }

        /**
         * Casts a long to an int.  If everything else is working correctly,
         * it should never be presented with an int that is too big to cast.
         *
         * @param  lval  long value
         * @return  int value
         */
        private int toInt( long lval ) {
            int ival = (int) lval;
            if ( ival != lval ) {
                throw new IllegalArgumentException( "Should have used long" );
            }
            return ival;
        }
    }

    /**
     * RowMapper that uses Long objects as IDs.
     */
    private static class LongOffsetMapper implements RowMapper<Long> {
        private final long index0_;

        /**
         * Constructor.
         *
         * @param  index0  row index offset
         */
        LongOffsetMapper( long index0 ) {
            index0_ = index0;
        }
        public Class<Long> getIdClass() {
            return Long.class;
        }
        public long rowIdToIndex( Long id ) {
            return id.longValue() - index0_;
        }
        public Long rowIndexToId( long index ) {
            return new Long( index + index0_ );
        }
    }
}
