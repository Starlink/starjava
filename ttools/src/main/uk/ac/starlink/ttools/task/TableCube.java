package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.mode.CubeMode;

/**
 * Task implementation for the histogram array creation task.
 *
 * @author   Mark Taylor
 * @since    3 May 2006
 */
public class TableCube extends MapperTask {
    public TableCube() {
        super( new ColumnSelectionMapper(), new CubeMode(), true, false );
        ColumnSelectionMapper mapper = (ColumnSelectionMapper) getMapper();
        CubeMode mode = (CubeMode) getOutputMode();
        WordsParameter colsParam = mapper.getColumnsParameter();
        colsParam.setDescription( new String[] {
             colsParam.getDescription(),
             "The number of columns listed in the value of this",
             "parameter defines the dimensionality of the output",
             "data cube.",
        } );
        mode.setColumnsParameter( mapper.getColumnsParameter() );
    }
}
