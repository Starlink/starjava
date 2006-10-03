package uk.ac.starlink.ttools.task;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
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
public class XmlEncodingParameter extends Parameter
                                  implements ExtraParameter {

    private Charset charset_;

    public XmlEncodingParameter( String name ) {
        super( name );
        setUsage( "<xml-encoding>" );
        setNullPermitted( true );

        setDescription( new String[] {
            "<p>Selects the Unicode encoding used for the output XML.",
            "The available options and default are dependent on your JVM,",
            "but the default probably corresponds to UTF-8.",
            "Use <code>help=" + getName() + "</code> for a full listing.",
            "</p>",
        } );
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( '\n' )
            .append( "   Supported encodings:\n" );
        for ( Iterator it = Charset.availableCharsets().keySet().iterator();
              it.hasNext(); ) {
            sbuf.append( "      " )
                .append( it.next().toString() )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Returns the value of this parameter as a Charset object.
     *
     * @param   env  execution environment
     * @return  charset representing the XML encoding
     */
    public Charset charsetValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return charset_;
    }

    public void setValueFromString( Environment env, String sval )
            throws TaskException {
        try {
            charset_ = sval == null ? null
                                    : Charset.forName( sval );
            super.setValueFromString( env, sval );
        }
        catch ( UnsupportedCharsetException e ) {
            throw new ParameterValueException( this, e );
        }
        catch ( IllegalCharsetNameException e ) {
            throw new ParameterValueException( this, e );
        }
    }

}
