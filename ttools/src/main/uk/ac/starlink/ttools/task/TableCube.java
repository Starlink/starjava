package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.mode.CubeMode;

/**
 * Task implementation for the histogram array creation task.
 *
 * @author   Mark Taylor
 * @since    3 May 2006
 */
public class TableCube extends SingleMapperTask {

    private final WordsParameter<String> colsParam_;

    public TableCube() {
        super( "Calculates N-dimensional histograms", new CubeMode(),
               false, true );
        CubeMode mode = (CubeMode) getOutputMode();
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        colsParam_ = WordsParameter.createStringWordsParameter( "cols" );
        colsParam_.setWordUsage( "<expr> ..." );
        colsParam_.setPrompt( "Space-separated list of input columns" );
        colsParam_.setDescription( new String[] {
            "<p>Columns to use for this task.",
            "One or more <code>&lt;expr&gt;</code> elements, ",
            "separated by spaces, should be given.",
            "Each one represents a numeric value from the table,",
            "provided as a column name or algebraic expression.",
            "</p>",
            "<p>The number of columns listed in the value of this",
            "parameter defines the dimensionality of the output",
            "data cube.",
            "</p>",
        } );
        mode.setColumnsParameter( colsParam_ );
        paramList.add( colsParam_ );

        getParameterList().addAll( 0, paramList );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        return super.createInputProducer( env );
    }
}
