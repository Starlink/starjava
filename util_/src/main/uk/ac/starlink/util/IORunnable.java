package uk.ac.starlink.util;

import java.io.IOException;

/**
 * Runnable-like interface that allows an IOException to be thrown.
 *
 * @author   Mark Taylor
 * @since    29 Jan 2024
 * @see      java.lang.Runnable
 */
@FunctionalInterface
public interface IORunnable {

    /**
     * Does something.
     */
    void run() throws IOException;
}
