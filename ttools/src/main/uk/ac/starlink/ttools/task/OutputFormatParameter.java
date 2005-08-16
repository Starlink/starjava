package uk.ac.starlink.ttools.task;

import java.util.Iterator;
import uk.ac.starlink.task.Parameter;

/**
 * Parameter to hold the format of a table for output.
 * 
 * @author   Mark Taylor 
 * @since    15 Aug 2005
 */
public class OutputFormatParameter extends Parameter 
                                   implements ExtraParameter {

    public OutputFormatParameter( String name ) {
        super( name );
        setUsage( "<out-format>" );
        setPrompt( "Format name for output table" );
        setNullPermitted( true );
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Known output formats:\n" );
        for ( Iterator it = env.getTableOutput().getKnownFormats().iterator();
              it.hasNext(); ) {
            sbuf.append( "      " )
                .append( ((String) it.next()).toLowerCase() )
                .append( '\n' );
        }
        return sbuf.toString();
    }
}
