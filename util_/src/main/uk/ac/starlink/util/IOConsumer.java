package uk.ac.starlink.util;

import java.io.IOException;

/**
 * Consumer-like interface that allows an IOException to be thrown.
 *
 * @see   java.util.function.Consumer
 */
@FunctionalInterface
public interface IOConsumer<T> {

    /**
     * Performs an operation on the given value.
     *
     * @throws  IOException  in case of error
     */
    void accept( T value ) throws IOException;
}
