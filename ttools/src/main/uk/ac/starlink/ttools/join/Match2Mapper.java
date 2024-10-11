package uk.ac.starlink.ttools.join;

import gnu.jel.CompilationException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.InputTableSpec;
import uk.ac.starlink.ttools.task.JoinFixActionParameter;
import uk.ac.starlink.ttools.task.RowRunnerParameter;
import uk.ac.starlink.ttools.task.TableMapper;
import uk.ac.starlink.ttools.task.TableMapping;
import uk.ac.starlink.ttools.task.WordsParameter;

/**
 * TableMapper which does the work for pair matching (tmatch2).
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class Match2Mapper implements TableMapper {

    private final MatchEngineParameter matcherParam_;
    private final List<WordsParameter<String>> tupleParams_;
    private final JoinTypeParameter joinParam_;
    private final FindModeParameter modeParam_;
    private final JoinFixActionParameter fixcolParam_;
    private final ProgressIndicatorParameter progressParam_;
    private final Parameter<RowRunner> runnerParam_;

    /**
     * Constructor.
     */
    public Match2Mapper() {
        matcherParam_ = new MatchEngineParameter( "matcher" );
        tupleParams_ = new ArrayList<WordsParameter<String>>( 2 );
        tupleParams_.add( matcherParam_.createMatchTupleParameter( "1" ) );
        tupleParams_.add( matcherParam_.createMatchTupleParameter( "2" ) );
        fixcolParam_ = new JoinFixActionParameter( "fixcols" );
        joinParam_ = new JoinTypeParameter( "join" );
        modeParam_ = new FindModeParameter( "find" );
        progressParam_ = new ProgressIndicatorParameter( "progress" );
        runnerParam_ = RowRunnerParameter.createMatchRunnerParameter( "runner");
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            matcherParam_,
            tupleParams_.get( 0 ),
            tupleParams_.get( 1 ),
            matcherParam_.getMatchParametersParameter(),
            matcherParam_.getTuningParametersParameter(),
            joinParam_,
            modeParam_,
            fixcolParam_,
            fixcolParam_.createSuffixParameter( "1" ),
            fixcolParam_.createSuffixParameter( "2" ),
            matcherParam_.getScoreParameter(),
            progressParam_,
            runnerParam_,
        };
    }

    /**
     * Returns the parameter used to acquire the pair match mode.
     *
     * @return  pair mode parameter
     */
    public Parameter<PairMode> getPairModeParameter() {
        return modeParam_;
    }

    /**
     * Returns the parameter used to acquire the join type.
     *
     * @return  join type parameter
     */
    public Parameter<JoinType> getJoinTypeParameter() {
        return joinParam_;
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {

        /* Get the matcher. */
        MatchEngine matcher = matcherParam_.matchEngineValue( env );

        /* Assemble the arrays of supplied expressions which will supply
         * the values to the matcher for each table. */
        String[][] tupleExprs = new String[ 2 ][];
        for ( int i = 0; i < 2; i++ ) {
            WordsParameter<String> tupleParam = tupleParams_.get( i );
            MatchEngineParameter.configureTupleParameter( tupleParam, matcher );
            tupleExprs[ i ] = tupleParam.wordsValue( env );
        }

        /* Get other parameter values. */
        JoinType join = joinParam_.joinTypeValue( env );
        PairMode pairMode = modeParam_.objectValue( env );
        JoinFixAction[] fixacts = fixcolParam_.getJoinFixActions( env, 2 );
        ValueInfo scoreInfo = matcherParam_.getScoreInfo( env );
        ProgressIndicator progger =
            progressParam_.progressIndicatorValue( env );
        RowRunner runner = runnerParam_.objectValue( env );

        /* Construct and return a mapping based on this lot. */
        return new Match2Mapping( matcher, tupleExprs[ 0 ], tupleExprs[ 1 ],
                                  join, pairMode, fixacts[ 0 ], fixacts[ 1 ],
                                  scoreInfo, progger, runner );
    }
}
