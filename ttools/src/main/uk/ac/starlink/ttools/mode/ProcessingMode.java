package uk.ac.starlink.ttools.mode;

import java.util.Iterator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Interface defining the final element of a table processing pipeline -
 * the one which disposes of the generated table in some way.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public interface ProcessingMode {

    /**
     * Creates a TableConsumer, deriving any additional required
     * configuration from a given environment.
     *
     * @param   env  execution environment
     */
    TableConsumer createConsumer( Environment env ) throws TaskException;

    /**
     * Returns a list of any parameters which are associated with this mode. 
     *
     * @return  parameter list
     */
    Parameter[] getAssociatedParameters();

    /**
     * Returns a textual description of this processing mode.  This will
     * be included in the user document so should be in XML.
     *
     * @return  textual description of this mode
     */
    String getDescription();
  
}
