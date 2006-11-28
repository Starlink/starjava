package uk.ac.starlink.ttools.task;

import java.util.Iterator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Interface for an operation which takes zero or more tables as input
 * and produces one table as output.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public interface TableMapper {

    /**
     * Returns the parameters defined by this mapper.
     *
     * @return  parameter array
     */
    Parameter[] getParameters();

    /**
     * Creates a new mapping object, using a given execution environment
     * for any additional required state.
     *
     * @param  env  execution environment
     * @param  nin  number of input tables that the mapping will operate on
     *              if known; -1 if not
     */
    TableMapping createMapping( Environment env, int nin )
           throws TaskException;
}
