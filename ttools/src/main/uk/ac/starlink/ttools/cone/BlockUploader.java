package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RandomStarTable;
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
    private final ServiceFindMode serviceMode_;
    private final boolean oneToOne_;
    private final boolean uploadEmpty_;
    private String truncationAdvice_;

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
     * @param   serviceMode   upload match mode
     * @param   oneToOne   true iff output rows match 1:1 with input rows
     * @param   uploadEmpty  determines behaviour if there are no input rows:
     *                       true means attempt the match anyway,
     *                       false means throw an IOException
     */
    public BlockUploader( UploadMatcher umatcher, int blocksize, long maxrec,
                          String outName, JoinFixAction uploadFixAct,
                          JoinFixAction remoteFixAct,
                          ServiceFindMode serviceMode, boolean oneToOne,
                          boolean uploadEmpty ) {
        umatcher_ = umatcher;
        blocksize_ = blocksize;
        maxrec_ = maxrec;
        outName_ = outName;
        uploadFixAct_ = uploadFixAct;
        remoteFixAct_ = remoteFixAct;
        serviceMode_ = serviceMode;
        oneToOne_ = oneToOne;
        uploadEmpty_ = uploadEmpty;
        if ( oneToOne_ && ! serviceMode.supportsOneToOne() ) {
            throw new IllegalArgumentException( "Mode " + serviceMode
                                              + " doesn't support 1:1" );
        }
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
     * <p>The row indices given by <code>ConeQueryRowSequence.getIndex</code>
     * calls associated with the supplied <code>qsFact</code> object
     * must correspond to the row indices in the supplied <code>inTable</code>.
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
        RowMapper<?> rowMapper = nrow >= 0 && nrow < Integer.MAX_VALUE
                               ? new IntegerMapper()
                               : new LongMapper();

        /* Perform an upload/match operation for each block of rows.
         * Each block takes its input from the next lot of rows from the
         * complete input query sequence, and appends its output to the
         * same single row store. */
        int nOverflow = 0;
        long totOut = 0;
        boolean done = false;
        int iblock = 0;
        while ( ! done && ( maxrec_ < 0 || totOut < maxrec_ ) ) {
            PreviewBlockSequence blockSeq =
                new PreviewBlockSequence( coneSeq, blocksize_ );
            boolean isFirst = iblock == 0;
            boolean hasNext = blockSeq.hasNext();
            if ( isFirst && ! hasNext && ! uploadEmpty_ ) {
                throw new IOException( "No candidate rows for upload match" );
            }
            if ( hasNext || isFirst ) {
                BlockSink blockSink = new BlockSink( rawResultStore, isFirst );
                long nRemain = maxrec_ >= 0 ? maxrec_ - totOut : -1;
                boolean over =
                    umatcher_.streamRawResult( blockSeq, blockSink, rowMapper,
                                               nRemain );
                int nIn = blockSeq.getProducedCount();
                long nOut = blockSink.getCount();
                nOverflow += over ? 1 : 0;
                logger_.info( "Match block " + ( iblock + 1 ) + ": "
                            + nIn + " uploaded, " + nOut + " received"
                            + ( over ? " (truncated)" : "" ) );
                if ( over ) {
                    logger_.warning( "Block " + ( iblock + 1 )
                                   + " truncated at " + nOut + " rows" );
                }
                totOut += nOut;
                iblock++;
            }
            done = ! hasNext;
        };
        int nblock = iblock;
        coneSeq.close();
        rawResultStore.endRows();
        if ( nOverflow > 0 ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "Truncations in " )
                .append( nOverflow )
                .append( "/" )
                .append( nblock )
                .append( " blocks" );
            if ( truncationAdvice_ != null ) {
                sbuf.append( "; " )
                    .append( truncationAdvice_ );
            }
            logger_.warning( sbuf.toString() );
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
        if ( serviceMode_.isRemoteUnique() && nblock > 1 ) {
            logger_.warning( "Bug: a remote row may appear up to "
                           + nblock + " times, not just once, in the result" );
        }

        /* Prepare objects that know what uploaded/result columns and rows
         * go where. */
        ColumnInfo[] rawCols = Tables.getColumnInfos( rawResult );
        ColumnInfo[] inCols = Tables.getColumnInfos( inTable );
        ColumnPlan cplan = umatcher_.getColumnPlan( rawCols, inCols );

        /* Deduplicate column names as required. */
        Tables.fixColumns( new ColumnInfo[][] { rawCols, inCols },
                           new JoinFixAction[] { remoteFixAct_,
                                                 uploadFixAct_ } );

        /* Combine the result table with the upload table to get the
         * final output. */
        int icolId = cplan.getResultIdColumnIndex();
        final StarTable outTable;

        /* In the 1:1 case, the output table has exactly one row for each
         * row of the input table.  Use a row index pairs array in
         * which the upload row index goes from 1 to nrUp, and the
         * result index is the index of the result row corresponding
         * to that input row if there is one, or -1 otherwise. */
        if ( oneToOne_ ) {
            int nrRes = Tables.checkedLongToInt( rawResult.getRowCount() );
            int nrUp = Tables.checkedLongToInt( inTable.getRowCount() );
            IrowPair[] irPairs = new IrowPair[ nrUp ];
            for ( int irUp = 0; irUp < nrUp; irUp++ ) {
                irPairs[ irUp ] = new IrowPair( -1, irUp );
            }
            for ( int irRes = 0; irRes < nrRes; irRes++ ) {
                Object idUp = rawResult.getCell( irRes, icolId );
                long irUp = rowIdToIndex( rowMapper, idUp );
                irPairs[ Tables.checkedLongToInt( irUp ) ]  =
                    new IrowPair( irRes, irUp );
            }
            outTable =
                new XmatchOutputTable( rawResult, inTable, cplan, irPairs );
        }

        /* In the non-1:1 case, the output table has one row for each
         * row in the raw result of the match.  Use a row index pairs
         * array in which the result index goes from 1 to nrRes, and the
         * upload row index is the upload row index associated with it. */
        else {
            int nrRes = Tables.checkedLongToInt( rawResult.getRowCount() );
            IrowPair[] irPairs = new IrowPair[ nrRes ];
            for ( int ir = 0; ir < nrRes; ir++ ) {
                Object idUp = rawResult.getCell( ir, icolId );
                irPairs[ ir ] =
                    new IrowPair( ir, rowIdToIndex( rowMapper, idUp ) );
            }
            outTable =
                new XmatchOutputTable( rawResult, inTable, cplan, irPairs );
        }

        /* Return the output table. */
        outTable.setName( outName_ );
        return outTable;
    }

    /**
     * Sets a string that can be issued to the user as additional advice
     * if there are truncations in block results.
     *
     * @param  truncationAdvice  user-readable advice string
     */
    public void setTruncationAdvice( String truncationAdvice ) {
        truncationAdvice_ = truncationAdvice;
    }

    /**
     * Turns a rowId value into an index into the upload table.
     *
     * @param   rowMapper   row mapper object
     * @param   id  row identifier object
     * @return  upload table row index
     */
    private static <I> long rowIdToIndex( RowMapper<I> rowMapper, Object id ) {
        return rowMapper.rowIdToIndex( rowMapper.getIdClass().cast( id ) );
    }

    /**
     * Table which combines a raw result generated by {@link #streamRawResult}
     * and an input table representing the uploaded data to give a
     * joined output table.
     */
    private static class XmatchOutputTable extends RandomStarTable {
        private final StarTable rawResult_;
        private final StarTable uploadTable_;
        private final ColumnPlan cplan_;
        private final IrowPair[] irPairs_;
        private final int ncol_;

        /**
         * Constructor.
         *
         * @param   rawResult  table generated by an earlier call to
         *                     <code>streamRawResult</code>
         * @param   uploadTable  table with rows corresponding to those
         *                       which generated the input query sequence;
         *                       this table must have random access
         * @param   cplan      rule for working out how to get the columns
         *                     in the output table from the rows in the
         *                     upload and raw result tables
         * @param   irPairs    list of row index pairs pointing to input
         *                     table rows, one for each row of this
         *                     output table
         */
        XmatchOutputTable( StarTable rawResult, StarTable uploadTable,
                           ColumnPlan cplan, IrowPair[] irPairs ) {
            rawResult_ = rawResult;
            uploadTable_ = uploadTable;
            cplan_ = cplan;
            irPairs_ = irPairs;
            ncol_ = cplan.getOutputColumnCount();
            getParameters().addAll( rawResult.getParameters() );
            if ( ! rawResult.isRandom() || ! uploadTable.isRandom() ) {
                throw new IllegalArgumentException( "Non-random input table" );
            }
        }

        public long getRowCount() {
            return irPairs_.length;
        }

        public int getColumnCount() {
            return ncol_;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            int loc = cplan_.getOutputColumnLocation( icol );
            return loc >= 0 ? rawResult_.getColumnInfo( loc )
                            : uploadTable_.getColumnInfo( -loc - 1 );
        }

        public Object getCell( long irow, int icol ) throws IOException {
            int loc = cplan_.getOutputColumnLocation( icol );
            IrowPair irPair = irPairs_[ Tables.checkedLongToInt( irow ) ];
            if ( loc >= 0 ) {
                long ir = irPair.irRes_;
                return ir >= 0 ? rawResult_.getCell( ir, loc ) : null;
            }
            else {
                long ir = irPair.irUp_;
                return ir >= 0 ? uploadTable_.getCell( ir, -loc - 1 ) : null;
            }
        }

        public Object[] getRow( long irow ) throws IOException {
            IrowPair irPair = irPairs_[ Tables.checkedLongToInt( irow ) ];
            long irRes = irPair.irRes_;
            long irUp = irPair.irUp_;
            Object[] resRow = irRes >= 0 ? rawResult_.getRow( irRes ) : null;
            Object[] upRow = irUp >= 0 ? uploadTable_.getRow( irUp ) : null;
            Object[] row = new Object[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                int loc = cplan_.getOutputColumnLocation( icol );
                if ( loc >= 0 ) {
                    if ( resRow != null ) {
                        row[ icol ] = resRow[ loc ];
                    }
                }
                else {
                    if ( upRow != null ) {
                        row[ icol ] = upRow[ -loc - 1 ];
                    }
                }
            }
            return row;
        }
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
     * Wraps a BlockSequence implementation to provide a method "hasNext",
     * which can tell before next is called what it will return.
     * This is useful so that an empty sequence can be identified before
     * it is passed to the rest of the upload machinery.
     * Use with caution: read the contract carefully.
     */
    private static class PreviewBlockSequence extends BlockSequence {
        private Boolean hasNext_;

        /**
         * Constructor.
         *
         * @param  baseSeq  base query sequence
         * @param  maxrec   maximum number of rows this sequence will produce
         */
        PreviewBlockSequence( ConeQueryRowSequence baseSeq, int maxrow ) {
            super( baseSeq, maxrow );
        }

        @Override public final boolean next() throws IOException {
            readyNext();
            boolean hasNext = hasNext_.booleanValue();
            hasNext_ = null;
            return hasNext;
        }

        /**
         * Returns the value of the next call to <code>next</code>.
         *
         * <p><bf>Note</bf>: this has the effect of advancing the underlying
         * sequence, so no data access should be performed between
         * <code>hasNext</code> and <code>next</code> calls.
         *
         * @return  value of the next invocation of <code>next</code>
         */
        public boolean hasNext() throws IOException {
            readyNext();
            return hasNext_.booleanValue();
        }

        /**
         * Attempts to advance this sequence to the next row to find
         * out whether there is one, but without calling this object's
         * <code>next</code> method.
         */
        private void readyNext() throws IOException {
            if ( hasNext_ == null ) {
                hasNext_ = Boolean.valueOf( super.next() );
            }
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
        private int nProduced_;
        private int nConsumed_;

        /**
         * Constructor.
         *
         * @param  baseSeq  base query sequence
         * @param  maxrec   maximum number of rows this sequence will produce
         */
        BlockSequence( ConeQueryRowSequence baseSeq, int maxrow ) {
            super( baseSeq );
            maxrow_ = maxrow;
        }

        @Override
        public boolean next() throws IOException {
            while ( nProduced_ < maxrow_ && super.next() ) {
                nConsumed_++;
                if ( isPossibleMatch() ) {
                    nProduced_++;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void close() throws IOException {
            // no action
        }

        /**
         * Returns number of rows read from the base sequence so far.
         *
         * @return  number of consumed rows
         */
        public int getConsumedCount() {
            return nConsumed_;
        }

        /**
         * Returns the number of rows dispensed by this sequence so far.
         *
         * @return  number of produced rows
         */
        public int getProducedCount() {
            return nProduced_;
        }

        /**
         * Indicates whether the current row can possibly match a sky position.
         *
         * @return  true iff current position has coordinates
         */
        private boolean isPossibleMatch() throws IOException {
            double ra = getRa();
            double dec = getDec();
            return ! Double.isNaN( ra )
                && dec >= -90 && dec <= +90;
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
     * RowMapper that uses Integer objects as IDs.
     */
    private static class IntegerMapper implements RowMapper<Integer> {
        public Class<Integer> getIdClass() {
            return Integer.class;
        }
        public long rowIdToIndex( Integer id ) {
            return id.longValue();
        }
        public Integer rowIndexToId( long index ) {
            return Integer.valueOf( toInt( index ) );
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
    private static class LongMapper implements RowMapper<Long> {
        public Class<Long> getIdClass() {
            return Long.class;
        }
        public long rowIdToIndex( Long id ) {
            return id.longValue();
        }
        public Long rowIndexToId( long index ) {
            return Long.valueOf( index );
        }
    }

    /**
     * Defines a pair of row indices: one for the raw result table,
     * and one for the upload table.  These tie together rows from the
     * two input tables that go to produce the output xmatch table.
     */
    private static class IrowPair {
        final long irRes_;
        final long irUp_;

        /**
         * Constructor.
         *
         * @param   irRes  row index in the raw result table
         * @param   irUp   row index in the upload table
         */
        IrowPair( long irRes, long irUp ) {
            irRes_ = irRes;
            irUp_ = irUp;
        }
    }
}
