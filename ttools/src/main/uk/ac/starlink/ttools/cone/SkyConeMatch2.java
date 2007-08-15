package uk.ac.starlink.ttools.cone;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowPipe;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.SelectorStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.ColumnIdentifier;
import uk.ac.starlink.ttools.JELUtils;
import uk.ac.starlink.ttools.SequentialJELRowReader;
import uk.ac.starlink.ttools.func.Coords;
import uk.ac.starlink.ttools.task.ChoiceMode;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.ttools.task.SingleMapperTask;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * Crossmatcher which works by performing one cone-search type query 
 * for each row of an input table on an external service of some kind.
 * This is not <i>prima facie</i> a very efficient way of doing a
 * cross match, but if the external service represents a table which 
 * is too large or otherwise unfeasible to access as one term in a
 * normal cone search it's about the only way to do it.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2007
 */
public abstract class SkyConeMatch2 extends SingleMapperTask {

    private final Coner coner_;
    private final Parameter raParam_;
    private final Parameter decParam_;
    private final Parameter srParam_;
    private final Parameter copycolsParam_;
    private final ChoiceParameter modeParam_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param  purpose  one-line description of the purpose of the task
     * @param  coner   object which provides the sky cone search service
     */
    public SkyConeMatch2( String purpose, Coner coner ) {
        super( purpose, new ChoiceMode(), true, true );
        coner_ = coner;
        List paramList = new ArrayList();
        String system = coner.getSkySystem();
        String sysParen;
        String inSys;
        if ( system == null || system.length() == 0 ) {
            sysParen = "";
            inSys = "";
        }
        else {
            sysParen = " (" + system + ")";
            inSys = " in the " + system + " coordinate system";
        }
    
        raParam_ = new Parameter( "ra" );
        raParam_.setUsage( "<expr>" );
        raParam_.setPrompt( "Right Ascension expression in degrees"
                          + sysParen );
        raParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the right ascension in degrees"
            + inSys,
            "for the request at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one.",
            "</p>",
        } );
        paramList.add( raParam_ );

        decParam_ = new Parameter( "dec" );
        decParam_.setUsage( "<expr>" );
        decParam_.setPrompt( "Declination expression in degrees"
                           + sysParen );
        decParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the declination in degrees"
            + inSys,
            "for the request at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one.",
            "</p>",
        } );
        paramList.add( decParam_ );

        srParam_ = new Parameter( "sr" );
        srParam_.setUsage( "<expr>" );
        srParam_.setPrompt( "Search radius in degrees" );
        srParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the search radius in degrees",
            "for the request at each row of the input table.",
            "This will often be a constant numerical value, but may be",
            "the name or ID of a column in the input table,",
            "or a function involving one.",
            "</p>",
        } );
        paramList.add( srParam_ );

        copycolsParam_ = new Parameter( "copycols" );
        copycolsParam_.setUsage( "<colid-list>" );
        copycolsParam_.setNullPermitted( true );
        copycolsParam_.setPrompt( "Columns to be copied from input table" );
        copycolsParam_.setDescription( new String[] {
            "<p>List of columns from the input table which are to be copied",
            "to the output table.",
            "Each column identified here will be prepended to the",
            "columns of the combined output table,",
            "and its value for each row taken from the input table row",
            "which provided the parameters of the query which produced it.",
            "See <ref id='colid-list'/> for list syntax.",
            "</p>",
        } );
        paramList.add( copycolsParam_ );

        modeParam_ = new ChoiceParameter( "find", new String[] {
            "best", "all",
        } );
        modeParam_.setDefault( "all" );
        modeParam_.setPrompt( "Type of match to perform" );
        modeParam_.setDescription( new String[] {
            "<p>Determines which matches are retained.",
            "If <code>best</code> is selected, then only the query table row",
            "which best matches the row from the input table will be output.",
            "If <code>all</code> is selected, then any rows in the query table",
            "which match the input table are output.",
            "</p>",
        } );
        paramList.add( modeParam_ );

        getParameterList().addAll( paramList );
        getParameterList().addAll( Arrays.asList( coner.getParameters() ) );
    }

    protected TableProducer createProducer( Environment env )
            throws TaskException {

        /* Interrogate environment for parameter values. */
        final String copyColIdList = copycolsParam_.stringValue( env );
        final String raString = raParam_.stringValue( env );
        final String decString = decParam_.stringValue( env );
        final String srString = srParam_.stringValue( env );
        final boolean bestOnly;
        String mode = modeParam_.stringValue( env );
        if ( mode.toLowerCase().equals( "best" ) ) {
            bestOnly = true;
        }
        else if ( mode.toLowerCase().equals( "all" ) ) {
            bestOnly = false;
        }
        else {
            throw new UsageException( "Unknown value of " +
                                      modeParam_.getName() + "??" );
        }
        final TableProducer inProd = createInputProducer( env );
        final ConeSearcher coneSearcher =
            coner_.createSearcher( env, bestOnly );

        /* Return a table producer using these values. */
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                StarTable inTable = inProd.getTable();
                SequentialJELRowReader jelReader =
                    new SequentialJELRowReader( inTable );
                Library lib = JELUtils.getLibrary( jelReader );
                CompiledExpression raExpr = compileDouble( raString, lib );
                CompiledExpression decExpr = compileDouble( decString, lib );
                CompiledExpression srExpr = compileDouble( srString, lib );
                int[] iCopyCols = ( copyColIdList == null ||
                                    copyColIdList.trim().length() == 0 )
                                ? new int[ 0 ]
                                : new ColumnIdentifier( inTable )
                                 .getColumnIndices( copyColIdList );
                RowPipe rowPipe = new OnceRowPipe();
                Thread coneWorker =
                    new ConeWorker( rowPipe, inTable, coneSearcher, jelReader,
                                    raExpr, decExpr, srExpr, iCopyCols,
                                    bestOnly );
                coneWorker.setDaemon( true );
                coneWorker.start();
                return rowPipe.waitForStarTable();
            }
        };
    }

    /**
     * Thread which performs the individual cone search and writes the 
     * results down a pipe from which it will be read for output.
     */
    private static class ConeWorker extends Thread {
        private final RowPipe rowPipe_;
        private final StarTable inTable_;
        private final ConeSearcher coneSearcher_;
        private final SequentialJELRowReader jelReader_;
        private final CompiledExpression raExpr_;
        private final CompiledExpression decExpr_;
        private final CompiledExpression srExpr_;
        private final int[] iCopyCols_;
        private final boolean bestOnly_;

        /**
         * Constructor.
         *
         * @param   rowPipe  row data pipe
         * @param   inTable  input table
         * @param   coneSearcher   cone search implementation object
         * @param   jelReader   JEL row sequence taken from the input table,
         *                      positioned at the start of the data
         * @param   raExpr   expression representing FK5 RA in degrees 
         *                   in input table
         * @param   decExpr  expression representing FK5 Dec in degrees
         *                   in input table
         * @param   srExpr   expression representing search radius in degrees
         *                   in input table
         * @param   iCopyCols  indices of columns from the input table to
         *                     be copied to the output table
         * @param   bestOnly  true if only the best one row is to be returned
         */
        ConeWorker( RowPipe rowPipe, StarTable inTable,
                    ConeSearcher coneSearcher, SequentialJELRowReader jelReader,
                    CompiledExpression raExpr, CompiledExpression decExpr,
                    CompiledExpression srExpr, int[] iCopyCols,
                    boolean bestOnly ) {
            super( "Cone searcher" );
            rowPipe_ = rowPipe;
            inTable_ = inTable;
            coneSearcher_ = coneSearcher;
            jelReader_ = jelReader;
            raExpr_ = raExpr;
            decExpr_ = decExpr;
            srExpr_ = srExpr;
            iCopyCols_ = iCopyCols;
            bestOnly_ = bestOnly;
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
                    jelReader_.close();
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
            boolean started = false;

            /* Loop over rows of the input table. */
            int irow = 0;
            while ( jelReader_.next() ) {

                /* Perform the cone search for this row. */
                StarTable result = getConeResult();

                /* Append one row to the output for each row in the
                 * cone search result.  Each row consists of the copied
                 * columns from the input table followed by all the 
                 * columns from the cone search result. */
                RowSequence coneSeq = result.getRowSequence();
                int nr = 0;
                try {
                    while ( coneSeq.next() ) {
                        nr++;

                        /* If this is the first non-blank entry we've got, 
                         * acquire the metadata (most importantly column 
                         * descriptions) from it and use that to initialise the
                         * output row pipe. */
                        if ( ! started ) {
                            rowPipe_.acceptMetadata( getMetadata( result ) );
                            started = true;
                        }

                        /* Append the actual rows. */
                        int ncIn = iCopyCols_.length;
                        int ncCone = result.getColumnCount();
                        Object[] row = new Object[ ncIn + ncCone ];
                        for ( int ic = 0; ic < ncIn; ic++ ) {
                            row[ ic ] = jelReader_.getCell( iCopyCols_[ ic ] );
                        }
                        for ( int ic = 0; ic < ncCone; ic++ ) {
                            row[ ncIn + ic ] = coneSeq.getCell( ic );
                        }
                        rowPipe_.acceptRow( row );
                    }
                }
                finally {
                    coneSeq.close();
                }

                /* Log number of rows successfully appended. */
                logger_.info( "Row " + irow++ + ": got " + nr
                            + ( ( nr == 1 ) ? " row" : " rows" ) );
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
            ColumnInfo[] infos = new ColumnInfo[ ncol ];
            for ( int icol = 0; icol < iCopyCols_.length; icol++ ) {
                infos[ icol ] = inTable_.getColumnInfo( iCopyCols_[ icol ] );
            }
            for ( int icol = 0; icol < coneResult.getColumnCount(); icol++ ) {
                infos[ icol + iCopyCols_.length ] =
                    coneResult.getColumnInfo( icol );
            }
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
         * @return  table containing cone search result; if it has no rows then
         *          the column metadata may be wrong and should not be used
         */
        private StarTable getConeResult() throws IOException {
            Object raObj;
            Object decObj;
            Object srObj;
            try {
                raObj = jelReader_.evaluate( raExpr_ );
                decObj = jelReader_.evaluate( decExpr_ );
                srObj = jelReader_.evaluate( srExpr_ );
            }
            catch ( IOException e ) { 
                throw e;
            }
            catch ( Throwable e ) {
                logger_.warning( "Data evaluation error: " + e.getMessage() );
                return new EmptyStarTable();
            }
            double ra = raObj instanceof Number
                      ? ((Number) raObj).doubleValue()
                      : Double.NaN;
            double dec = decObj instanceof Number
                       ? ((Number) decObj).doubleValue()
                       : Double.NaN;
            double sr = srObj instanceof Number
                      ? ((Number) srObj).doubleValue()
                      : Double.NaN;
            if ( ! Double.isNaN( ra ) && ! Double.isNaN( dec ) &&
                 ! Double.isNaN( sr ) ) {
                try {
                    StarTable coneResult = getConeResult( ra, dec, sr );
                    return coneResult;
                }
                catch ( IOException e ) {
                    logger_.warning( "Cone search error: " + e.getMessage() );
                    return new EmptyStarTable();
                }
            }
            else {
                logger_.warning( "Invalid search parameters" );
                return new EmptyStarTable();
            }
        }

        /**
         * Performs a cone search and returns the resulting table with
         * appropriate filtering operations applied.
         * The resulting table will fall strictly within the specified 
         * search region and will contain a restricted set of rows if
         * that has been requested.
         *
         * @param   ra0   right ascension in degrees of region centre
         * @param   dec0  declination in degrees of region centre
         * @param   sr    search radius in degrees
         * @return   filtered result table
         */
        private StarTable getConeResult( final double ra0, final double dec0,
                                         final double sr )
                throws IOException {

            /* Perform the cone search itself. */
            logger_.info( "Cone: ra=" + ra0 + "; dec=" + dec0 + "; sr=" + sr );
            StarTable result = coneSearcher_.performSearch( ra0, dec0, sr );

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

    /**
     * Compiles a JEL expression.
     * An informative UsageException is thrown if it won't compile.
     *
     * @param   lib   JEL library
     * @param   sexpr   string expression
     * @return  compiled expression
     */
    private static CompiledExpression compileDouble( String sexpr, Library lib )
            throws UsageException {
        try {
            return Evaluator.compile( sexpr, lib, double.class );
        }
        catch ( CompilationException e ) {
            throw new UsageException( "Bad numeric expression \"" + sexpr + "\""
                                    + " - " + e.getMessage() );
        }
    }
}
