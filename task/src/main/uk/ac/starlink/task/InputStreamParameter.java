package uk.ac.starlink.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import uk.ac.starlink.util.DataSource;

/**
 * Parameter which can provide an input stream based on its value.
 * The string value may be a filename, a URL or the special value "-"
 * which indicates standard input.  Streams are automatically uncompressed.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Aug 2005
 * @see      uk.ac.starlink.util.Compression
 */
public class InputStreamParameter extends Parameter {

    public InputStreamParameter( String name ) {
        super( name );
        setUsage( "<location>" );
    }

    public void setValueFromString( Environment env, String sval )
            throws TaskException {
        if ( sval != null &&
             ! sval.equals( "-" ) &&
             ! new File( sval ).exists() ) {
            try {
                new URL( sval ); 
            }
            catch ( MalformedURLException e ) {
                String msg = "Value " + sval + " is not a file, URL or \"-\"";
                throw new ParameterValueException( this, msg );
            }
        }
        super.setValueFromString( env, sval );
    }

    /**
     * Returns an input stream based on the value of this parameter.
     *
     * @param   env  execution environment
     * @return  new, uncompressed input stream
     */
    public InputStream inputStreamValue( Environment env )
            throws TaskException {
        String loc = stringValue( env );
        try {
            return loc == null
                 ? null
                 : DataSource.getInputStream( stringValue( env ) );
        }
        catch ( IOException e ) {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
