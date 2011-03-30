package uk.ac.starlink.vo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Defines a parameter to be submitted as part of a multipart/form-data
 * HTTP POST operation.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2011
 */
public interface HttpStreamParam {

    /**
     * Returns the headers associated with this parameter.
     * Note these should generally include the Content-Type unless it is
     * text/plain.  The Content-Disposition should not be included.
     *
     * @return   name->value HTTP header map
     */
    Map<String,String> getHttpHeaders();

    /**
     * Writes the data content of this parameter to a stream.
     *
     * @param   out  destination stream
     */
    void writeContent( OutputStream out ) throws IOException;

    /**
     * Returns the number of bytes that will be written to the output stream,
     * if konwn.  If not known, -1 may be returned.
     *
     * @return   content length, or -1 if not known
     */
    long getContentLength();
}
