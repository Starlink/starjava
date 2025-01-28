package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.mode.MocShapeMode;

/**
 * Task for generating MOCs from shape specifications in a table.
 *
 * @author   Mark Taylor
 * @since    30 Jan 2025
 */
public class MocShape extends SingleMapperTask {

    /**
     * Constructor.
     */
    public MocShape() {
        super( "Generates Multi-Order Coverage maps from shape values",
               new MocShapeMode(), false, true );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        return super.createInputProducer( env );
    }
}
