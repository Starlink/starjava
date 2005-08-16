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
        setNullPermitted( true );
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
        if ( stringval != null ) {
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
        return TableEnvironment.getTableFactory( env );
    }
}
