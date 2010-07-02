package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Interface for an object which can acquire multiple input tables.
 *
 * @author   Mark Taylor
 * @since    2 Jul 2010
 */
public interface TablesInput {

    /**
     * Returns the parameters associated with this object.
     *
     * @return  parameters
     */
    Parameter[] getParameters();

    /**
     * Returns an array of InputTableSpec objects describing the input tables
     * used by this task.
     *
     * @param    env  execution environment
     * @return   input table specifiers
     */
    InputTableSpec[] getInputSpecs( Environment env ) throws TaskException;
}
