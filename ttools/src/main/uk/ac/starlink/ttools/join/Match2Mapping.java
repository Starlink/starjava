package uk.ac.starlink.ttools.join;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RowLink;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELTable;
import uk.ac.starlink.ttools.task.InputTableSpec;
import uk.ac.starlink.ttools.task.TableMapping;

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
    final PairMode pairMode_;
    final JoinType join_;
    final ValueInfo scoreInfo_;
    final ProgressIndicator progger_;
    final RowRunner runner_;

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
     * @param   pairMode   pair matching mode
     * @param   fixact1    deduplication fix action for first input table
     * @param   fixact2    deduplication fix action for second input table
     * @param   scoreInfo  column description for inter-table match score
     *                     values, or null for no score column
     * @param   progger    progress indicator for matching
     * @param   runner    controls parallel implementation,
     *                    or null for sequential
     */
    Match2Mapping( MatchEngine matchEngine, String[] exprTuple1,
                   String[] exprTuple2, JoinType join, PairMode pairMode,
                   JoinFixAction fixact1, JoinFixAction fixact2,
                   ValueInfo scoreInfo, ProgressIndicator progger,
                   RowRunner runner ) {
        matchEngine_ = matchEngine;
        exprTuple1_ = exprTuple1;
        exprTuple2_ = exprTuple2;
        join_ = join;
        pairMode_ = pairMode;
        fixacts_ = new JoinFixAction[] { fixact1, fixact2, };
        scoreInfo_ = join.getUsedMatchFlag() ? scoreInfo : null;
        progger_ = progger;
        runner_ = runner;
    }

    public StarTable mapTables( InputTableSpec[] inSpecs )
            throws IOException, TaskException {
        try {
            return attemptMapTables( inSpecs );
        }
        catch ( InterruptedException e ) {
            throw new ExecutionException( e.getMessage(), e );  
        }
    }

    /**
     * Does the work for the mapTables method, but may throw an
     * InterruptedException if the operations were forcibly terminated.
     *
     * @param  inSpecs  input table specifications
     * @return  joined table
     */
    private StarTable attemptMapTables( InputTableSpec[] inSpecs )
            throws IOException, TaskException, InterruptedException {
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
            RowMatcher
           .createMatcher( matchEngine_,
                           new StarTable[] { subTable1, subTable2 }, runner_ );
        matcher.setIndicator( progger_ );
        LinkSet matches = matcher.findPairMatches( pairMode_ );
        boolean addGroups = pairMode_.mayProduceGroups();

        /* Process the row link lists according to the chosen join type.
         * This will give a set of rows to be retained in
         * the output table based in some way on the actual pair matches
         * which were found. */
        int nrows1 = Tables.checkedLongToInt( inTable1.getRowCount() );
        int nrows2 = Tables.checkedLongToInt( inTable2.getRowCount() );
        matches = join_.processLinks( matches, new int[] { nrows1, nrows2 } );

        /* Work out which of the input tables will actually make an
         * appearance in the output table (i.e. which of their columns
         * will be required). */
        boolean[] useFlags = join_.getUsedTableFlags();
        StarTable[] tables = new StarTable[] {
            useFlags[ 0 ] ? inTable1 : null,
            useFlags[ 1 ] ? inTable2 : null,
        };

        /* Create a new table from the result and return. */
        Collection<RowLink> links = MatchStarTables.orderLinks( matches );
        return MatchStarTables.createInstance( progger_, runner_ )
              .makeJoinTable( tables, links, addGroups, fixacts_, scoreInfo_ );
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
        return JELTable.createJELTable( inTable, matchEngine_.getTupleInfos(),
                                        exprTuple );
    }
}
