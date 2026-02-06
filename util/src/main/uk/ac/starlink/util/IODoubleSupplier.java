package uk.ac.starlink.util;

import java.io.IOException;

/**
 * DoubleSupplier-like interface that allows an IOException to be thrown.
 *
 * @see   java.util.function.DoubleSupplier
 */
@FunctionalInterface
public interface IODoubleSupplier {

    /**
     * Gets a result.
     *
     * @return   result
     * @throws  IOException  in case of error
     */
    double getAsDouble() throws IOException;
}
