package uk.ac.starlink.util;

import java.io.IOException;

/**
 * Function-like interface that allows an IOException to be thrown.
 *
 * @see  java.util.function.Function
 */
@FunctionalInterface
public interface IOFunction<T,R> {

    /**
     * Applies this function to the given argument.
     *
     * @param   t  the function argument
     * @return  the function result
     * @throws   IOException in case of error
     */
    R apply( T t ) throws IOException;
}
