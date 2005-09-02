package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.JELTable;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * TableMapper which does the work for pair matching (tmatch2).
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class Match2Mapper implements TableMapper {

    private final MatchEngineParameter matcherParam_;
    private final WordsParameter[] tupleParams_;
    private final ChoiceParameter selectParam_;
    private final ChoiceParameter modeParam_;

    public Match2Mapper() {

        matcherParam_ = new MatchEngineParameter( "matcher" );

        tupleParams_ = new WordsParameter[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            tupleParams_[ i ] = new WordsParameter( "values" + i1 );
            tupleParams_[ i ].setUsage( "<expr-list>" );
            tupleParams_[ i ].setPrompt( "Expressions for match values " +
                                         "from table " + i1 );
            tupleParams_[ i ].setDescription( new String[] {
                "Defines the values from table " + i1,
                "which are used to determine whether a match has occurred.",
                "These will typically be coordinate values such as RA and Dec",
                "perhaps some per-row error values as well, though exactly",
                "what values are required is determined by the kind of match",
                "as determined by",
                "<code>" + matcherParam_.getName() + "</code>.",
                "Depending on the kind of match, the number and type of",
                "the values required will be different.",
                "Multiple values should be separated by whitespace;",
                "if whitespace occurs within a single value it must be",
                "'quoted' or \"quoted\".",
                "Elements of the expression list are commonly just column",
                "names, but may be algebraic expressions calculated from",
                "one or more columns as explained in <ref id='jel'/>.",
            } );
        }

        modeParam_ = new ChoiceParameter( "mode", new String[] {
            "best", "all",
        } );
        modeParam_.setDefault( "best" );
        modeParam_.setPrompt( "Type of match to perform" );
        modeParam_.setDescription( new String[] {
            "Determines which matches are retained.",
            "If <code>best</code> is selected, then only the best match",
            "between the two tables will be retained; in this case",
            "the data from a row of either input table will appear in",
            "at most one row of the output table.",
            "If <code>all</code> is selected, then all pairs of rows",
            "from the two input tables which match the input criteria",
            "will be represented in the output table.",
        } );

        selectParam_ = new ChoiceParameter( "outrows", new String[] {
            "all1", "all2", "1or2", "1and2", "1not2", "2not1", "1xor2",
        } );
        selectParam_.setDefault( "1or2" );
        selectParam_.setPrompt( "Selection criteria for output rows" );
        selectParam_.setDescription( new String[] {
            "Determines which rows are included in the output table.",
            "The matching algorithm determines which of the rows from",
            "the first table correspond to which rows from the second.",
            "This parameter determines what to do with that information.",
            "Perhaps the most obvious thing is to write out a table",
            "containing only rows which correspond to a row in both of",
            "the two input tables.  However, you may also want to see",
            "the unmatched rows from one or both input tables,",
            "or rows present in one table but unmatched in the other,",
            "or other possibilities.",
            "The options are:",
            "<ul>",
            "<li><code>1and2</code>:",
                 "An output row for each row represented in",
                 "both input tables</li>",
            "<li><code>1or2</code>:",
                 "Only rows represented in both input tables</li>",
            "<li><code>1or2</code>:",
                 "An output row for each row represented in",
                 "either or both of the input tables</li>",
            "<li>.. and so on</li>",
            "</ul>",
        } );
    }

    public int getInCount() {
        return 2;
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            matcherParam_,
            tupleParams_[ 0 ],
            tupleParams_[ 1 ],
            matcherParam_.getMatchParametersParameter(),
            selectParam_,
            modeParam_,
        };
    }

    public TableMapping createMapping( Environment env ) throws TaskException {

        /* Get the matcher. */
        MatchEngine matcher = matcherParam_.matchEngineValue( env );

        /* Find the number and characteristics of the columns required
         * for this matcher. */
        ValueInfo[] tinfos = matcher.getTupleInfos();
        int ncol = tinfos.length;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < ncol; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( tinfos[ i ].getName() );
        }
        String colNames = sbuf.toString();

        /* Assemble the arrays of supplied expressions which will supply
         * the values to the matcher for each table. */
        String[][] tupleExprs = new String[ 2 ][];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            tupleParams_[ i ].setRequiredWordCount( ncol );
            tupleParams_[ i ].setPrompt( "Table " + i1 + " match columns ("
                                       + colNames + ")" );
            tupleExprs[ i ] = tupleParams_[ i ].wordsValue( env );
        }

        /* Get other parameter values. */
        String select = selectParam_.stringValue( env );
        String mode = modeParam_.stringValue( env );
        PrintStream out = env.getPrintStream();

        /* Construct and return a mapping based on this lot. */
        return new Match2Mapping( matcher, tupleExprs[ 0 ], tupleExprs[ 1 ],
                                  select, mode, out );
    }

    /**
     * TableMapping implementation which does the work of matching two tables.
     */
    private static class Match2Mapping implements TableMapping {

        final String[][] exprTuples_;
        final MatchEngine matchEngine_;
        final PrintStream out_;

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
         * @param   select  output row selection type
         * @param   mode   match mode
         * @param   out   output print stream
         */
        Match2Mapping( MatchEngine matchEngine, String[] exprTuple1,
                       String[] exprTuple2, String select, String mode,
                       PrintStream out ) {
            matchEngine_ = matchEngine;
            exprTuples_ = new String[][] { exprTuple1, exprTuple2 };
            out_ = out;
        }

        public void mapTables( StarTable[] inTables, TableConsumer[] consumers )
                throws IOException, TaskException {

            /* Attempt to create the tables containing the tuples with which
             * the match will be done.  This is a dry run, intended to
             * catch any exceptions before the possibly expensive work
             * of randomising the input tables is performed. */
            makeSubTables( inTables );

            /* Now randomise the tables (currently required for the rest
             * of the matching) and create the subtables for real. */
            for ( int i = 0; i < 2; i++ ) {
                inTables[ i ] = Tables.randomTable( inTables[ i ] );
            }
            StarTable[] subTables = makeSubTables( inTables );

            /* Do the match. */
            RowMatcher matcher = new RowMatcher( matchEngine_, subTables );
            matcher.setIndicator( new TextProgressIndicator( out_ ) );
            Map matchScores;
            try {
                matchScores = matcher.findPairMatches( true, true );
                matchScores = matcher.sortMap( matchScores,
                                               "Sorting output rows" );
            }
            catch ( InterruptedException e ) {
                throw new ExecutionException( e.getMessage(), e );  
            }

             /* Create a new table from the result. */
            JoinStarTable.FixAction[] fixActs = new JoinStarTable.FixAction[] {
                JoinStarTable.FixAction.makeRenameDuplicatesAction( "_1" ),
                JoinStarTable.FixAction.makeRenameDuplicatesAction( "_2" ),
            };
            ValueInfo scoreInfo = matchEngine_.getMatchScoreInfo();
            StarTable out = MatchStarTables
                           .makeJoinTable( inTables, matchScores.keySet(),
                                           fixActs, matchScores, scoreInfo );

            /* Dispose of the resulting matched table. */
            consumers[ 0 ].consume( out );
        }

        /**
         * Creates the tables containing the values which are required by the
         * matcher.  These typically consist of a few of the columns from
         * the input tables, but in general may come from any JEL expression
         * based on them.  Because JEL compilation is performed here, an
         * exception (rethrown as an ExecutionException) may occur.
         *
         * @param  inTables  input tables
         * @return   match value tables based on <code>inTables</code>
         *           using this mapping's known algebraic expressions
         * @throws  ExecutionException  if a compilation error occurs
         */
        private StarTable[] makeSubTables( StarTable[] inTables )
                throws ExecutionException {

            /* Work out the type of columns we will require from each table. */
            ValueInfo[] tupleInfos = matchEngine_.getTupleInfos();
            int nCoord = tupleInfos.length;

            /* For each table, create a new table containing only columns
             * defined by the supplied expressions. */
            StarTable[] subTables = new StarTable[ inTables.length ];
            for ( int i = 0; i < subTables.length; i++ ) {
                ColumnInfo[] colInfos = new ColumnInfo[ nCoord ];
                for ( int j = 0; j < nCoord; j++ ) {
                    colInfos[ j ] = new ColumnInfo( tupleInfos[ j ] );
                }
                try {
                    subTables[ i ] = new JELTable( inTables[ i ], colInfos,
                                                   exprTuples_[ i ] );
                }
                catch ( CompilationException e ) {
                    throw new ExecutionException( e.getMessage(), e );
                }
            }
            return subTables;
        }
    }
}
