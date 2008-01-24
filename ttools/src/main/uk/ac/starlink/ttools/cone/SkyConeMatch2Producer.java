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
import uk.ac.starlink.ttools.func.Coords;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * TableProducer which does the work for a multiple cone search-type
 * sky crossmatch operation.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2007
 */
public class SkyConeMatch2Producer implements TableProducer {

    private final ConeSearcher coneSearcher_;
    private final TableProducer inProd_;
    private final QuerySequenceFactory qsFact_;
    private final int parallelism_;
    private final boolean bestOnly_;
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
    }

    /**
     * Convenience constructor which selects default values for most options.
     *
     * @param   coneSearcher   cone search implementation
     * @param   inProd   source of input table (containing each crossmatch
     *                   specification)
     * @param   qsFact    object which can produce a ConeQueryRowSequence
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
     */
    public SkyConeMatch2Producer( ConeSearcher coneSearcher,
                                  TableProducer inProd,
                                  QuerySequenceFactory qsFact,
                                  boolean bestOnly ) {
        this( coneSearcher, inProd, qsFact, bestOnly,
              1, "*", DISTANCE_INFO.getName(), JoinFixAction.NO_ACTION,
              JoinFixAction.makeRenameDuplicatesAction( "_1", false, false ) );
    }
    

    /**
     * Full-functioned constructor.
     *
     * @param   coneSearcher   cone search implementation
     * @param   inProd   source of input table (containing each crossmatch
     *                   specification)
     * @param   qsFact    object which can produce a ConeQueryRowSequence
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
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
    public SkyConeMatch2Producer( ConeSearcher coneSearcher,
                                  TableProducer inProd,
                                  QuerySequenceFactory qsFact,
                                  boolean bestOnly,
                                  int parallelism,
                                  String copyColIdList,
                                  String distanceCol,
                                  JoinFixAction inFixAct,
                                  JoinFixAction coneFixAct ) {
        coneSearcher_ = coneSearcher;
        inProd_ = inProd;
        qsFact_ = qsFact;
        bestOnly_ = bestOnly;
        parallelism_ = parallelism;
        copyColIdList_ = copyColIdList;
        distanceCol_ = distanceCol;
        inFixAct_ = inFixAct;
        coneFixAct_ = coneFixAct;
    }

    /**
     * Determines whether this object's {@link #getTable} method will 
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
     * Returns the result, which is a join between the input table and
     * the table on which the cone searches are defined.
     *
     * <p><strong>Note</strong></p>: if the streamOut attribute of this
     * class has been set the result will be a one-read-only table,
     * designed for streaming.
     *
     * @return   joined table
     */
    public StarTable getTable() throws IOException, TaskException {
        StarTable inTable = inProd_.getTable();
        ConeQueryRowSequence querySeq = qsFact_.createQuerySequence( inTable );
        final ConeResultRowSequence resultSeq;
        if ( parallelism_ == 1 ) {
            resultSeq = new SequentialResultRowSequence( querySeq,
                                                         coneSearcher_,
                                                         bestOnly_,
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
                                                       bestOnly_,
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
        RowPipe rowPipe = new OnceRowPipe();
        Thread coneWorker =
            new ConeWorker( rowPipe, inTable, resultSeq, iCopyCols, 
                            inFixAct_, coneFixAct_ );
        coneWorker.setDaemon( true );
        coneWorker.start();
        StarTable streamTable = rowPipe.waitForStarTable();
        return streamOutput_ ? streamTable
                             : Tables.randomTable( streamTable );
    }

    /**
     * Performs a cone search and returns the resulting table with
     * appropriate filtering operations applied.
     * The resulting table will fall strictly within the specified
     * search region and will contain a restricted set of rows if
     * that has been requested.
     *
     * <p>If a non-null <code>distanceCol</code> parameter is supplied,
     * the final column in the table will contain the angle in degrees
     * between the region centre and the position described in the row.
     *
     * <p>If no records in the cone are found, the return value may either
     * be null or (preferably) an empty table with the correct columns.
     *
     * @param   coneSearcher   cone search implementation
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
     * @param   distanceCol  name of column to hold distance information
     *                       int output table, or null
     * @param   ra0   right ascension in degrees of region centre
     * @param   dec0  declination in degrees of region centre
     * @param   sr    search radius in degrees
     * @return   filtered result table, or null
     */
    public static StarTable getConeResult( ConeSearcher coneSearcher,
                                           boolean bestOnly,
                                           String distanceCol,
                                           final double ra0, final double dec0,
                                           final double sr )
            throws IOException {

        /* Validate parameters. */
        if ( Double.isNaN( ra0 ) || Double.isNaN( dec0 ) ||
             Double.isNaN( sr ) ) {
            logger_.warning( "Invalid search parameters" );
            return null;
        }

        /* Perform the cone search itself. */
        logger_.info( "Cone: ra=" + ra0 + "; dec=" + dec0 + "; sr=" + sr );
        StarTable result = coneSearcher.performSearch( ra0, dec0, sr );
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
                    if ( dist <= sr &&
                         ( dist < bestDist || Double.isNaN( bestDist ) ) ) {
                        bestDist = dist;
                        bestRow = (Object[]) rseq.getRow().clone();
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
        else {
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
     * @param  ira  index of column in inTable giving right ascension in degrees
     *              - may be -1 if unknown
     * @param  idec index of column in inTable giving declination in degrees
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
        ColumnInfo[] addCols = new ColumnInfo[] { distInfo };
        if ( ira < 0 || idec < 0 ) {
            return new AddColumnsTable( inTable, new int[ 0 ],
                                        addCols, ncolIn ) {
                protected Object[] calculateValues( Object[] inValues ) {
                    return new Object[] { new Double( Double.NaN ) };
                }
            };
        }
        else {
            return new AddColumnsTable( inTable, new int[] { ira, idec },
                                        addCols, ncolIn ) {
                protected Object[] calculateValues( Object[] inValues ) {
                    double ra1 = inValues[ 0 ] instanceof Number
                               ? ((Number) inValues[ 0 ]).doubleValue()
                               : Double.NaN;
                    double dec1 = inValues[ 1 ] instanceof Number
                                ? ((Number) inValues[ 1 ]).doubleValue()
                                : Double.NaN;
                    double dist =
                        Coords.skyDistanceDegrees( ra0, dec0, ra1, dec1 );
                    return new Object[] { new Double( dist ) };
                }
            };
        }
    }

    /**
     * Thread which performs the individual cone search and writes the 
     * results down a pipe from which it will be read for output.
     */
    private static class ConeWorker extends Thread {
        private final RowPipe rowPipe_;
        private final StarTable inTable_;
        private final ConeResultRowSequence resultSeq_;
        private final int[] iCopyCols_;
        private final JoinFixAction inFixAct_;
        private final JoinFixAction coneFixAct_;

        /**
         * Constructor.
         *
         * @param   rowPipe  row data pipe
         * @param   inTable  input table
         * @param   resultSeq  cone search result row sequence, positioned at
         *                     the start of the data
         * @param   iCopyCols  indices of columns from the input table to
         *                     be copied to the output table
         * @param   inFixAct   column name deduplication action for input table
         * @param   coneFixAct column name deduplication action for result
         *                     of cone searches
         */
        ConeWorker( RowPipe rowPipe, StarTable inTable,
                    ConeResultRowSequence resultSeq, int[] iCopyCols,
                    JoinFixAction inFixAct, JoinFixAction coneFixAct ) {
            super( "Cone searcher" );
            rowPipe_ = rowPipe;
            inTable_ = inTable;
            resultSeq_ = resultSeq;
            iCopyCols_ = iCopyCols;
            inFixAct_ = inFixAct;
            coneFixAct_ = coneFixAct;
        }

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
            int ncol = -1;

            /* Loop over rows of the input table. */
            for ( int irow = 0; resultSeq_.next(); irow++ ) {

                /* Perform the cone search for this row. */
                StarTable result = resultSeq_.getConeResult();
                if ( result != null ) {

                    /* If this is the first entry we've got, acquire the 
                     * metadata (most importantly column descriptions) from it 
                     * and use that to initialise the output row pipe. */
                    int nc = result.getColumnCount();
                    if ( ncol < 0 ) {
                        ncol = nc;
                        rowPipe_.acceptMetadata( getMetadata( result ) );
                    }
                    else if ( nc != ncol ) {
                        String msg = "Inconsistent column counts "
                                   + "from different cone search invocations"
                                   + " (" + nc + " != " + ncol + ")";
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
                            int ncIn = iCopyCols_.length;
                            int ncCone = result.getColumnCount();
                            Object[] row = new Object[ ncIn + ncCone ];
                            for ( int ic = 0; ic < ncIn; ic++ ) {
                                row[ ic ] =
                                    resultSeq_.getCell( iCopyCols_[ ic ] );
                            }
                            for ( int ic = 0; ic < ncCone; ic++ ) {
                                row[ ncIn + ic ] = rSeq.getCell( ic );
                            }
                            rowPipe_.acceptRow( row );
                        }
                    }
                    finally {
                        rSeq.close();
                    }

                    /* Log number of rows successfully appended. */
                    logger_.info( "Row " + irow + ": got " + nr
                                + ( ( nr == 1 ) ? " match" : " matches" ) );
                }
                else {
                    logger_.info( "Row " + irow + ": got no matches" );
                }
            }

            /* If the output table was never initialised, do it here with a
             * dummy table.  Ideally this will not happen, but it might do
             * in the case that no matches were found AND the ConeSearcher
             * implementation returns nulls instead of empty tables for
             * empty searches. */
            if ( ncol < 0 ) {
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
            List colNames = new ArrayList();
            for ( int icol = 0; icol < infos.length; icol++ ) {
                colNames.add( infos[ icol ].getName() );
            }
            for ( int icol = 0; icol < infos.length; icol++ ) {
                JoinFixAction fixAct = icol < iCopyCols_.length ? inFixAct_
                                                                : coneFixAct_;
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
}
