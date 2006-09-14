package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.mode.CubeMode;

/**
 * Task implementation for the histogram array creation task.
 *
 * @author   Mark Taylor
 * @since    3 May 2006
 */
public class TableCube extends SingleMapperTask {

    private final WordsParameter colsParam_;

    public TableCube() {
        super( new TrivialMapper( 1 ), new CubeMode(), true, false );
        CubeMode mode = (CubeMode) getOutputMode();
        colsParam_ = new WordsParameter( "cols" );
        colsParam_.setWordUsage( "<col-id>" );
        colsParam_.setPrompt( "Space-separated list of input columns" );
        colsParam_.setDescription( new String[] {
            "Columns to use for this task.",
            "One or more <code>&lt;col-id&gt;</code> elements, ",
            "separated by spaces, should be given.",
            "Each one represents a column in the table, using either its",
            "name or index.",
             "The number of columns listed in the value of this",
             "parameter defines the dimensionality of the output",
             "data cube.",
        } );
        mode.setColumnsParameter( colsParam_ );
    }

    public Parameter[] getParameters() {
        Parameter[] sps = super.getParameters();
        Parameter[] params = new Parameter[ sps.length + 1 ];
        System.arraycopy( sps, 0, params, 0, sps.length );
        params[ sps.length ] = colsParam_;
        return params;
    }
}
