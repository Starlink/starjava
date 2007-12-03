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
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.JELTable;
import uk.ac.starlink.ttools.task.InputTableSpec;
import uk.ac.starlink.ttools.task.JoinFixActionParameter;
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
    private final JoinFixActionParameter fixcolsParam_;
    private final ChoiceParameter mmodeParam_;
    private final IntegerParameter irefParam_;

    private static final String PAIRS_MODE = "pairs";
    private static final String GROUP_MODE = "group";

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.join" );

    /**
     * Constructor.
     */
    public MatchMapper() {

        irefParam_ = new IntegerParameter( "iref" );
        irefParam_.setPrompt( "Index of reference table in " + PAIRS_MODE
                            + " mode" );
        irefParam_.setUsage( "<table-index>" );
        irefParam_.setDefault( "1" );

        mmodeParam_ =
            new ChoiceParameter( "multimode",
                                 new String[] { PAIRS_MODE, GROUP_MODE, } );
        mmodeParam_.setPrompt( "Semantics of multi-table match" );
        mmodeParam_.setDefault( PAIRS_MODE );

        irefParam_.setDescription( new String[] {
            "<p>If <code>" + mmodeParam_.getName() + "</code>"
                           + "=<code>" + PAIRS_MODE + "</code>",
            "this parameter gives the index of the table in the input table",
            "list which is to serve as the reference table",
            "(the one which must be matched by other tables).",
            "Ignored in other modes.",
            "</p>",
        } );
        mmodeParam_.setDescription( new String[] {
            "<p>Defines what is meant by a multi-table match.",
            "There are two possibilities:",
            "<ul>",
            "<li><code>" + PAIRS_MODE + "</code>:",
            "Each output row corresponds to a single row of the",
            "<em>reference table</em>",
            "(see parameter <code>" + irefParam_.getName() + "</code>)",
            "and contains entries from other tables which are pair matches",
            "to that.",
            "If a reference table row matches multiple rows from one of",
            "the other tables, only the best one is included.",
            "</li>",
            "<li><code>" + GROUP_MODE + "</code>:",
            "Each output row corresponds to a group of entries from the",
            "input tables which are",
            "mutually linked by pair matches between them.",
            "This means that although you can get from any entry to any",
            "other entry via one or more pair matches,",
            "there is no guarantee that any entry",
            "is a pair match with any other entry.",
            "No table has privileged status in this case.",
            "If there are multiple entries from a given table in the",
            "match group, an arbitrary one is chosen for inclusion",
            "(there is no unique way to select the best).",
            "</li>",
            "</ul>",
            "</p>",
        } );

        matcherParam_ = new MatchEngineParameter( "matcher" );
        fixcolsParam_ = new JoinFixActionParameter( "fixcols" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            mmodeParam_,
            irefParam_,
            matcherParam_,
            matcherParam_.getMatchParametersParameter(),
            matcherParam_.createMatchTupleParameter( "N" ),
            new UseAllParameter( "N" ),
            fixcolsParam_,
            fixcolsParam_.createSuffixParameter( "N" ),
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {

        String mmode = mmodeParam_.stringValue( env );
        final int iref;
        if ( PAIRS_MODE.equalsIgnoreCase( mmode ) ) {
            irefParam_.setMinimum( 1 );
            irefParam_.setMaximum( nin );
            iref = irefParam_.intValue( env ) - 1;
        }
        else {
            iref = -1;
        }
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
            fixActs[ i ] = fixcolsParam_.getJoinFixAction( env, numLabel );
            useAlls[ i ] = new UseAllParameter( numLabel ).useAllValue( env );
        }
        PrintStream logStrm = env.getErrorStream();
        if ( GROUP_MODE.equalsIgnoreCase( mmode ) ) {
            return new GroupMatchMapping( matcher, exprTuples, fixActs, logStrm,
                                          useAlls );
        }
        else if ( PAIRS_MODE.equalsIgnoreCase( mmode ) ) {
            return new PairsMatchMapping( matcher, exprTuples, fixActs, logStrm,
                                          iref, useAlls );
        }
        else {
            throw new AssertionError( "Unknown multimode " + mmode + "???" );
        }
    }

    /**
     * TableMapping implementation used by MatchMapper.
     */
    private static abstract class MatchMapping implements TableMapping {

        private final int nin_;
        private final MatchEngine matchEngine_;
        private final String[][] exprTuples_;
        private final JoinFixAction[] fixActs_;
        private final PrintStream logStrm_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   logStrm   output stream for progress logging
         */
        MatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                      JoinFixAction[] fixActs, PrintStream logStrm ) {
            matchEngine_ = matchEngine;
            exprTuples_ = exprTuples;
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
                matches = findMatches( matcher );
                if ( ! matches.sort() ) {
                    logger.warning( "Implementation can't sort rows - "
                                  + "matched table rows may not be sorted" );
                }
            }
            catch ( InterruptedException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }

            /* Create a new table based on the matched rows. */
            return createJoinTable( inTables, matches, fixActs_ );
        }

        /**
         * Calculates a set of RowLinks representing the required table match.
         * The returned set is not necessarily sorted.
         *
         * @param   matcher  row matcher configured for use
         * @return  set of matched row links
         */
        protected abstract LinkSet findMatches( RowMatcher matcher )
                throws IOException, InterruptedException;

        /**
         * Builds a table representing the requested join given a set of
         * row links.  These links will already have been sorted if appropriate.
         *
         * @param   inTables   array of input tables
         * @param   matches    row link set
         * @param   fixActs    actions for fixing up duplicated table columns
         */
        protected abstract StarTable createJoinTable( StarTable[] inTables,
                                                      LinkSet matches,
                                                      JoinFixAction[] fixActs )
                throws IOException;
      

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
     * MatchMapping concrete subclass for multi-pair matches.
     */
    private static class PairsMatchMapping extends MatchMapping {

        private final int iref_;
        private final boolean[] useAlls_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   logStrm   output stream for progress logging
         * @param   iref      index (0-based) of reference table
         */
        PairsMatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                           JoinFixAction[] fixActs, PrintStream logStrm,
                           int iref, boolean[] useAlls ) {
            super( matchEngine, exprTuples, fixActs, logStrm );
            iref_ = iref;
            useAlls_ = useAlls;
        }

        protected LinkSet findMatches( RowMatcher matcher )
                throws IOException, InterruptedException {
            return matcher.findMultiPairMatches( iref_, true, useAlls_ );
        }

        protected StarTable createJoinTable( StarTable[] inTables,
                                             LinkSet matches,
                                             JoinFixAction[] fixActs ) {
            return MatchStarTables
                  .makeJoinTable( inTables, matches, false, fixActs, null );
        }
    }

    /**
     * MatchMapping concrete subclass for group matches.
     */
    private static class GroupMatchMapping extends MatchMapping {

        private final boolean[] useAlls_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   logStrm   output stream for progress logging
         * @param   useAlls   nin-element array of flags indicating whether
         *                    all or only matched rows should be output
         */
        GroupMatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                           JoinFixAction[] fixActs, PrintStream logStrm,
                           boolean[] useAlls ) {
            super( matchEngine, exprTuples, fixActs, logStrm );
            useAlls_ = useAlls;
        }

        protected LinkSet findMatches( RowMatcher matcher )
                throws IOException, InterruptedException {
            return matcher.findGroupMatches( useAlls_ );
        }

        protected StarTable createJoinTable( StarTable[] inTables,
                                             LinkSet matches,
                                             JoinFixAction[] fixActs ) {
            return MatchStarTables
                  .makeJoinTable( inTables, matches, false, fixActs, null );
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
                "<p>Determines which rows",
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
                "which participate in a match are included in the output.",
                "</p>",
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
