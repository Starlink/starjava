package uk.ac.starlink.task;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Parameter representing a URL value.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2011
 */
public class URLParameter extends Parameter {

    private URL urlValue_;

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public URLParameter( String name ) {
        super( name );
        setUsage( "<url-value>" );
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        try {
            urlValue_ = new URL( stringval );
        }
        catch ( MalformedURLException e ) {
            throw new ParameterValueException( this, e );
        }
        super.setValueFromString( env, stringval );
    }

    /**
     * Returns the value of this parameter as a URL.
     *
     * @return  url value
     */
    public URL urlValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return urlValue_;
    }
}
