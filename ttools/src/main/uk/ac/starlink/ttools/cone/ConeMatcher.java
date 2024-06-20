package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowPipe;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.SelectorStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.filter.AddColumnsTable;
import uk.ac.starlink.ttools.filter.CalculatorColumnSupplement;
import uk.ac.starlink.ttools.filter.ColumnSupplement;
import uk.ac.starlink.ttools.filter.PermutedColumnSupplement;
import uk.ac.starlink.ttools.filter.SupplementData;
import uk.ac.starlink.ttools.func.CoordsDegrees;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * TableProducer which does the work for a multiple cone search-type
 * sky crossmatch operation.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2007
 */
public class ConeMatcher {

    private final ConeSearcher coneSearcher_;
    private final ConeErrorPolicy errAct_;
    private final TableProducer inProd_;
    private final QuerySequenceFactory qsFact_;
    private final int parallelism_;
    private final boolean bestOnly_;
    private final Coverage coverage_;
    private final boolean includeBlanks_;
    private final boolean distFilter_;
    private final String copyColIdList_;
    private final JoinFixAction inFixAct_;
    private final JoinFixAction coneFixAct_;
    private final String distanceCol_;
    private boolean streamOutput_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /** Default metadata for distance column. */
    private static final ValueInfo DISTANCE_INFO =
        new DefaultValueInfo( "Distance", Double.class,
                              "Angular separation between query position and "
                            + "result position" );
    static {
        ((DefaultValueInfo) DISTANCE_INFO).setUnitString( "deg" );
        ((DefaultValueInfo) DISTANCE_INFO).setUCD( "pos.angDistance" );
    }

    /**
     * Convenience constructor which selects default values for most options.
     *
     * @param   coneSearcher   cone search implementation
     * @param   errAct   defines action on cone search invocation error
     * @param   inProd   source of input table (containing each crossmatch
     *                   specification)
     * @param   qsFact    object which can produce a ConeQueryRowSequence
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
     */
    public ConeMatcher( ConeSearcher coneSearcher, ConeErrorPolicy errAct,
                        TableProducer inProd, QuerySequenceFactory qsFact,
                        boolean bestOnly ) {
        this( coneSearcher, errAct, inProd, qsFact, bestOnly, null, true, false,
              1, "*", DISTANCE_INFO.getName(), JoinFixAction.NO_ACTION,
              JoinFixAction.makeRenameDuplicatesAction( "_1", false, false ) );
    }

    /**
     * Full-functioned constructor.
     *
     * @param   coneSearcher   cone search implementation
     * @param   errAct   defines action on cone search invocation error
     * @param   inProd   source of input table (containing each crossmatch
     *                   specification)
     * @param   qsFact    object which can produce a ConeQueryRowSequence
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
     * @param   coverage  coverage for cone searcher, or null
     * @param   includeBlanks  true iff a row is to be output for input rows
     *                         for which the cone search has no matches
     * @param   distFilter true to perform post-query filtering on results
     *                     based on the distance between the query position
     *                     and the result row position
     * @param   parallelism  number of threads to concurrently execute matches -
     *                       only &gt;1 if coneSearcher is thread-safe
     * @param   copyColIdList  space-separated list of column identifiers for
     *                         columns to be copied to the output table,
     *                         "*" for all columns
     * @param   distanceCol  name of column to hold position separation values,
     *                       or null for no separation column
     * @param   inFixAct   column name deduplication action for input table
     * @param   coneFixAct column name deduplication action for result
     *                     of cone searches
     */
    public ConeMatcher( ConeSearcher coneSearcher, ConeErrorPolicy errAct,
                        TableProducer inProd,
                        QuerySequenceFactory qsFact, boolean bestOnly,
                        Coverage coverage, boolean includeBlanks,
                        boolean distFilter, int parallelism,
                        String copyColIdList, String distanceCol,
                        JoinFixAction inFixAct, JoinFixAction coneFixAct ) {
        coneSearcher_ = coneSearcher;
        errAct_ = errAct;
        inProd_ = inProd;
        qsFact_ = qsFact;
        bestOnly_ = bestOnly;
        coverage_ = coverage;
        includeBlanks_ = includeBlanks;
        distFilter_ = distFilter;
        parallelism_ = parallelism;
        copyColIdList_ = copyColIdList;
        distanceCol_ = distanceCol;
        inFixAct_ = inFixAct;
        coneFixAct_ = coneFixAct;
    }

    /**
     * Determines whether this object's {@link #createConeWorker} method will 
     * produce a one-read-only table or not.  If set true, then the output
     * table is good for only a single read (<code>getRowSequence</code>
     * may be called only once).
     * The default is false.
     *
     * @param  streamOutput  whether output is streamed
     */
    public void setStreamOutput( boolean streamOutput ) {
        streamOutput_ = streamOutput;
    }

    /**
     * Returns an object which can compute the multi-cone result.
     * The result is a join between the input table and
     * the table on which the cone searches are defined.
     * See the <code>ConeWorker</code> documentation for how to use
     * the returned object.
     *
     * <p><strong>Note</strong></p>: if the <code>streamOut</code>
     * attribute of this ConeMatcher has been set the table produced by
     * the returned worker will be one-read-only, designed for streaming.
     *
     * @return   cone worker which can produce the result table
     */
    public ConeWorker createConeWorker() throws IOException, TaskException {
        StarTable inTable = inProd_.getTable();
        ConeQueryRowSequence querySeq = qsFact_.createQuerySequence( inTable );
        if ( coverage_ != null ) {
            try {
                coverage_.initCoverage();
            }
            catch ( IOException e ) {
                logger_.warning( "Coverage initialisation failed: " + e );
            }
        }
        final ConeResultRowSequence resultSeq;
        if ( parallelism_ == 1 ) {
            resultSeq = new SequentialResultRowSequence( querySeq,
                                                         coneSearcher_,
                                                         errAct_,
                                                         coverage_,
                                                         bestOnly_, distFilter_,
                                                         distanceCol_ ) {
                   public void close() throws IOException {
                       super.close();
                       coneSearcher_.close();
                   }
               };
        }
        else {
            resultSeq = new ParallelResultRowSequence( querySeq,
                                                       coneSearcher_,
                                                       errAct_,
                                                       coverage_,
                                                       bestOnly_, distFilter_,
                                                       distanceCol_,
                                                       parallelism_ ) {
                public void close() throws IOException {
                    super.close();
                    coneSearcher_.close();
                }
            };
        }
        int[] iCopyCols = ( copyColIdList_ == null ||
                            copyColIdList_.trim().length() == 0 )
                        ? new int[ 0 ]
                        : new ColumnIdentifier( inTable )
                         .getColumnIndices( copyColIdList_ );
        return new ConeWorker( inTable, resultSeq, iCopyCols, includeBlanks_,
                               ( distanceCol_ != null &&
                                 distanceCol_.trim().length() > 0 ) ? 1 : 0,
                               inFixAct_, coneFixAct_, JoinFixAction.NO_ACTION,
                               streamOutput_ );
    }

    /**
     * Performs a cone search and returns the resulting table with
     * appropriate filtering operations applied.
     * The resulting table may contain fewer rows than the output of the
     * actual query; if <code>bestOnly</code> is true, only the best match
     * will be included, and if <code>distFilter</code> is true, then only
     * those rows whose sky position falls strictly within the specified
     * search radius will be included.
     *
     * <p>If a non-null <code>distanceCol</code> parameter is supplied,
     * the final column in the table will contain the angle in degrees
     * between the region centre and the position described in the row.
     *
     * <p>If no records in the cone are found, the return value may either
     * be null or (preferably) an empty table with the correct columns.
     *
     * @param   coneSearcher   cone search implementation
     * @param   errAct   defines action on cone search invocation error
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
     * @param   distFilter true to perform post-query filtering on results
     *                     based on the distance between the query position
     *                     and the result row position
     * @param   distanceCol  name of column to hold distance information
     *                       int output table, or null
     * @param   ra0   right ascension in degrees of region centre
     * @param   dec0  declination in degrees of region centre
     * @param   sr    search radius in degrees
     * @return   filtered result table, or null
     */
    public static StarTable getConeResult( ConeSearcher coneSearcher,
                                           ConeErrorPolicy errAct,
                                           boolean bestOnly, boolean distFilter,
                                           String distanceCol,
                                           final double ra0, final double dec0,
                                           final double sr )
            throws IOException {

        /* Validate parameters. */
        if ( Double.isNaN( ra0 ) || Double.isNaN( dec0 ) ) {
            logger_.warning( "Invalid search parameters" );
            return null;
        }
        distFilter = distFilter && sr > 0.0;

        /* Perform the cone search itself. */
        logger_.info( "Cone: ra=" + ra0 + "; dec=" + dec0 + "; sr=" + sr );
        StarTable result;
        try {
            result = errAct.performConeSearch( coneSearcher, ra0, dec0, sr );
        }
        catch ( InterruptedException e ) {
            throw new IOException( "Thread interrupted" );
        }
        if ( result == null ) {
            return null;
        }

        /* Work out the columns which represent RA and Dec in the result. */
        final int ira = coneSearcher.getRaIndex( result );
        final int idec = coneSearcher.getDecIndex( result );

        /* Add a column for distance information. */
        ColumnInfo distInfo = new ColumnInfo( DISTANCE_INFO );
        if ( distanceCol != null && distanceCol.trim().length() > 0 ) {
            distInfo.setName( distanceCol );
        }
        StarTable resultWithDistance =
            addDistanceColumn( result, ira, idec, ra0, dec0, distInfo );
        assert resultWithDistance.getColumnCount() == 
               result.getColumnCount() + 1;
        final int idist = resultWithDistance.getColumnCount() - 1;
        assert resultWithDistance.getColumnInfo( idist ).getName()
                                       .equals( distInfo.getName() ); 

        /* Prepare a table which contains only the rows of interest. */
        StarTable filteredResultWithDistance;

        /* If we can't calculate distances for some reason, we just have 
         * to use all the rows.  One could argue that if bestOnly has
         * been chosen in this case a single row should be selected at
         * random, but this would risk the user not seeing/ignoring the
         * log message and thinking he had the best row when it wasn't,
         * so play it safe.  This shouldn't normally happen in any case. */
        if ( ira < 0 || idec < 0 ) {
            logger_.warning( "Can't locate RA/DEC in output table - "
                           + "no post-filtering or distance calculation" );
            filteredResultWithDistance = resultWithDistance;
        }

        /* If only a single output row per input row has been requested,
         * identify the best match and prepare a table containing only 
         * that one. */
        else if ( bestOnly ) {
            RowSequence rseq = resultWithDistance.getRowSequence();
            double bestDist = Double.NaN;
            Object[] bestRow = null;
            while ( rseq.next() ) {
                Object distObj = rseq.getCell( idist );
                assert distObj == null || distObj instanceof Double;
                if ( distObj instanceof Number ) {
                    double dist = ((Number) distObj).doubleValue();
                    if ( ( dist <= sr || ! distFilter ) &&
                         ( dist < bestDist || Double.isNaN( bestDist ) ) ) {
                        bestDist = dist;
                        bestRow = rseq.getRow().clone();
                    }
                }
            }
            filteredResultWithDistance =
                new RowListStarTable( resultWithDistance );
            if ( ! Double.isNaN( bestDist ) ) {
                ((RowListStarTable) filteredResultWithDistance)
                                   .addRow( bestRow );
            }
        }

        /* Otherwise return a table which ensures that all the rows are
         * in the search region.  This filtering is necessary since the
         * ConeSearcher contract allows the return of supersets of the
         * requested region. */
        else if ( distFilter ) {
            filteredResultWithDistance =
                    new SelectorStarTable( resultWithDistance ) {
                public boolean isIncluded( RowSequence rseq )
                        throws IOException {
                    Object distObj = rseq.getCell( idist );
                    assert distObj == null || distObj instanceof Double;
                    return ( distObj instanceof Number )
                        && ((Number) distObj).doubleValue() <= sr;
                }
            };
        }

        /* If no filtering is required, just pass the table through. */
        else {
            filteredResultWithDistance = resultWithDistance;
        }

        /* Return the filtered table with or without the distance column
         * as requested. */
        if ( distanceCol != null && distanceCol.trim().length() > 0 ) {
            return filteredResultWithDistance;
        }
        else {
            assert idist == filteredResultWithDistance.getColumnCount() - 1;
            assert idist == result.getColumnCount();
            int[] colMap = new int[ idist ];
            for ( int icol = 0; icol < idist; icol++ ) {
                colMap[ icol ] = icol;
            }
            return new ColumnPermutedStarTable( filteredResultWithDistance,
                                                colMap, true );
        }
    }

    /**
     * Returns a table which is like the input table but contains a single
     * column appended after the input ones containing the distance from
     * a given point on the sky.  If the values cannot be calculated for
     * some reason, the column will still be present but will be full of
     * <code>Double.NaN</code>s.
     *
     * @param  inTable  input table
     * @param  ira  index of column in inTable giving right ascension
     *              - may be -1 if unknown
     * @param  idec index of column in inTable giving declination
     *              - may be -1 if unknown
     * @param  ra0  right ascension in degrees of position to calculate
     *              distances from
     * @param  dec0 declination in degrees of position to calculate 
     *              distances from
     * @param  distInfo  metadata for distance column
     * @return  table with additional distance column
     */
    private static StarTable addDistanceColumn( StarTable inTable,
                                                int ira, int idec,
                                                final double ra0,
                                                final double dec0,
                                                ColumnInfo distInfo ) {
        if ( ! distInfo.getContentClass().isAssignableFrom( Double.class ) ) {
            throw new IllegalArgumentException( "Bad column info type" );
        }
        int ncolIn = inTable.getColumnCount();
        ColumnInfo[] distCols = new ColumnInfo[] { distInfo };
        final ColumnSupplement distSup;
        if ( ira < 0 || idec < 0 ) {
            Object[] blankRow = new Object[] { Double.valueOf( Double.NaN ) };
            distSup = new ConstantColumnSupplement( distCols, blankRow );
        }
        else {
            final double raUnit =
                getAngleUnit( inTable.getColumnInfo( ira ).getUnitString() );
            final double decUnit =
                getAngleUnit( inTable.getColumnInfo( idec ).getUnitString() );
            ColumnSupplement radecSup =
                new PermutedColumnSupplement( inTable,
                                              new int[] { ira, idec } );
            distSup = new CalculatorColumnSupplement( radecSup, distCols ) {
                protected Object[] calculate( Object[] inValues ) {
                    double ra1 = getDouble( inValues[ 0 ] ) * raUnit;
                    double dec1 = getDouble( inValues[ 1 ] ) * decUnit;
                    double dist = CoordsDegrees
                                 .skyDistanceDegrees( ra0, dec0, ra1, dec1 );
                    return new Object[] { Double.valueOf( dist ) };
                }
            };
        }
        return new AddColumnsTable( inTable, distSup );
    }

    /**
     * Returns the conversion factor from a named angular unit to degrees.
     *
     * @param   unitString  unit string value
     * @return   factor to multiply angles by to get degrees
     */
    private static double getAngleUnit( String unitString ) {
        if ( unitString == null || unitString.trim().length() == 0 ) {
            return 1.0;
        }
        else if ( unitString.toLowerCase().startsWith( "deg" ) ) {
            return 1.0;
        }
        else if ( unitString.toLowerCase().startsWith( "rad" ) ) {
            return 180. / Math.PI;
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Object which produces the result table.
     * It performs the individual cone searches and writes the results
     * down a pipe from which it will be read asynchronously for output.
     *
     * <p>To use an instance of this class, it is necessary to
     * call its <code>run</code> method in a separate thread.
     * The <code>getTable</code> method may be called before
     * the <code>run</code> method has completed (or even started),
     * and will return a table whose rows may be streamed.
     *
     * <p>The run method checks for interruptions, so interrupting the
     * thread in which it runs will cause it to stop consuming resources.
     *
     * <p>This code was originally written for J2SE1.4.
     * There may be less baroque ways of achieving the same effect using
     * the J2SE5 java.util.concurrent classes (<code>BlockingQueue</code>).
     */
    public static class ConeWorker implements Runnable, TableProducer {
        private final StarTable inTable_;
        private final ConeResultRowSequence resultSeq_;
        private final int[] iCopyCols_;
        private final boolean includeBlanks_;
        private final int extraCols_;
        private final JoinFixAction inFixAct_;
        private final JoinFixAction coneFixAct_;
        private final JoinFixAction extrasFixAct_;
        private final boolean strmOut_;
        private final RowPipe rowPipe_;

        /**
         * Constructor.
         *
         * @param   inTable  input table
         * @param   resultSeq  cone search result row sequence, positioned at
         *                     the start of the data
         * @param   iCopyCols  indices of columns from the input table to
         *                     be copied to the output table
         * @param   includeBlanks true iff a row is to be output for input rows
         *                        with an empty cone search
         * @param   extraCols  number of columns at the end of the column list
         *                     which correspond to neither the input nor the
         *                     searched tables
         * @param   inFixAct   column name deduplication action for input table
         * @param   coneFixAct column name deduplication action for result
         *                     of cone searches
         * @param   extrasFixAct  column name deduplication action for 
         *                        extra columns
         * @param   strmOut  whether output is streamed
         */
        private ConeWorker( StarTable inTable,
                            ConeResultRowSequence resultSeq, int[] iCopyCols,
                            boolean includeBlanks,
                            int extraCols, JoinFixAction inFixAct,
                            JoinFixAction coneFixAct,
                            JoinFixAction extrasFixAct,
                            boolean strmOut ) {
            inTable_ = inTable;
            resultSeq_ = resultSeq;
            iCopyCols_ = iCopyCols;
            includeBlanks_ = includeBlanks;
            extraCols_ = extraCols;
            inFixAct_ = inFixAct;
            coneFixAct_ = coneFixAct;
            extrasFixAct_ = extrasFixAct;
            strmOut_ = strmOut;
            rowPipe_ = new OnceRowPipe();
        }

        /**
         * Returns the result table.  It will block until <code>run</code>
         * has at least started, but not necessarily until it has completed.
         *
         * @return   result table
         */
        public StarTable getTable() throws IOException {
            StarTable streamTable = rowPipe_.waitForStarTable();
            return strmOut_ ? streamTable
                            : Tables.randomTable( streamTable );
        }

        /**
         * Does the work of feeding the rows to the result table.
         * This method checks regularly for interruptions and will
         * stop running if the thread is interrupted, causing a
         * read error at the other end of the pipe.
         */
        public void run() {
            try {
                multiCone();
            }
            catch ( IOException e ) {
                rowPipe_.setError( e );
            }
            catch ( Throwable e ) {
                rowPipe_.setError( (IOException)
                                    new IOException( "Read error: "
                                                   + e.getMessage() )
                                   .initCause( e ) );
            }
            finally {
                try {
                    rowPipe_.endRows();
                }
                catch ( IOException e ) {
                    // never mind
                }
                try {
                    resultSeq_.close();
                }
                catch ( IOException e ) {
                    // never mind
                }
            }
        }

        /**
         * Performs the actual multiple cone searches to produce the result
         * and writes the result down a pipe.
         */
        private void multiCone() throws IOException {
            int ncIn = iCopyCols_.length;
            int ncCone = -1;
            List<Object[]> inQueue = null;

            /* Loop over rows of the input table. */
            for ( int irow = 0; resultSeq_.next(); irow++ ) {

                /* Perform the cone search for this row. */
                StarTable result = resultSeq_.getConeResult();
                if ( result != null ) {

                    /* If this is the first entry we've got, acquire the 
                     * metadata (most importantly column descriptions) from it 
                     * and use that to initialise the output row pipe. */
                    int nc = result.getColumnCount();
                    if ( ncCone < 0 ) {
                        ncCone = nc;
                        rowPipe_.acceptMetadata( getMetadata( result ) );

                        /* If there are queued rows waiting to find out
                         * what the result metadata looks like, output
                         * them now. */
                        if ( inQueue != null ) {
                            for ( Object[] inRow : inQueue ) {
                                Object[] outRow = new Object[ ncIn + ncCone ];
                                System.arraycopy( inRow, 0, outRow, 0, ncIn );
                                rowPipe_.acceptRow( outRow );
                            }
                            inQueue = null;
                        }
                    }
                    else if ( nc != ncCone ) {
                        String msg = "Inconsistent column counts "
                                   + "from different cone search invocations"
                                   + " (" + nc + " != " + ncCone + ")";
                        throw new IOException( msg );
                    }

                    /* Append one row to the output for each row in the
                     * cone search result.  Each row consists of the copied
                     * columns from the input table followed by all the 
                     * columns from the cone search result. */
                    RowSequence rSeq = result.getRowSequence();
                    int nr = 0;
                    try {
                        while ( rSeq.next() ) {
                            nr++;

                            /* Append the actual rows. */
                            Object[] row = new Object[ ncIn + ncCone ];
                            copyInCells( row );
                            for ( int ic = 0; ic < ncCone; ic++ ) {
                                row[ ncIn + ic ] = rSeq.getCell( ic );
                            }
                            rowPipe_.acceptRow( row );
                        }
                    }
                    finally {
                        rSeq.close();
                    }

                    /* In case of no result rows, write an output row with
                     * blank result columns only if we have been asked
                     * to do so. */
                    if ( nr == 0 && includeBlanks_ ) {
                        Object[] row = new Object[ ncIn + ncCone ];
                        copyInCells( row );
                        rowPipe_.acceptRow( row );
                    }

                    /* Log number of rows successfully appended. */
                    logger_.info( "Row " + irow + ": got " + nr
                                + ( ( nr == 1 ) ? " match" : " matches" ) );
                }

                /* Null result.  Services shouldn't do this, but if they
                 * do, assume it means no results found. */
                else {

                    /* If we've been asked to write rows for empty results,
                     * we have to do some work. */
                    if ( includeBlanks_ ) {

                        /* If we don't know what the output table columns are
                         * yet, just store the input data for later. */
                        if ( ncCone < 0 ) {
                            Object[] inRow = new Object[ ncIn ];
                            copyInCells( inRow );
                            if ( inQueue == null ) {
                                inQueue = new ArrayList<Object[]>();
                            }
                            inQueue.add( inRow );
                        }

                        /* If we do know the output table columns, write
                         * this row plus empty cells. */
                        else {
                            Object[] row = new Object[ ncIn + ncCone ];
                            copyInCells( row );
                            rowPipe_.acceptRow( row );
                        }
                    }

                    /* Log match failure. */
                    logger_.info( "Row " + irow + ": got no matches" );
                }
            }

            /* If the output table was never initialised, do it here with a
             * dummy table.  Ideally this will not happen, but it might do
             * in the case that no matches were found AND the ConeSearcher
             * implementation returns nulls instead of empty tables for
             * empty searches. */
            if ( ncCone < 0 ) {
                String msg = "No results were found and no table metadata "
                           + "could be gathered.  Sorry.";
                logger_.warning( msg );
                StarTable result0 = new EmptyStarTable();
                ValueInfo msgInfo =
                    new DefaultValueInfo( "Message", String.class,
                                          "Multicone execution report" );
                result0.getParameters()
                       .add( new DescribedValue( msgInfo, msg ) );
                rowPipe_.acceptMetadata( new EmptyStarTable() );
            }
        }

        /**
         * Copies the input table cells from this worker's result sequence 
         * which will be required for output to a given object array.
         *
         * @param  row   destination array, must have at least
         *               iCopyCols_.length elements
         */
        private void copyInCells( Object[] row )
                throws IOException {
            int ncIn = iCopyCols_.length;
            for ( int ic = 0; ic < ncIn; ic++ ) {
                row[ ic ] = resultSeq_.getCell( iCopyCols_[ ic ] );
            }
        }

        /**
         * Constructs a metadata table which will describe (but not contain)
         * the rows that this object is going to write to the output pipe.
         * The columns are those copied from the input table followed by 
         * all the columns from each cone search.  The assumption is made
         * that every successful cone search to the same service will return
         * tables with the same set of columns.
         *
         * @param   coneResult  result of a non-empty cone search
         * @return   dataless table 
         */
        private StarTable getMetadata( StarTable coneResult ) {
            int ncol = iCopyCols_.length + coneResult.getColumnCount();

            /* Assemble metadata for copied columns. */
            ColumnInfo[] infos = new ColumnInfo[ ncol ];
            for ( int icol = 0; icol < iCopyCols_.length; icol++ ) {
                infos[ icol ] = inTable_.getColumnInfo( iCopyCols_[ icol ] );
            }

            /* Assemble metadata for columns returned from cone search. */
            for ( int icol = 0; icol < coneResult.getColumnCount(); icol++ ) {
                infos[ icol + iCopyCols_.length ] =
                    coneResult.getColumnInfo( icol );
            }

            /* Perform column name deduplication as required. */
            List<String> colNames = new ArrayList<String>();
            for ( int icol = 0; icol < infos.length; icol++ ) {
                colNames.add( infos[ icol ].getName() );
            }
            for ( int icol = 0; icol < infos.length; icol++ ) {
                JoinFixAction fixAct;
                if ( icol < iCopyCols_.length ) {
                    fixAct = inFixAct_;
                }
                else if ( icol < infos.length - extraCols_ ) {
                    fixAct = coneFixAct_;
                }
                else {
                    fixAct = extrasFixAct_;
                }
                String name = infos[ icol ].getName();
                assert name.equals( colNames.get( icol ) );
                colNames.set( icol, null );
                String fixName = fixAct.getFixedName( name, colNames );
                colNames.set( icol, name );
                if ( ! fixName.equals( name ) ) {
                    ColumnInfo info = new ColumnInfo( infos[ icol ] );
                    info.setName( fixName );
                    infos[ icol ] = info;
                }
            }

            /* Return the metadata table. */
            return new RowListStarTable( infos ) {
                public long getRowCount() {
                    return -1L;
                }
                public boolean isRandom() {
                    return false;
                }
            };
        }
    }

    /**
     * ColumnSupplement implementation that has a constant value for each row.
     */
    private static class ConstantColumnSupplement implements ColumnSupplement {
        private final ColumnInfo[] colInfos_;
        private final Object[] row_;

        /**
         * Constructor.
         *
         * @param   colInfos  column metadata array
         * @param   row   column data array, same for each row
         */
        ConstantColumnSupplement( ColumnInfo[] colInfos, Object[] row ) {
            colInfos_ = colInfos;
            row_ = row;
        }

        public int getColumnCount() {
            return colInfos_.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public Object getCell( long irow, int icol ) {
            return row_[ icol ];
        }

        public Object[] getRow( long irow ) {
            return row_.clone();
        }

        public SupplementData createSupplementData( RowData rdata ) {
            return new SupplementData() {
                public Object getCell( long irow, int icol ) {
                    return row_[ icol ];
                }
                public Object[] getRow( long irow ) {
                    return row_.clone();
                }
            };
        }
    }
}
