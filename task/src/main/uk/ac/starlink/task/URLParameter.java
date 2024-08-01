package uk.ac.starlink.task;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Parameter representing a URL value.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2011
 */
public class URLParameter extends Parameter<URL> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    @SuppressWarnings("this-escape")
    public URLParameter( String name ) {
        super( name, URL.class, false );
        setUsage( "<url-value>" );
    }

    public URL stringToObject( Environment env, String stringval )
            throws ParameterValueException {
        try {
            return new URL( stringval );
        }
        catch ( MalformedURLException e ) {
            throw new ParameterValueException( this, "Not a URL", e );
        }
    }
}
