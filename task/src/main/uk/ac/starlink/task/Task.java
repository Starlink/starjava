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
     * Returns a usage message for this task. 
     * It should just contain the argument list (not the task's name)
     * The argument list should be consistent with the parameter list
     * returned by {@link #getParameters}; in particular it should use
     * the same parameter names.
     *
     * @return   usage message
     */
    String getUsage();

    /**
     * Invokes the task based on the given <tt>Environment</tt> object.
     *
     * @param  env  the environment in which the task will operate
     * @throws  ExcecutionException  if a simple error message will suffice -
     *          the object invoking this Task will typically not print
     *          a stack trace in this case
     * @throws  Exception  any other exception may be thrown -
     *          a normal stacktrace will typically be displayed to the user
     */
    void invoke( Environment env ) throws TaskException;
}
