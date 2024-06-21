package uk.ac.starlink.task;

/**
 * Defines a user-level task.
 * Each task has a list of parameters, and an <code>invoke</code> method.
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
    Parameter<?>[] getParameters();

    /**
     * Creates an executable object which will do the work of this task
     * in the given Environment.
     * This method should do all of the parameter processing and prepare
     * an object which just executes.  The idea is that any communication
     * with the user related to the supplied parameter values can be
     * done before the task starts to execute in earnest. 
     * Consequently, the returned Executable object ought not to 
     * make any reference to <code>env</code>, though this is not
     * absolutely prohibited.
     *
     * @param  env  the environment in which the task will operate
     * @throws   TaskException  if no executable can be created;
     *           this should usually be a {@link UsageException} or
     *           some subclass
     */
    Executable createExecutable( Environment env ) throws TaskException;

    /**
     * Returns a short (one-line) description of the purpose of this task.
     *
     * @return  plain text description of this task's purpose
     */
    String getPurpose();
}
