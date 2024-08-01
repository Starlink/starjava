package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for selecting input table format.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class InputFormatParameter extends StringParameter
                                  implements ExtraParameter {

    @SuppressWarnings("this-escape")
    public InputFormatParameter( String name ) {
        super( name );
        setNullPermitted( false );
        setStringDefault( StarTableFactory.AUTO_HANDLER );
        setTableDescription( "the input table", null );
    }

    /** 
     * Sets the wording used to refer to the input table in parameter
     * descriptions. 
     * If not set, the wording "the input table" is used.
     *  
     * @param  inDescrip  text to replace "the input table"
     * @param  tableParam  if supplied, gives the table parameter on behalf
     *                     of which this format parameter is operating;
     *                     may be null
     */ 
    public final void
            setTableDescription( String inDescrip,
                                 AbstractInputTableParameter<?> tableParam ) {
        setPrompt( "Format name for " + inDescrip );
        StringBuffer dbuf = new StringBuffer();
        dbuf.append( inDescrip );
        if ( tableParam != null ) {
            dbuf.append( " as specified by parameter <code>" )
                .append( tableParam.getName() )
                .append( "</code>" );
        }
        setDescription( new String[] {
            "<p>Specifies the format of " + dbuf.toString() + ".",
            "The known formats are listed in <ref id='inFormats'/>.",
            "This flag can be used if you know what format your",
            "table is in.",
            "If it has the special value",
            "<code>" + StarTableFactory.AUTO_HANDLER + "</code> (the default),",
            "then an attempt will be",
            "made to detect the format of the table automatically.",
            "This cannot always be done correctly however, in which case",
            "the program will exit with an error explaining which",
            "formats were attempted.",
            "This parameter is ignored for scheme-specified tables.",
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
        for ( TableBuilder handler : tfact.getDefaultBuilders() ) {
            sbuf.append( "      " )
                .append( handler.getFormatName().toLowerCase() )
                .append( '\n' );
        }
        sbuf.append( '\n' );
        sbuf.append( "   Known in-formats:\n" );
        for ( String fmt : tfact.getKnownFormats() ) {
            sbuf.append( "      " )
                .append( fmt.toLowerCase() )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    public String stringToObject( Environment env, String stringval )
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
        return super.stringToObject( env, stringval );
    }

    private StarTableFactory getTableFactory( Environment env ) {
        return LineTableEnvironment.getTableFactory( env );
    }
}
