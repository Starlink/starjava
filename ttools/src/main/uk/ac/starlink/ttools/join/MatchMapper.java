package uk.ac.starlink.ttools.join;

import java.io.IOException;
import java.util.Collection;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.MultiJoinType;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RowLink;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELTable;
import uk.ac.starlink.ttools.task.InputTableSpec;
import uk.ac.starlink.ttools.task.JoinFixActionParameter;
import uk.ac.starlink.ttools.task.RowRunnerParameter;
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
    private final ChoiceParameter<String> mmodeParam_;
    private final IntegerParameter irefParam_;
    private final ProgressIndicatorParameter progressParam_;
    private final Parameter<RowRunner> runnerParam_;

    /** MultiMode parameter value for pairs matching. */
    public static final String PAIRS_MODE = "pairs";

    /** MultiMode parameter value for group matching. */
    public static final String GROUP_MODE = "group";

    /**
     * Constructor.
     */
    public MatchMapper() {

        irefParam_ = new IntegerParameter( "iref" );
        irefParam_.setPrompt( "Index of reference table in " + PAIRS_MODE
                            + " mode" );
        irefParam_.setUsage( "<table-index>" );
        irefParam_.setIntDefault( 1 );

        mmodeParam_ =
            new ChoiceParameter<String>( "multimode",
                                         new String[] { PAIRS_MODE,
                                                        GROUP_MODE, } );
        mmodeParam_.setPrompt( "Semantics of multi-table match" );
        mmodeParam_.setStringDefault( PAIRS_MODE );

        irefParam_.setDescription( new String[] {
            "<p>If <code>" + mmodeParam_.getName() + "</code>"
                           + "=<code>" + PAIRS_MODE + "</code>",
            "this parameter gives the index of the table in the input table",
            "list which is to serve as the reference table",
            "(the one which must be matched by other tables).",
            "Ignored in other modes.",
            "</p>",
            "<p>Row ordering in the output table is usually tidiest",
            "if the default setting of 1 is used",
            "(i.e. if the first input table is used as the reference table).",
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
            "See <ref id='matchGroup'/> for more discussion.",
            "</li>",
            "</ul>",
            "Note that which rows actually appear in the output",
            "is also influenced by the", 
            "<code>" + createMultiJoinTypeParameter( "N" ).getName()
                     + "</code>",
            "parameter.",
            "</p>",
        } );

        matcherParam_ = new MatchEngineParameter( "matcher" );
        fixcolsParam_ = new JoinFixActionParameter( "fixcols" );
        progressParam_ = new ProgressIndicatorParameter( "progress" );
        runnerParam_ = RowRunnerParameter.createMatchRunnerParameter( "runner");
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            mmodeParam_,
            irefParam_,
            matcherParam_,
            matcherParam_.getMatchParametersParameter(),
            matcherParam_.getTuningParametersParameter(),
            matcherParam_.createMatchTupleParameter( "N" ),
            createMultiJoinTypeParameter( "N" ),
            fixcolsParam_,
            fixcolsParam_.createSuffixParameter( "N" ),
            progressParam_,
            runnerParam_,
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {

        final String mmode = mmodeParam_.stringValue( env );
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
        MultiJoinType[] joinTypes = new MultiJoinType[ nin ];
        for ( int i = 0; i < nin; i++ ) {
            String numLabel = Integer.toString( i + 1 );
            WordsParameter<String> tupleParam =
                matcherParam_.createMatchTupleParameter( numLabel );
            MatchEngineParameter.configureTupleParameter( tupleParam, matcher );
            exprTuples[ i ] = tupleParam.wordsValue( env );
            StringParameter suffixParam =
                fixcolsParam_.createSuffixParameter( numLabel );
            fixActs[ i ] = fixcolsParam_.getJoinFixAction( env, suffixParam );
            joinTypes[ i ] =
                createMultiJoinTypeParameter( numLabel ).objectValue( env );
        }
        ProgressIndicator progger =
            progressParam_.progressIndicatorValue( env );
        RowRunner runner = runnerParam_.objectValue( env );
        if ( GROUP_MODE.equalsIgnoreCase( mmode ) ) {
            return new GroupMatchMapping( matcher, exprTuples, fixActs, progger,
                                          runner, joinTypes );
        }
        else if ( PAIRS_MODE.equalsIgnoreCase( mmode ) ) {
            return new PairsMatchMapping( matcher, exprTuples, fixActs, progger,
                                          runner, iref, joinTypes );
        }
        else {
            throw new AssertionError( "Unknown multimode " + mmode + "???" );
        }
    }

    /**
     * Returns the parameter used to acquire multi-table match type.
     *
     * @return   multimode parameter
     */
    public Parameter<String> getMultiModeParameter() {
        return mmodeParam_;
    }

    /**
     * Returns the parameter used to acquire join type
     * for an input table identified by a given suffix.
     *
     * @param  suffix  input table suffix
     * @return   join type parameter for one table
     */
    public Parameter<MultiJoinType>
            createMultiJoinTypeParameter( String suffix ) {
        return new MultiJoinTypeParameter( suffix );
    }

    /**
     * TableMapping implementation used by MatchMapper.
     */
    private static abstract class MatchMapping implements TableMapping {

        private final int nin_;
        private final MatchEngine matchEngine_;
        private final String[][] exprTuples_;
        private final JoinFixAction[] fixActs_;
        final RowRunner runner_;
        final ProgressIndicator progger_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   progger   progress indicator
         * @param   runner    controls parallel implementation,
         *                    or null for sequential
         */
        MatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                      JoinFixAction[] fixActs, ProgressIndicator progger,
                      RowRunner runner ) {
            matchEngine_ = matchEngine;
            exprTuples_ = exprTuples;
            fixActs_ = fixActs;
            progger_ = progger;
            runner_ = runner;
            nin_ = exprTuples_.length;
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
            RowMatcher matcher =
                RowMatcher.createMatcher( matchEngine_, subTables, runner_ );
            matcher.setIndicator( progger_ );
            LinkSet matches = findMatches( matcher );

            /* Create a new table based on the matched rows. */
            Collection<RowLink> orderedMatches =
                MatchStarTables.orderLinks( matches );
            return createJoinTable( inTables, orderedMatches, fixActs_ );
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
        protected abstract StarTable
                createJoinTable( StarTable[] inTables,
                                 Collection<RowLink> matches,
                                 JoinFixAction[] fixActs )
                throws IOException, InterruptedException;
      

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
        private final MultiJoinType[] joinTypes_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   progger   progress indicator
         * @param   runner    controls parallel implementation,
         *                    or null for sequential
         * @param   iref      index (0-based) of reference table
         * @param   joinTypes inclusion criteria for links in output table
         */
        PairsMatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                           JoinFixAction[] fixActs, ProgressIndicator progger,
                           RowRunner runner,
                           int iref, MultiJoinType[] joinTypes ) {
            super( matchEngine, exprTuples, fixActs, progger, runner );
            iref_ = iref;
            joinTypes_ = joinTypes;
        }

        protected LinkSet findMatches( RowMatcher matcher )
                throws IOException, InterruptedException {
            return matcher.findMultiPairMatches( iref_, true, joinTypes_ );
        }

        protected StarTable createJoinTable( StarTable[] inTables,
                                             Collection<RowLink> matches,
                                             JoinFixAction[] fixActs )
                throws InterruptedException {
            return MatchStarTables.createInstance( progger_, runner_ )
                  .makeJoinTable( inTables, matches, false, fixActs, null );
        }
    }

    /**
     * MatchMapping concrete subclass for group matches.
     */
    private static class GroupMatchMapping extends MatchMapping {

        private final MultiJoinType[] joinTypes_;

        /**
         * Constructor.
         *
         * @param   matchEngine  match engine
         * @param   exprTuples  nin-element array of tuples of JEL 
         *          expressions for matcher inputs
         * @param   fixActs   nin-element array of actions for fixing up 
         *                    duplicated table columns
         * @param   progger   progress indicator
         * @param   runner    controls parallel implementation,
         *                    or null for sequential
         * @param   joinTypes inclusion criteria for links in output table
         */
        GroupMatchMapping( MatchEngine matchEngine, String[][] exprTuples,
                           JoinFixAction[] fixActs, ProgressIndicator progger,
                           RowRunner runner, MultiJoinType[] joinTypes ) {
            super( matchEngine, exprTuples, fixActs, progger, runner );
            joinTypes_ = joinTypes;
        }

        protected LinkSet findMatches( RowMatcher matcher )
                throws IOException, InterruptedException {
            return matcher.findGroupMatches( joinTypes_ );
        }

        protected StarTable createJoinTable( StarTable[] inTables,
                                             Collection<RowLink> matches,
                                             JoinFixAction[] fixActs )
                throws InterruptedException {
            return MatchStarTables.createInstance( progger_, runner_ )
                  .makeJoinTable( inTables, matches, false, fixActs, null );
        }
    }

    /**
     * Parameter which determines whether all or only matched rows should be
     * output for a given input table.
     */
    private static class MultiJoinTypeParameter
            extends ChoiceParameter<MultiJoinType> {

        /**
         * Constructor.
         *
         * @param   numLabel  numeric label, "1", "2", "N" etc
         */
        public MultiJoinTypeParameter( String numLabel ) {
            super( "join" + numLabel,
                   new MultiJoinType[] {
                       MultiJoinType.DEFAULT,
                       MultiJoinType.MATCH,
                       MultiJoinType.NOMATCH,
                       MultiJoinType.ALWAYS,
                   } );
            boolean hasNum = numLabel != null && numLabel.length() > 0;
            String prompt = "Output row selection criteria";
            if ( hasNum ) {
                prompt += " from table " + numLabel;
            }
            setPrompt( prompt );
            setDefaultOption( MultiJoinType.DEFAULT );
            setDescription( new String[] {
                "<p>Determines which rows",
                ( hasNum ? ( "from input table " + numLabel )
                         : "" ),
                "are included in the output table.",
                "The matching algorithm determines which of the rows in",
                "each of the input tables correspond to which rows in",
                "the other input tables, and this parameter determines",
                "what to do with that information.",
                "</p>",
                "<p>The default behaviour is that a row will appear in the",
                "output table if it represents a match of rows from two or",
                "more of the input tables.",
                "This can be altered on a per-input-table basis however",
                "by choosing one of the non-default options below:",
                "<ul>",
                "<li><code>" + MultiJoinType.MATCH + "</code>:",
                "Rows are included only if they contain an entry from",
                "input table " + numLabel + ".",
                "</li>",
                "<li><code>" + MultiJoinType.NOMATCH + "</code>:",
                "Rows are included only if they do not contain an entry from",
                "input table " + numLabel + ".",
                "</li>",
                "<li><code>" + MultiJoinType.ALWAYS + "</code>:", 
                "Rows are included if they contain an entry from",
                "input table " + numLabel,
                "(overrides any " + MultiJoinType.MATCH
                        + " and " + MultiJoinType.NOMATCH,
                "settings of other tables).",
                "</li>",
                "<li><code>" + MultiJoinType.DEFAULT + "</code>:",
                "Input table " + numLabel + " has no special effect on",
                "whether rows are included.",
                "</li>",
                "</ul>",
                "</p>",
            } );
        }
    }
}
