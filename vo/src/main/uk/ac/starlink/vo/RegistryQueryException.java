package uk.ac.starlink.vo;

/**
 * Unchecked exception used to contain a checked Exception.
 * This is needed because it may have to be thrown from an Iterator.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2008
 * @see   RegistryQuery#getQueryIterator
 */
public class RegistryQueryException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param   cause  underlying exception
     */
    public RegistryQueryException( Exception cause ) {
        super( cause.getMessage() == null ? "Registry Access Exception"
                                          : cause.getMessage(), cause );
    }
}
