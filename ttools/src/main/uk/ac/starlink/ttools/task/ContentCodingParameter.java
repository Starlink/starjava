package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.util.ContentCoding;

/**
 * Parameter for configuring HTTP-level compression.
 *
 * @author   Mark Taylor
 * @since    6 May 2015
 */
public class ContentCodingParameter extends BooleanParameter {

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public ContentCodingParameter() {
        super( "compress" );
        setPrompt( "Use HTTP-level compression?" );
        setDescription( new String[] {
            "<p>If true, the service is requested to provide HTTP-level",
            "compression for the response stream",
            "(Accept-Encoding header is set to \"<code>gzip</code>\",",
            "see RFC 2616).",
            "This does not guarantee that compression will happen",
            "but if the service honours this request it may result in",
            "a smaller amount of network traffic",
            "at the expense of more processing on the server and client.",
            "</p>",
        } );
        setBooleanDefault( true );
    }

    /**
     * Returns the content coding instance indicated by the current
     * value of this parameter.
     *
     * @param  env  execution envirionment
     * @return   content-coding instance, not null
     */
    public ContentCoding codingValue( Environment env ) throws TaskException {
        return booleanValue( env ) ? ContentCoding.GZIP : ContentCoding.NONE;
    }
}
