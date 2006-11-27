package uk.ac.starlink.ttools.task;

import java.util.Iterator;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for selecting input table format.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class InputFormatParameter extends Parameter implements ExtraParameter {

    public InputFormatParameter( String name ) {
        super( name );
        setPrompt( "Format name for input table" );
        setNullPermitted( false );
        setDefault( StarTableFactory.AUTO_HANDLER );

        setDescription( new String[] {
            "<p>Specifies the format of the input table",
            "(one of the known formats listed in <ref id='inFormats'/>).",
            "This flag can be used if you know what format your input",
            "table is in.",
            "If it has the special value",
            "<code>" + StarTableFactory.AUTO_HANDLER + "</code> (the default),",
            "then an attempt will be",
            "made to detect the format of the table automatically.",
            "This cannot always be done correctly however, in which case",
            "the program will exit with an error explaining which",
            "formats were attempted.",
            "</p>",
        } );
    }

    public String getUsage() {
        return "<in-format>";
    }

    public String getExtraUsage( TableEnvironment env ) {
        StarTableFactory tfact = env.getTableFactory();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Auto-detected in-formats:\n" );
        for ( Iterator it = tfact.getDefaultBuilders().iterator();
              it.hasNext(); ) {
            sbuf.append( "      " )
                .append( ((TableBuilder) it.next())
                        .getFormatName().toLowerCase() )
                .append( '\n' );
        }
        sbuf.append( '\n' );
        sbuf.append( "   Known in-formats:\n" );
        for ( Iterator it = tfact.getKnownFormats().iterator();
              it.hasNext(); ) {
            sbuf.append( "      " )
                .append( ((String) it.next()).toLowerCase() )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        if ( ! StarTableFactory.AUTO_HANDLER.equals( stringval ) ) {
            try {
                getTableFactory( env ).getTableBuilder( stringval );
            }
            catch ( TableFormatException e ) {
                throw new ParameterValueException(
                    this, "Unknown format " + stringval, e );
            }
        }
        super.setValueFromString( env, stringval );
    }

    private StarTableFactory getTableFactory( Environment env ) {
        return LineTableEnvironment.getTableFactory( env );
    }
}
