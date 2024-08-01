package uk.ac.starlink.ttools.task;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter which describes one of the available XML encodings
 * (UTF-8 etc).
 *
 * @author   Mark Taylor
 * @since    16 Aug 2005
 */
public class XmlEncodingParameter extends Parameter<Charset>
                                  implements ExtraParameter {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    @SuppressWarnings("this-escape")
    public XmlEncodingParameter( String name ) {
        super( name, Charset.class, true );
        setUsage( "<xml-encoding>" );
        setNullPermitted( true );
        String dflt = "UTF-8";
        try {
            Charset.forName( dflt );
            setStringDefault( dflt );
        }
        catch ( UnsupportedCharsetException e ) {
            logger_.log( Level.WARNING,
                         "Unsupported charset " + dflt + "???", e );
        }

        setDescription( new String[] {
            "<p>Selects the Unicode encoding used for the output XML.",
            "The available options are dependent on your JVM,",
            "use <code>help=" + getName() + "</code> for a full listing.",
            "Setting the value null will use the JVM's system default.",
            "</p>",
        } );
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( '\n' )
            .append( "   Supported encodings:\n" );
        for ( String csetName : Charset.availableCharsets().keySet() ) {
            sbuf.append( "      " )
                .append( csetName )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    public Charset stringToObject( Environment env, String sval )
            throws TaskException {
        try {
            return Charset.forName( sval );
        }
        catch ( UnsupportedCharsetException e ) {
            throw new ParameterValueException( this, e );
        }
        catch ( IllegalCharsetNameException e ) {
            throw new ParameterValueException( this, e );
        }
    }
}
