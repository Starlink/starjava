package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.mode.MocMode;

/**
 * Constructs and writes a Multi-Order Coverage map.
 *
 * @author   Mark Taylor
 * @since    8 Mar 2012
 */
public class PixFootprint extends SingleMapperTask {

    /**
     * Constructor.
     */
    public PixFootprint() {
        super( "Generates Multi-Order Coverage maps", new MocMode(),
               false, true );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        return super.createInputProducer( env );
    }
}
