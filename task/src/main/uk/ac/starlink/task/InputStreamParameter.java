package uk.ac.starlink.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLUtils;

/**
 * Parameter which can provide an input stream based on its value.
 * The string value may be a filename, a URL or the special value "-"
 * which indicates standard input.  Streams are automatically uncompressed.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Aug 2005
 * @see      uk.ac.starlink.util.Compression
 */
public class InputStreamParameter extends Parameter<InputStream> {

    private final boolean allowSystem_;

    @SuppressWarnings("this-escape")
    public InputStreamParameter( String name ) {
        super( name, InputStream.class, true );
        allowSystem_ = true;
        setUsage( "<location>" );
    }

    public InputStream stringToObject( Environment env, String sval )
            throws ParameterValueException {
        if ( ! sval.equals( "-" ) &&
             ! new File( sval ).exists() ) {
            try {
                URLUtils.newURL( sval );
            }
            catch ( MalformedURLException e ) {
                String msg = "Value " + sval + " is not a file, URL or \"-\"";
                throw new ParameterValueException( this, msg );
            }
        }
        try {
            return DataSource.getInputStream( sval, allowSystem_ );
        }
        catch ( IOException e ) {
            throw (ParameterValueException)
                  new ParameterValueException( this, e.toString() )
                 .initCause( e );
        }
    }
}
