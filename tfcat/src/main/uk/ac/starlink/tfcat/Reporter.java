package uk.ac.starlink.tfcat;

/**
 * Recipient for validation messages.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public interface Reporter {

    /**
     * Reports an error message.
     * The message should generally contain human-readable information
     * about some conformance error, but the location of the error is
     * not required.
     *
     * @param  msg  human-readable message
     */
    void report( String msg );

    /**
     * Report on validity of the supplied Uniform Content Descriptor.
     * Any issues of concern will be reported.
     *
     * @param  ucd  UCD
     * @see   <a href="https://www.ivoa.net/documents/UCD1+/">UCD1+</a>
     */
    void checkUcd( String ucd );

    /**
     * Report on validity of the supplied unit string.
     * Any issues of concern will be reported.
     *
     * @param  unit  unit string
     * @see  <a href="https://www.ivoa.net/documents/VOUnits/">VOUnits</a>
     */
    void checkUnit( String unit );

    /**
     * Returns a reporter suitable for use in a subcontext characterised
     * by a string, that usually means a level down in the object
     * hierarchy.
     *
     * @param   subContext  subcontext designation
     * @return  new reporter
     */
    Reporter createReporter( String subContext );

    /**
     * Returns a reporter suitable for use in a subcontext characterised
     * by an integer, that usually means an indexed array element
     * below the current level.
     *
     * @param   subContext  subcontext designation
     * @return  new reporter
     */
    Reporter createReporter( int subContext );
}
