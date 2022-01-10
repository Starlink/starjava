package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.TaskException;

/**
 * Defines a parser which can turn a string into some other value.
 *
 * @author   Mark Taylor
 * @since    9 May 2006
 */
@FunctionalInterface
public interface WordParser<W> {

    /**
     * Parses a string to return a value of some kind.
     * If the word cannot be parsed, a <code>TaskException</code>,
     * preferably with an explanatory (user-directed) message,
     * should be thrown.  This method serves the purpose of validation
     * as well as translation.
     *
     * @param   word  string form
     * @return   parsed value
     */
    W parseWord( String word ) throws TaskException;
}
