package uk.ac.starlink.util;

import java.io.IOException;

/**
 * Supplier-like interface that allows an IOException to be thrown.
 *
 * @see   java.util.function.Supplier
 */
@FunctionalInterface
public interface IOSupplier<T> {

    /**
     * Gets a result.
     *
     * @return   result
     * @throws  IOException  in case of error
     */
    T get() throws IOException;
}
