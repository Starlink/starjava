package uk.ac.starlink.task;

/**
 * Defines a user-level task.
 * Each task has a list of parameters, and an <tt>invoke</tt> method.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface Task {

    /**
     * Returns the list of parameters which may be used by this task.
     *
     * @return  an array of the Parameter objects this task may request
     *          values for during its invocation
     */
    Parameter[] getParameters();

    /**
     * Invokes the task based on the given <tt>Environment</tt> object.
     *
     * @param  env  the environment in which the task will operate
     */
    void invoke( Environment env ) throws TaskException;
}
