package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Extends the Task interface by methods which allow task parameters to be
 * queried based on a (at least partially) populated Environment.
 * By default, Tasks report the Parameters they use so that the on-line
 * help system can supply that information to users at runtime.
 * However, for some tasks the parameters in use depend on the value of
 * other parameters.  This interface allows tasks to make that information
 * available.  The method implementations are considered to be on a
 * best-efforts basis, it is not guaranteed that they will be able to
 * report all the parameters.  This information is only used for user help.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2014
 */
public interface DynamicTask {

    /**
     * Attempts to find a parameter with a given name that might be used
     * by this task in the content of the given environment.
     * This ought not to result in additional prompts to the user.
     *
     * @param   env  execution environment
     * @param   paramName  requested parameter name
     * @return   parameter with the given name, or null
     */
    Parameter getParameterByName( Environment env, String paramName )
            throws TaskException;
}
