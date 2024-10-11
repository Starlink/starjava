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
    Parameter<?>[] getParameters();

    /**
     * Returns an array of InputTableSpec objects describing the input tables
     * used by this task.
     *
     * @param    env  execution environment
     * @return   input table specifiers
     */
    InputTableSpec[] getInputSpecs( Environment env ) throws TaskException;

    /**
     * Returns a parameter used for acquiring one of the numbered input tables.
     *
     * <p>Behaviour is undefined if you ask for a table index not applicable
     * to this input.
     *
     * @param   i  table index (0-based)
     * @return   table input parameter
     */
    public AbstractInputTableParameter<?> getInputTableParameter( int i );

    /**
     * Returns a parameter used for acquiring an input filter for
     * one of the numbered input tables.
     *
     * <p>Behaviour is undefined if you ask for a table index not applicable
     * to this input.
     *
     * @param   i  table index (0-based)
     * @return   input filter parameter
     */
    public FilterParameter getFilterParameter( int i );
}
