package uk.ac.starlink.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines an object which can return an output stream, and hence
 * represents the potential destination of a stream of data.
 * It is used in preference to an OutputStream so that you can
 * avoid opening output files before you know you need them,
 * so you don't get new empty files (possibly overwriting old ones)
 * when a command fails.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2006
 */
public interface Destination {

    /**
     * Returns an output stream which will write to this destination.
     * This method is only intended to be called once for a given
     * instance.
     *
     * @return  output stream
     */
    OutputStream createStream() throws IOException;

    /** Destination which directs output to <code>System.out</code>.  */
    public static final Destination SYSTEM_OUT = new Destination() {
        public OutputStream createStream() {
            return System.out;
        }
    };
}
