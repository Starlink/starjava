package uk.ac.starlink.tfcat;

/**
 * Interface for simple syntax checking.
 *
 * @author   Mark Taylor
 * @since    10 Feb 2022
 */
@FunctionalInterface
public interface WordChecker {

    /**
     * Reports warnings or errors associated with a supplied string.
     * If the string is correct, or if it is empty or null,
     * null is returned, otherwise some human-readable indication of
     * what's wrong is returned.
     * <p>In general the warning does not need to quote the whole input string,
     * though it can highlight particular parts of it if that's expected
     * to be useful.
     *
     * @param   word  input text
     * @return    error message, or null for no error
     */
    String checkWord( String word );
}
