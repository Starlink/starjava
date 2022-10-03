package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.Match1Type;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.join.Match1Mapping;
import uk.ac.starlink.ttools.join.Match1TypeParameter;
import uk.ac.starlink.ttools.join.MatchEngineParameter;
import uk.ac.starlink.ttools.join.ProgressIndicatorParameter;

/**
 * Performs an internal (single-table) crossmatch.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2007
 */
public class TableMatch1 extends SingleMapperTask {

    private final MatchEngineParameter matcherParam_;
    private final WordsParameter<String> tupleParam_;
    private final Match1TypeParameter type1Param_;
    private final ProgressIndicatorParameter progressParam_;
    private final Parameter<RowRunner> runnerParam_;

    /**
     * Constructor.
     */
    public TableMatch1() {
        super( "Performs a crossmatch internal to a single table",
               new ChoiceMode(), true, true );
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        matcherParam_ = new MatchEngineParameter( "matcher" );
        paramList.add( matcherParam_ );
        paramList.add( matcherParam_.getMatchParametersParameter() );
        paramList.add( matcherParam_.getTuningParametersParameter() );

        tupleParam_ = matcherParam_.createMatchTupleParameter( "" );
        paramList.add( tupleParam_ );

        type1Param_ = new Match1TypeParameter( "action" );
        paramList.add( type1Param_ );

        progressParam_ = new ProgressIndicatorParameter( "progress" );
        paramList.add( progressParam_ );

        runnerParam_ = RowRunnerParameter.createMatchRunnerParameter( "runner");
        paramList.add( runnerParam_ );

        getParameterList().addAll( 0, paramList );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {

        /* Get the matcher and tuple expressions. */
        MatchEngine matcher = matcherParam_.matchEngineValue( env );
        MatchEngineParameter.configureTupleParameter( tupleParam_, matcher );
        String[] tupleExprs = tupleParam_.wordsValue( env );

        /* Get the matching type. */
        Match1Type type1 = type1Param_.typeValue( env );

        /* Get the progress indicator. */
        ProgressIndicator progger =
            progressParam_.progressIndicatorValue( env );

        /* Get parallel implementation option. */
        RowRunner runner = runnerParam_.objectValue( env );

        /* Construct and return a table producer which will do the work. */
        final SingleTableMapping mapping = 
            new Match1Mapping( matcher, type1, tupleExprs, progger, runner );
        final TableProducer inProd = createInputProducer( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                return mapping.map( inProd.getTable() );
            }
        };
    }
}
