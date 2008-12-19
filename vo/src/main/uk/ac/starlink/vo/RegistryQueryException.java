package uk.ac.starlink.vo;

import net.ivoa.registry.RegistryAccessException;

/**
 * Unchecked exception used to contain a RegistryAccessException.
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
    public RegistryQueryException( RegistryAccessException cause ) {
        super( cause.getMessage() == null ? "Registry Access Exception"
                                          : cause.getMessage(), cause );
    }
}
