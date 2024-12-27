package uk.ac.starlink.util;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utilities for use with java Streams.
 *
 * @author   Mark Taylor
 * @since    23 Jun 2023
 */
public class StreamUtil {

    /**
     * Private sole constructor prevents instantiation.
     */
    private StreamUtil() {
    }

    /**
     * Utility function that can be used to filter streams to exclude any
     * elements that are not instances of a particular type.
     * This doesn't do anything particularly complicated, but it allows
     * one to combine a filter (for class) step with a type-casting step
     * in a way which is commonly required and otherwise annoyingly verbose. 
     *
     * <p>Use the result of this method as the argument to
     * {@link java.util.stream.Stream#flatMap}.
     *
     * @param  keepClazz  class for which instances are required
     * @return  function for use in flatMap
     */
    public static <T,R> Function<T,Stream<R>>
            keepInstances( Class<R> keepClazz ) {
        return item -> keepClazz.isInstance( item )
             ? Stream.of( keepClazz.cast( item ) )
             : Stream.empty();
    }
}
