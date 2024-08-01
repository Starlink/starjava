package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.task.StringParameter;

/**
 * Parameter to hold the format of a table for output.
 * 
 * @author   Mark Taylor 
 * @since    15 Aug 2005
 */
public class OutputFormatParameter extends StringParameter 
                                   implements ExtraParameter {

    @SuppressWarnings("this-escape")
    public OutputFormatParameter( String name ) {
        super( name );
        setUsage( "<out-format>" );
        setPrompt( "Format name for output table" );
        setStringDefault( StarTableOutput.AUTO_HANDLER );
        setNullPermitted( false );

        setDescription( new String[] {
            "<p>Specifies the format in which the output table will be written",
            "(one of the ones in <ref id='outFormats'/> - matching is",
            "case-insensitive and you can use just the first few letters).",
            "If it has the special value",
            "\"<code>" + StarTableOutput.AUTO_HANDLER + "</code>\"",
            "(the default),",
            "then the output filename will be",
            "examined to try to guess what sort of file is required",
            "usually by looking at the extension.",
            "If it's not obvious from the filename what output format is",
            "intended, an error will result.",
            "</p>",
        } );
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Known output formats:\n" );
        sbuf.append( "      " )
            .append( StarTableOutput.AUTO_HANDLER )
            .append( '\n' );
        for ( String fmt : env.getTableOutput().getKnownFormats() ) {
            sbuf.append( "      " )
                .append( fmt.toLowerCase() )
                .append( '\n' );
        }
        return sbuf.toString();
    }
}
