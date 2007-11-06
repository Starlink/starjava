package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.JELTable;

/**
 * TableMapping implementation which does the work of matching two tables.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2007
 */
public class Match2Mapping implements TableMapping {

    final String[] exprTuple1_;
    final String[] exprTuple2_;
    final JoinFixAction[] fixacts_;
    final MatchEngine matchEngine_;
    final boolean bestOnly_;
    final JoinType join_;
    final PrintStream logStrm_;

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param   matchEngine  engine defining the match characteristics
     * @param   exprTuple1  array of strings defining the values from
     *          the first input table which are used for input into the
     *          match; each element is a JEL expression evaluated in
     *          the context of the first table
     * @param   exprTuple2  array of strings defining the values from
     *          the second input table which are used for input into the
     *          match; each element is a JEL expression evaluated in
     *          the context of the second table
     * @param   join  output row selection type
     * @param   bestOnly   whether only the best match is to be retained
     * @param   fixact1    deduplication fix action for first input table
     * @param   fixact2    deduplication fix action for second input table
     * @param   logStrm    print stream for logging output
     */
    Match2Mapping( MatchEngine matchEngine, String[] exprTuple1,
                   String[] exprTuple2, JoinType join,
                   boolean bestOnly, JoinFixAction fixact1, 
                   JoinFixAction fixact2, PrintStream logStrm ) {
        matchEngine_ = matchEngine;
        exprTuple1_ = exprTuple1;
        exprTuple2_ = exprTuple2;
        join_ = join;
        bestOnly_ = bestOnly;
        fixacts_ = new JoinFixAction[] { fixact1, fixact2, };
        logStrm_ = logStrm;
    }

    public StarTable mapTables( InputTableSpec[] inSpecs )
            throws IOException, TaskException {
        StarTable inTable1 = inSpecs[ 0 ].getWrappedTable();
        StarTable inTable2 = inSpecs[ 1 ].getWrappedTable();

        /* Attempt to create the tables containing the tuples with which
         * the match will be done.  This is a dry run, intended to
         * catch any exceptions before the possibly expensive work
         * of randomising the input tables is performed. */
        makeSubTable( inTable1, exprTuple1_ );
        makeSubTable( inTable2, exprTuple2_ );

        /* Now randomise the tables (currently required for the rest
         * of the matching) and create the subtables for real. */
        inTable1 = Tables.randomTable( inTable1 );
        inTable2 = Tables.randomTable( inTable2 );
        StarTable subTable1 = makeSubTable( inTable1, exprTuple1_ );
        StarTable subTable2 = makeSubTable( inTable2, exprTuple2_ );

        /* Do the match. */
        RowMatcher matcher =
            new RowMatcher( matchEngine_,
                            new StarTable[] { subTable1, subTable2 } );
        matcher.setIndicator( new TextProgressIndicator( logStrm_ ) );
        LinkSet matches;
        try {
            matches = matcher.findPairMatches( bestOnly_ );
            if ( ! matches.sort() ) {
                logger.warning( "Implementation can't sort rows - "
                              + "matched table rows may not be ordered" );
            }
        }
        catch ( InterruptedException e ) {
            throw new ExecutionException( e.getMessage(), e );  
        }

         /* Create a new table from the result and return. */
        ValueInfo scoreInfo = matchEngine_.getMatchScoreInfo();
        return MatchStarTables.makeJoinTable( inTable1, inTable2, matches,
                                              join_, ! bestOnly_, fixacts_,
                                              scoreInfo );
    }

    /**
     * Creates a table containing the values which are required by the
     * matcher.  This typically consists of a few of the columns from
     * the input table, but in general may come from any JEL 
     * expression based on them.  Because JEL compilation is performed here,
     * an exception (rethrown as an ExecutionException) may occur.
     *
     * @param  inTable  input table
     * @param  exprTuple  array of JEL expressions giving the values of 
     *           the tuple elements required for the matcher
     * @return  table containing only a column for each tuple element 
     *          required for the matcher
     * @throws  ExecutionException  if a compilation error occurs
     */
    protected StarTable makeSubTable( StarTable inTable, String[] exprTuple )
            throws ExecutionException {

        /* Work out the type of columns we will require from each table. */
        ValueInfo[] tupleInfos = matchEngine_.getTupleInfos();
        int nCoord = tupleInfos.length;
        ColumnInfo[] colInfos = new ColumnInfo[ nCoord ];
        for ( int i = 0; i < nCoord; i++ ) {
            colInfos[ i ] = new ColumnInfo( tupleInfos[ i ] );
        }

        /* Create and return a new table containing only columns defined
         * by the supplied expressions. */
        try {
            return new JELTable( inTable, colInfos, exprTuple );
        }
        catch ( CompilationException e ) {
            throw new ExecutionException( e.getMessage(), e );
        }
    }
}
