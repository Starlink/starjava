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
     * This environment should obtain a value for the parameter <code>par</code>
     * in whatever way it sees fit, and must then call <code>par</code>'s 
     * {@link Parameter#setValueFromString} method so that the parameter
     * knows what its new value is.  If the <code>setValueFromString</code> call
     * throws a <code>ParameterValueException</code> the environment may 
     * try to get another value (for instance by re-prompting the user)
     * or may give up and re-throw the exception.
     *
     * @param   par  the Parameter whose value is to be obtained and set
     */
    void acquireValue( Parameter<?> par ) throws TaskException;

    /**
     * Clears a value for a given parameter.
     *
     * @param  par  the Parameter whose value is to be cleared
     */
    void clearValue( Parameter<?> par );

    /**
     * Returns an array of parameter names which have been specified.
     * The result is not necessarily an exhaustive list of all parameters 
     * whose values can be retrieved from this environment, since an
     * interactive environment may be able to prompt the user for values,
     * but it can give a list of values provided explicitly or without
     * interactive prompts.
     *
     * @return   array of names of known supplied parameters
     */
    String[] getNames();

    /**
     * Returns an output stream into which text output from a task can
     * be written.  This would correspond to standard output for a
     * terminal-based application.
     *
     * @return  a stream for text output
     */
    PrintStream getOutputStream();

    /**
     * Returns an output stream into which error or logging output from
     * a task can be written.  This would correspodnd to standard error
     * for a terminal-based application.
     *
     * @return  a stream for error output
     */
    PrintStream getErrorStream();
}
