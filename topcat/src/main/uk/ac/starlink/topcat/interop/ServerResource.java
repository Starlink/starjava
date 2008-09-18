package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines a resource suitable for serving by the {@link TopcatServer}
 * HTTP server.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2008
 */
public interface ServerResource {

    /**
     * Returns the MIME type of this resource.
     *
     * @return   value of Content-Type HTTP header
     */
    String getContentType();

    /**
     * Returns the number of bytes in this resource, if known.
     *
     * @return   value of Content-Length HTTP header if known;
     *           otherwise a negative number
     */
    long getContentLength();

    /**
     * Writes resource body.
     *
     * @param  out  destination stream
     */
    void writeBody( OutputStream out ) throws IOException;
}
