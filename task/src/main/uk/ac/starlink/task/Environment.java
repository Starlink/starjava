package uk.ac.starlink.task;

import java.io.PrintStream;

/**
 * Defines an execution environment within which tasks can operate.
 * The main job that it does is to be able to provide a String value
 * for a given Parameter object.  Every invocation of a Task is
 * expected to have a new Environment.
 * 
 * @author   Mark Taylor
 */
public interface Environment {

    /**
     * Obtains a legal value for a given parameter from the environment and 
     * sets the parameter's value accordingly.
     * <p>
     * This environment should obtain a value for the parameter <tt>par</tt>
     * in whatever way it sees fit, and must then call <tt>par</tt>'s 
     * {@link Parameter#setValueFromString} method so that the parameter
     * knows what its new value is.  If the <tt>setValueFromString</tt> call
     * throws a <tt>ParameterValueException</tt> the environment may 
     * try to get another value (for instance by re-prompting the user)
     * or may give up and re-throw the exception.
     *
     * @param   par  the Parameter whose value is to be obtained and set
     * @return  a String representing the value of the parameter
     * @throws  AbortException   if the environment determines during the
     *          attempt to obtain the parameter value that the task should
     *          be aborted
     * @throws  ParameterValueException  if no legal value can be set
     */
    void setParameterValue( Parameter par )
            throws AbortException, ParameterValueException;

    /**
     * Clears a value for a given parameter.
     *
     * @param  par  the Parameter whose value is to be cleared
     */
    void clearParameterValue( Parameter par );

    /**
     * Returns an output stream into which text output from a task can
     * be written.
     *
     * @return  a stream for text output
     */
    PrintStream getPrintStream();
}
