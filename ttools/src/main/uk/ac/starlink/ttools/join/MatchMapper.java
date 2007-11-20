package uk.ac.starlink.ttools.join;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.JELTable;
import uk.ac.starlink.ttools.task.InputTableSpec;
import uk.ac.starlink.ttools.task.TableMapper;
import uk.ac.starlink.ttools.task.TableMapping;
import uk.ac.starlink.ttools.task.WordsParameter;

/**
 * TableMapper which implements multi-table crossmatches.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2007
 */
public class MatchMapper implements TableMapper {

    private final MatchEngineParameter matcherParam_;

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.join" );

    /**
     * Constructor.
     */
    public MatchMapper() {
        matcherParam_ = new MatchEngineParameter( "matcher" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            matcherParam_,
            matcherParam_.getMatchParametersParameter(),
            matcherParam_.createMatchTupleParameter( "N" ),
            new UseAllParameter( "N" ),
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {

        MatchEngine matcher = matcherParam_.matchEngineValue( env );
        String[][] exprTuples = new String[ nin ][];
        JoinFixAction[] fixActs = new JoinFixAction[ nin ];
        boolean[] useAlls = new boolean[ nin ];
        for ( int i = 0; i < nin; i++ ) {
            String numLabel = Integer.toString( i + 1 );
            WordsParameter tupleParam =
                matcherParam_.createMatchTupleParameter( numLabel );
            MatchEngineParameter.configureTupleParameter( tupleParam, matcher );
            exprTuples[ i ] = tupleParam.wordsValue( env );
            fixActs[ i ] =
                JoinFixAction.makeRenameDuplicatesAction( "_" + numLabel );
            useAlls[ i ] =
                new UseAllParameter( numLabel ).useAllValue( env );
        }
        PrintStream logStrm = env.getErrorStream();
        return new MatchMapping( matcher, exprTuples, useAlls, fixActs,
                                 logStrm );
    }

    /**
     * TableMapping implementation used by MatchMapper.
     */
    private static class MatchMapping implements TableMapping {

        private final int nin_;
        private final MatchEngine matchEngine_;
        private final String[][] exprTuples_;
        private final boolean[] useAlls_;
        private final JoinFixAction[] fixActs_;
        private final PrintStream logStrm_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   useAlls   nin-element array of flags indicating whether
         *                    all or only matched rows should be output
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   logStrm   output stream for progress logging
         */
        MatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                      boolean[] useAlls, JoinFixAction[] fixActs,
                      PrintStream logStrm ) {
            matchEngine_ = matchEngine;
            exprTuples_ = exprTuples;
            useAlls_ = useAlls;
            fixActs_ = fixActs;
            logStrm_ = logStrm;
            nin_ = exprTuples_.length;
        }

        public StarTable mapTables( InputTableSpec[] inSpecs )
                throws IOException, TaskException {

            /* Get the input tables and check for errors. */
            StarTable[] inTables = new StarTable[ nin_ ];
            for ( int i = 0; i < nin_; i++ ) {
                inTables[ i ] = inSpecs[ i ].getWrappedTable();

                /* Dry run to catch any compilation errors associated with the
                 * tuple expressions, done before the possibly expensive step
                 * of randomising the input tables. */
                makeSubTable( inTables[ i ], exprTuples_[ i ] );
            }

            /* Prepare the tables in the form they will be required for
             * the match. */
            StarTable[] subTables = new StarTable[ nin_ ];
            for ( int i = 0; i < nin_; i++ ) {
                inTables[ i ] = Tables.randomTable( inTables[ i ] );
                subTables[ i ] = makeSubTable( inTables[ i ],
                                               exprTuples_[ i ] );
            }

            /* Do the match. */
            RowMatcher matcher = new RowMatcher( matchEngine_, subTables );
            matcher.setIndicator( new TextProgressIndicator( logStrm_ ) );
            LinkSet matches;
            try { 
                matches = matcher.findGroupMatches( useAlls_ );
                if ( ! matches.sort() ) {
                    logger.warning( "Implementation can't sort rows - "
                                  + "matched table rows may not be sorted" );
                }
            }
            catch ( InterruptedException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }

            /* Create a new table based on the matched rows. */
            return MatchStarTables
                  .makeJoinTable( inTables, matches, false, fixActs_, null );
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
        private StarTable makeSubTable( StarTable inTable, String[] exprTuple )
                throws ExecutionException {
            return JELTable.createJELTable( inTable,
                                            matchEngine_.getTupleInfos(),
                                            exprTuple );
        }
    }

    /**
     * Parameter which determines whether all or only matched rows should be
     * output for a given input table.
     */
    private static class UseAllParameter extends ChoiceParameter {

        private static final String TRUE = "all";
        private static final String FALSE = "matched";

        /**
         * Constructor.
         *
         * @param   numLabel  numeric label, "1", "2", "N" etc
         */
        public UseAllParameter( String numLabel ) {
            super( "join" + numLabel, new String[] { TRUE, FALSE, } );
            boolean hasNum = numLabel != null && numLabel.length() > 0;
            String prompt = "Output row selection criteria";
            if ( hasNum ) {
                prompt += " from table " + numLabel;
            }
            setPrompt( prompt );
            setDefault( FALSE );
            setDescription( new String[] {
                "Determines which rows",
                ( hasNum ? ( "from input table " + numLabel )
                         : "" ),
                "are included in the output table.",
                "The matching algorithm determines which of the rows in",
                "each of the input tables correspond to which rows in",
                "the other input tables, and this parameter determines",
                "what to do with that information.",
                "If it has the value \"<code>" + TRUE + "</code>\"",
                "then all of the row from",
                ( hasNum ? ( "input table " + numLabel )
                         : "the input tables" ),
                "will be included in the output table;",
                "if it has the value \"<code>" + FALSE + "</code>\"",
                "then only those rows",
                "which form a match with at least one other input table",
                "are included in the output.",
            } );
        }

        /**
         * Returns the boolean value of this parameter.
         *
         * @return   true for use all, false for use matched only
         */
        public boolean useAllValue( Environment env ) throws TaskException {
            Object oval = objectValue( env );
            if ( TRUE.equals( oval ) ) {
                return true;
            }
            else if ( FALSE.equals( oval ) ) {
                return false;
            }
            else {
                assert false;
                return false;
            }
        }
    }
}
