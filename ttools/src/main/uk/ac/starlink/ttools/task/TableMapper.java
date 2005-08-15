package uk.ac.starlink.ttools.task;

import java.util.Iterator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.UsageException;

/**
 * Interface for an operation which takes zero or more tables as input
 * and produces one table as output.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public interface TableMapper {

    /**
     * Returns the number of tables required for input.
     *
     * @return   input table count
     */
    int getInCount();

    /**
     * Returns the parameters defined by this mapper.
     *
     * @return  parameter array
     */
    Parameter[] getParameters();

    /**
     * Returns a usage string fragment for this mapper.
     *
     * @return   usage
     */
    String getUsage();

    /**
     * Creates a new mapping object, using a given execution environment
     * for any additional required state.
     *
     * @param  env  execution environment
     */
    TableMapping createMapping( Environment env ) throws UsageException;
}
