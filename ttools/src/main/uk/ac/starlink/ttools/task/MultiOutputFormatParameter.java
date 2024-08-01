package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;

/**
 * Parameter to hold an output format capable of writing multiple tables.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2010
 */
public class MultiOutputFormatParameter extends OutputFormatParameter
                                        implements ExtraParameter {

    @SuppressWarnings("this-escape")
    public MultiOutputFormatParameter( String name ) {
        super( name );
        setPrompt( "Format name for output tables" );
        setDescription( new String[] {
            super.getDescription().replaceAll( "table\\b", "tables" ),
            "<p>Not all output formats are capable of writing multiple tables;",
            "if you choose one that is not, an error will result.",
            "</p>",
        } );
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Known multi-table output formats:\n" )
            .append( "      " )
            .append( StarTableOutput.AUTO_HANDLER )
            .append( '\n' );
        for ( StarTableWriter writer : env.getTableOutput().getHandlers() ) {
            if ( writer instanceof MultiStarTableWriter ) {
                sbuf.append( "      " )
                    .append( writer.getFormatName().toLowerCase() )
                    .append( '\n' );
            }
        }
        return sbuf.toString();
    }
}
