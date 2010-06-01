package uk.ac.starlink.registry;

/**
 * Unchecked exception used to contain a checked Exception.
 * This is needed because it may have to be thrown from an Iterator.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2008
 * @see   AbstractRegistryClient#getResourceIterator
 */
public class RegistryQueryException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param   cause  underlying exception
     */
    public RegistryQueryException( Throwable cause ) {
        super( cause.getMessage() == null ? "Registry Access Exception"
                                          : cause.getMessage(), cause );
    }
}
