package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Defines a way to specify a StarTable given a textual specification.
 * This is intended mainly for tables that are not derived from an
 * input stream.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2020
 */
public interface TableScheme {

    /**
     * Returns the name of this scheme.
     * This string must be alphanumeric and should be short.
     * It will be used between the colons in a table specification.
     *
     * @return  scheme name
     */
    String getSchemeName();

    /**
     * Returns a short, plain-text usage string.
     * This should just represent the legal syntax for the specification
     * string.
     *
     * @return  usage syntax
     */
    String getSchemeUsage();

    /**
     * Turns a scheme-specific specification into a table.
     * In case of any error, an exception should be thrown.
     *
     * @param  specification  scheme-specific table specification
     *                        (scheme name part is not included)
     * @return  created table, not null
     * @throws   TableFormatException  if the format of the specification
     *                                 is syntactically incorrect
     *                                 (will typically provoke an error
     *                                 containing the schemeUsage string)
     * @throws   IOException  if there is some other error in creating
     *                        the table
     */
    StarTable createTable( String specification ) throws IOException;

    /**
     * Returns a scheme-specific specification string suitable for use
     * in examples.  It should return a table short enough to appear
     * in textual documentation.  If no suitable example is available,
     * null may be returned.
     *
     * @return  scheme-specific specification (scheme name part not included),
     *          or null
     */
    String getExampleSpecification();
}
