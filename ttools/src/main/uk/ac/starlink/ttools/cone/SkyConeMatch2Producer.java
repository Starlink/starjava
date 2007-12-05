package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
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
import uk.ac.starlink.ttools.ColumnIdentifier;
import uk.ac.starlink.ttools.func.Coords;
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
    private final boolean bestOnly_;
    private final String copyColIdList_;
    private final JoinFixAction inFixAct_;
    private final JoinFixAction coneFixAct_;
    private boolean streamOutput_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param   coneSearcher   cone search implementation
     * @param   inProd   source of input table (containing each crossmatch
     *                   specification)
     * @param   qsFact    object which can produce a ConeQueryRowSequence
     * @param   bestOnly  true iff only the best match for each input table
     *                    row is required, false for all matches within radius
     * @param   copyColIdList  space-separated list of column identifiers for
     *                         columns to be copied to the output table,
     *                         "*" for all columns
     * @param   inFixAct   column name deduplication action for input table
     * @param   coneFixAct column name deduplication action for result
     *                     of cone searches
     */
    public SkyConeMatch2Producer( ConeSearcher coneSearcher,
                                  TableProducer inProd,
                                  QuerySequenceFactory qsFact,
                                  boolean bestOnly,
                                  String copyColIdList,
                                  JoinFixAction inFixAct,
                                  JoinFixAction coneFixAct ) {
        coneSearcher_ = coneSearcher;
        inProd_ = inProd;
        qsFact_ = qsFact;
        bestOnly_ = bestOnly;
        copyColIdList_ = copyColIdList;
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
        int[] iCopyCols = ( copyColIdList_ == null ||
                            copyColIdList_.trim().length() == 0 )
                        ? new int[ 0 ]
                        : new ColumnIdentifier( inTable )
                         .getColumnIndices( copyColIdList_ );
        RowPipe rowPipe = new OnceRowPipe();
        Thread coneWorker =
            new ConeWorker( rowPipe, inTable, coneSearcher_, querySeq,
                            iCopyCols, bestOnly_, inFixAct_, coneFixAct_ );
        coneWorker.setDaemon( true );
        coneWorker.start();
        StarTable streamTable = rowPipe.waitForStarTable();
        return streamOutput_ ? streamTable
                             : Tables.randomTable( streamTable );
    }

    /**
     * Thread which performs the individual cone search and writes the 
     * results down a pipe from which it will be read for output.
     */
    private static class ConeWorker extends Thread {
        private final RowPipe rowPipe_;
        private final StarTable inTable_;
        private final ConeSearcher coneSearcher_;
        private final ConeQueryRowSequence querySeq_;
        private final int[] iCopyCols_;
        private final boolean bestOnly_;
        private final JoinFixAction inFixAct_;
        private final JoinFixAction coneFixAct_;

        /**
         * Constructor.
         *
         * @param   rowPipe  row data pipe
         * @param   inTable  input table
         * @param   coneSearcher   cone search implementation object
         * @param   querySeq  cone search query row sequence, positioned at
         *                    the start of the data
         * @param   iCopyCols  indices of columns from the input table to
         *                     be copied to the output table
         * @param   bestOnly  true if only the best one row is to be returned
         * @param   inFixAct   column name deduplication action for input table
         * @param   coneFixAct column name deduplication action for result
         *                     of cone searches
         */
        ConeWorker( RowPipe rowPipe, StarTable inTable,
                    ConeSearcher coneSearcher, ConeQueryRowSequence querySeq,
                    int[] iCopyCols, boolean bestOnly,
                    JoinFixAction inFixAct, JoinFixAction coneFixAct ) {
            super( "Cone searcher" );
            rowPipe_ = rowPipe;
            inTable_ = inTable;
            coneSearcher_ = coneSearcher;
            querySeq_ = querySeq;
            iCopyCols_ = iCopyCols;
            bestOnly_ = bestOnly;
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
                    querySeq_.close();
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
            for ( int irow = 0; querySeq_.next(); irow++ ) {

                /* Perform the cone search for this row. */
                StarTable result = getConeResult();
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
                    RowSequence resultSeq = result.getRowSequence();
                    int nr = 0;
                    try {
                        while ( resultSeq.next() ) {
                            nr++;

                            /* Append the actual rows. */
                            int ncIn = iCopyCols_.length;
                            int ncCone = result.getColumnCount();
                            Object[] row = new Object[ ncIn + ncCone ];
                            for ( int ic = 0; ic < ncIn; ic++ ) {
                                row[ ic ] =
                                    querySeq_.getCell( iCopyCols_[ ic ] );
                            }
                            for ( int ic = 0; ic < ncCone; ic++ ) {
                                row[ ncIn + ic ] = resultSeq.getCell( ic );
                            }
                            rowPipe_.acceptRow( row );
                        }
                    }
                    finally {
                        resultSeq.close();
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

        /**
         * Queries this object's cone searcher according to the search
         * parameters in the current row of the input table.
         *
         * @return  table containing cone search result, or possibly null
         */
        private StarTable getConeResult() throws IOException {
            double ra = querySeq_.getRa();
            double dec = querySeq_.getDec();
            double sr = querySeq_.getRadius();
            if ( ! Double.isNaN( ra ) && ! Double.isNaN( dec ) &&
                 ! Double.isNaN( sr ) ) {
                return getConeResult( ra, dec, sr );
            }
            else {
                logger_.warning( "Invalid search parameters" );
                return null;
            }
        }

        /**
         * Performs a cone search and returns the resulting table with
         * appropriate filtering operations applied.
         * The resulting table will fall strictly within the specified 
         * search region and will contain a restricted set of rows if
         * that has been requested.
         *
         * <p>If no records in the cone are found, the return value may either
         * be null or (preferably) an empty table with the correct columns.
         *
         * @param   ra0   right ascension in degrees of region centre
         * @param   dec0  declination in degrees of region centre
         * @param   sr    search radius in degrees
         * @return   filtered result table, or null
         */
        private StarTable getConeResult( final double ra0, final double dec0,
                                         final double sr )
                throws IOException {

            /* Perform the cone search itself. */
            logger_.info( "Cone: ra=" + ra0 + "; dec=" + dec0 + "; sr=" + sr );
            StarTable result = coneSearcher_.performSearch( ra0, dec0, sr );
            if ( result == null ) {
                return null;
            }

            /* Work out the columns which represent RA and Dec in the result. */
            final int ira = coneSearcher_.getRaIndex( result );
            final int idec = coneSearcher_.getDecIndex( result );
            if ( ira < 0 || idec < 0 ) {
                logger_.warning( "Can't locate RA/DEC in output table - "
                               + "no post-filtering" );
                return result;
            }

            /* If only a single output row per input row has been requested,
             * identify the best match and return a table containing only 
             * that one. */
            if ( bestOnly_ ) {
                RowSequence rseq = result.getRowSequence();
                double bestDist = Double.NaN;
                Object[] bestRow = null;
                while ( rseq.next() ) {
                    Object[] row = rseq.getRow();
                    double dist = getDistance( row, ira, idec, ra0, dec0 );
                    if ( dist <= sr &&
                         ( dist < bestDist || Double.isNaN( bestDist ) ) ) {
                        bestDist = dist;
                        bestRow = (Object[]) row.clone();
                    }
                }
                RowListStarTable result1 = new RowListStarTable( result );
                if ( ! Double.isNaN( bestDist ) ) {
                    result1.addRow( bestRow );
                }
                return result1;
            }

            /* Otherwise return a table which ensures that all the rows are
             * in the search region.  This filtering is necessary since the
             * ConeSearcher contract allows the return of supersets of the
             * requested region. */
            else {
                return new SelectorStarTable( result ) {
                    public boolean isIncluded( RowSequence rseq )
                            throws IOException {
                        return getDistance( rseq.getRow(),
                                            ira, idec, ra0, dec0 ) <= sr;
                    }
                };
            }
        }

        /**
         * Returns the distance between two points on the sky.
         *
         * @param  row  data row
         * @param  ira  index of element in <code>row</code> containing 
         *              right ascension in degrees of first point
         * @param  idec index of element in <code>row</code> containing
         *              declination in degrees of first point
         * @param  ra0  right ascension in degrees of second point
         * @param  dec0 declination in degrees of second point
         * @return   distance between points in degrees, or NaN if it can't
         *           be determined
         */
        private static double getDistance( Object[] row, int ira, int idec,
                                           double ra0, double dec0 ) {
            Object raObj = row[ ira ];
            Object decObj = row[ idec ];
            double ra1 = raObj instanceof Number
                       ? ((Number) raObj).doubleValue()
                       : Double.NaN;
            double dec1 = decObj instanceof Number
                        ? ((Number) decObj).doubleValue()
                        : Double.NaN;
            return Coords.skyDistanceDegrees( ra0, dec0, ra1, dec1 );
        }
    }
}
